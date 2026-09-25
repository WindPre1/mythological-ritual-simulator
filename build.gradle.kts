plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.24"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "dev.practice"
version = "2.3.0"

dependencies {
    paperweight.paperDevBundle("26.3.build.+")
}

tasks {
    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(27))
    }

    runServer {
        minecraftVersion("26.3")

        downloadPlugins {
            hangar("ViaVersion", "5.12.1-SNAPSHOT+1069")
            hangar("ViaBackwards", "5.12.1-SNAPSHOT+634")
            hangar("BlockReports", "2.2")
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-g", "-parameters"))
}
