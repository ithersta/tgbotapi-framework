import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSDeclarationContainer
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

private const val BASE_PACKAGE = "com.ithersta.tgbotapi.basetypes"
private const val BASE_ACTION_NAME = "$BASE_PACKAGE.Action"
private const val BASE_MESSAGE_STATE_NAME = "$BASE_PACKAGE.MessageState"

class Processor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
) : SymbolProcessor {
    private val subclasses = listOf(BASE_ACTION_NAME, BASE_MESSAGE_STATE_NAME)
        .associateWith { mutableListOf<KSClassDeclaration>() }

    private fun getAllDeclarations(resolver: Resolver): List<KSDeclaration> {
        val declarations: MutableList<KSDeclaration> =
            resolver.getAllFiles().flatMap { it.declarations }.toMutableList()
        declarations.forEach { declaration ->
            if (declaration is KSDeclarationContainer) {
                declarations.addAll(declaration.declarations)
            }
        }
        return declarations
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val declarations = getAllDeclarations(resolver)
        (declarations + resolver.getClassDeclarationByName("com.ithersta.tgbotapi.basetypes.MessageState.Empty"))
            .filterIsInstance<KSClassDeclaration>()
            .flatMap { subclasses.keys.map { baseClassName -> baseClassName to it } }
            .filter { (baseClassName, classDeclaration) ->
                classDeclaration.superTypes.any {
                    it.resolve().declaration.qualifiedName?.asString() == baseClassName
                }
            }
            .filter { (_, classDeclaration) -> classDeclaration.validate() }
            .forEach { (baseClassName, classDeclaration) ->
                subclasses.getValue(baseClassName) += classDeclaration
            }
        return emptyList()
    }

    override fun finish() {
        val distinctSubclasses = subclasses
            .mapValues { (_, value) -> value.distinctBy { it.qualifiedName?.asString() } }
        val files = distinctSubclasses
            .flatMap { it.value }
            .mapNotNull { it.containingFile }
            .distinctBy { it.filePath }
            .toTypedArray()
        val dependencies = Dependencies(aggregating = true, sources = files)
        val packageName = "com.ithersta.tgbotapi.autoconfigure"
        FileSpec
            .scriptBuilder(packageName = packageName, fileName = "SerializersModule")
            .addImport("kotlinx.serialization.modules", "polymorphic", "subclass")
            .addFunction(
                FunSpec
                    .builder("generatedSerializersModule")
                    .returns(ClassName("kotlinx.serialization.modules", "SerializersModule"))
                    .addCode(
                        CodeBlock.builder()
                            .beginControlFlow("return SerializersModule")
                            .apply {
                                distinctSubclasses.forEach { (baseClassName, subclasses) ->
                                    beginControlFlow("polymorphic($baseClassName::class)")
                                    subclasses
                                        .forEach { subclass ->
                                            if (!subclass.annotations.any { it.shortName.getShortName() == "Serializable" }) {
                                                logger.error(
                                                    "\n${subclass.qualifiedName?.asString()} is missing a @Serializable annotation",
                                                    subclass,
                                                )
                                            }
                                            addStatement(
                                                "subclass(%T::class)",
                                                subclass.asStarProjectedType().toTypeName(),
                                            )
                                        }
                                    endControlFlow()
                                }
                            }
                            .endControlFlow()
                            .build(),
                    )
                    .build(),
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, dependencies = dependencies)
        FileSpec
            .scriptBuilder(packageName = packageName, fileName = "Main")
            .addFunction(
                FunSpec
                    .builder("autoconfigure")
                    .receiver(ClassName("org.koin.core", "KoinApplication"))
                    .addModifiers(KModifier.SUSPEND)
                    .addCode("return autoconfigure(generatedSerializersModule())")
                    .build(),
            )
            .build()
            .writeTo(codeGenerator = codeGenerator, aggregating = false)
    }
}

class ProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return Processor(environment.codeGenerator, environment.logger)
    }
}
