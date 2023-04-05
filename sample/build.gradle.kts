plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":autoconfigure"))
    ksp(project(":autoconfigure-ksp"))
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
