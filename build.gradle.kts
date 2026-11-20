import java.util.Base64

plugins {
    kotlin("jvm") version "2.4.21"
    id("org.jetbrains.dokka-javadoc") version "2.2.0"
    application
    id("com.gradleup.shadow") version "9.3.0"
    id("io.github.sgtsilvio.gradle.maven-central-publishing") version "0.6.0"
    `maven-publish`
    signing
}

group = "io.github.riadh-mnasri"
version = providers.gradleProperty("artifactVersion").get()

repositories { mavenCentral() }

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.slf4j:slf4j-nop:2.0.17")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.3")
    implementation("io.modelcontextprotocol.sdk:mcp:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

kotlin { jvmToolchain(17) }

application { mainClass.set("dev.planreview.MainKt") }

tasks.test { useJUnitPlatform() }

java {
    withSourcesJar()
}

val dokkaJavadocJar = tasks.register<Jar>("dokkaJavadocJar") {
    description = "Packages Kotlin API documentation in Javadoc format for publication."
    from(tasks.dokkaGeneratePublicationJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(dokkaJavadocJar)
            pom {
                name.set("Terraform Change Review")
                description.set("Evidence-based Terraform plan review for CLI and MCP clients.")
                url.set("https://github.com/riadh-mnasri/terraform-change-review")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("riadh-mnasri")
                        name.set("Riadh Mnasri")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/riadh-mnasri/terraform-change-review.git")
                    developerConnection.set("scm:git:ssh://git@github.com/riadh-mnasri/terraform-change-review.git")
                    url.set("https://github.com/riadh-mnasri/terraform-change-review")
                }
            }
        }
    }
}

signing {
    val signingKey = providers.gradleProperty("signingKeyBase64").orNull?.let {
        String(Base64.getDecoder().decode(it), Charsets.UTF_8)
    }
    useInMemoryPgpKeys(
        signingKey,
        providers.gradleProperty("signingPassword").orNull,
    )
    sign(publishing.publications["mavenJava"])
}

tasks.shadowJar {
    archiveFileName.set("terraform-change-review.jar")
    mergeServiceFiles()
    manifest { attributes["Main-Class"] = "dev.planreview.MainKt" }
}

tasks.build { dependsOn(tasks.shadowJar) }
