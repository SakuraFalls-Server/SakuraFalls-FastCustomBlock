import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.4.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

group = "eu.bqkitcat"
version = "26.1.2-1"
description = "LiteProfiles"
java.sourceCompatibility = JavaVersion.VERSION_25

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        url = uri("https://maven.citizensnpcs.co/repo")
    }
}

tasks {
    withType<ProcessResources> {
        filter(mapOf(Pair("tokens", mapOf(
            Pair("version", version),
        ))), ReplaceTokens::class.java)
    }

    withType<ShadowJar> {
        this.configurations = listOf(project.configurations.shadow.get())
        this.archiveVersion.set("")
        this.archiveAppendix.set("")
    }
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    compileOnly("com.denizenscript:denizen:1.3.2-SNAPSHOT") { isTransitive = false }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}