plugins {
    id("application")
    id("com.github.ben-manes.versions") version "0.45.0"
}

group = "io.github.osobolev"
version = "1.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
    create("manual") {
        java.setSrcDirs(listOf("test"))
    }
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.release.set(17)
}

dependencies {
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation("org.json:json:20231013")
}

configurations["manualImplementation"].extendsFrom(configurations["implementation"])
configurations["manualRuntimeOnly"].extendsFrom(configurations["runtimeOnly"])
configurations["manualCompileOnly"].extendsFrom(configurations["compileOnly"])

dependencies {
    "manualImplementation"(sourceSets["main"].output)
}

application {
    mainClass.set("jkara.JKara")
    mainModule.set("jkara")
}

tasks.named("clean").configure {
    doLast {
        project.delete("$rootDir/distr")
    }
}

tasks.register("distr", Copy::class) {
    from(configurations.runtimeClasspath)
    from(tasks.jar)
    from("config")
    from(".") {
        include("scripts/**")
    }
    into("$rootDir/distr")
    val copied = mutableListOf<FileCopyDetails>()
    eachFile {
        copied.add(this)
    }
    doLast {
        copied.forEach {
            val target = it.relativePath.getFile(destinationDir)
            target.setLastModified(it.lastModified)
        }
    }
}
