package com.haulmont.cuba.cli

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Provides access for important project files. All paths are calculated lazily.
 * If file represented by path absents, rises {@link ProjectFileNotFoundException} on access.
 */
class ProjectFiles {
    //    fail first if no build.gradle found
    val buildGradle: Path = Paths.get("build.gradle") orFail "No build.gradle found"

    val rootPackageDirectory: Path by lazy {
        val global = getModule(ModuleType.GLOBAL)
        global.src.relativize(global.persistenceXml.parent)
    }

    fun getModule(type: ModuleType): ModuleFiles = when (type) {
        ModuleType.GLOBAL -> "global"
        ModuleType.CORE -> "core"
        ModuleType.WEB -> "web"
        ModuleType.GUI -> "gui"
    }.let { ModuleFiles(it) }
}

class ModuleFiles(val name: String) {
    val path: Path by lazy {
        Paths.get("modules", name) orFail "Module $name not found"
    }

    val src: Path by lazy {
        path.resolve("src") orFail "Module's $name src directory not found"
    }

    val persistenceXml: Path by lazy {
        Files.walk(src, Int.MAX_VALUE)
                .filter { Files.isRegularFile(it) and (it.fileName.toString() == "persistence.xml") }
                .findFirst()
                .orElse(null) orFail "Module $name persistence.xml not found"
    }

    val metadataXml: Path by lazy {
        Files.walk(src, Int.MAX_VALUE)
                .filter { Files.isRegularFile(it) and (it.fileName.toString() == "metadata.xml") }
                .findFirst()
                .orElse(null) orFail "Module $name metadata.xml not found"
    }
}

class ProjectFileNotFoundException(message: String) : RuntimeException(message)

enum class ModuleType {
    GLOBAL, CORE, WEB, GUI
}

private infix fun Path?.orFail(message: String): Path =
        this?.takeIf { Files.exists(this) } ?: throw ProjectFileNotFoundException(message)