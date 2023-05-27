plugins {
    id("java")
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
        java.srcDir("src")
    }
    test {
        java.srcDir("test")
    }
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
}

dependencies {
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation("org.json:json:20230227")
}

tasks.jar {
    manifest {
        attributes(
            "Class-Path" to configurations.runtimeClasspath.map { conf -> conf.files.map { f -> f.name }.sorted().joinToString(" ") },
            "Main-Class" to "jkara.JKara"
        )
    }
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
        include("ffmpeg/**")
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
