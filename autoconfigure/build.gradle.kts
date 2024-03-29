plugins {
    alias(libs.plugins.kotlin.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":lib"))
    api(libs.koin.core)
    implementation(project(":sqlite"))
    implementation(libs.ktor.client.okhttp)
}

kotlin {
    explicitApi()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
