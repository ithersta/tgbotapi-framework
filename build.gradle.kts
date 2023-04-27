plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    `java-library`
    `maven-publish`
}

allprojects {
    version = "0.1.0-SNAPSHOT"
    group = "com.ithersta"
}

subprojects {
    apply(plugin = "java-library")
    apply(plugin = "maven-publish")

    java {
        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = group.toString()
                artifactId = project.name
                version = project.version.toString()
                from(components["java"])
            }
        }
    }
}
