plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":sqlite"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
