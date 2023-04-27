plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.bundles.exposed)
    implementation(libs.xerial.sqlite)
    api(libs.kotlinx.serialization.protobuf)
    testImplementation(kotlin("test"))
}

kotlin {
    explicitApi()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
