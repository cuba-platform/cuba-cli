/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.cli

import com.haulmont.cuba.cli.generation.parse
import net.sf.practicalxml.DomUtil
import java.io.File
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

    val rootPackage: String by lazy {
        findRootPackage() ?: throw ProjectFileNotFoundException("Unable to find root package")
    }

    val rootPackageDirectory: Path by lazy {
        rootPackage.replace('.', File.separatorChar).let { Paths.get(it) }
    }

    fun getModule(type: ModuleType): ModuleFiles = when (type) {
        ModuleType.GLOBAL -> "global"
        ModuleType.CORE -> "core"
        ModuleType.WEB -> "web"
        ModuleType.GUI -> "gui"
    }.let { ModuleFiles(it, rootPackage) }
}

class ModuleFiles(val name: String, val rootPackage: String) {
    val path: Path = Paths.get("modules", name) orFail "Module $name not found"

    val src: Path by lazy {
        path.resolve("src") orFail "Module's $name src directory not found"
    }

    val rootPackageDirectory: Path by lazy {
        src.resolve(rootPackage.replace('.', File.separatorChar))
    }

    val metadataXml: Path by lazy {
        Files.walk(src)
                .filter { Files.isRegularFile(it) and (it.fileName.toString() == "metadata.xml") }
                .findFirst()
                .orElse(null) orFail "Module $name metadata.xml not found"
    }

    val persistenceXml: Path by lazy {
        src.findFile("persistence.xml") orFail "Module $name persistence.xml not found"
    }

    val screensXml: Path by lazy {
        val fileName = if (name == "gui") "screens.xml" else "web-screens.xml"
        src.resolve(fileName) orTry
                rootPackageDirectory.resolve(fileName) orFail
                "Module $name screens.xml not found"
    }
}

class ProjectFileNotFoundException(message: String) : RuntimeException(message)

enum class ModuleType {
    GLOBAL, CORE, WEB, GUI
}

private infix fun Path?.orTry(another: Path?): Path? =
        this?.takeIf { Files.exists(this) } ?: another

private infix fun Path?.orFail(message: String): Path =
        this?.takeIf { Files.exists(this) } ?: throw ProjectFileNotFoundException(message)


private fun findRootPackage(): String? {
    val globalModuleSrc = Paths.get("modules/global/src")

    globalModuleSrc.takeIf { Files.exists(it) }
            ?.findFile("metadata.xml")?.let {
                parse(it).documentElement
            }?.let {
                DomUtil.getChild(it, "metadata-model")
            }?.let {
                return it.getAttribute("root-package")
            } ?: throw ProjectFileNotFoundException("Unable to find root package")
}

private fun Path.findFile(name: String): Path? =
        Files.walk(this)
                .filter { Files.isRegularFile(it) && (it.fileName.toString() == name) }
                .findFirst()
                .orElse(null)
