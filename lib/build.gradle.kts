plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.arrow.resilience)
    api(libs.inmo.tgbotapi)
    testImplementation(kotlin("test"))
}

kotlin {
    explicitApi()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
