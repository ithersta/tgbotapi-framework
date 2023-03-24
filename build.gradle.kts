plugins {
    alias(libs.plugins.kotlin.jvm)
}

allprojects {
    version = "0.1.0-SNAPSHOT"
    group = "com.ithersta"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}
