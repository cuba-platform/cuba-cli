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

import com.haulmont.cuba.cli.generation.get
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.generation.xpath
import org.kodein.di.direct
import org.kodein.di.generic.instance
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path

/**
 * Provides access for important project files. All paths are calculated lazily.
 * If file represented by path absents, rises [ProjectFileNotFoundException] on access.
 */
class ProjectStructure {
    val path: Path = run {
        val directoryManager = kodein.direct.instance<WorkingDirectoryManager>()
        directoryManager.workingDirectory
    }
    //    fail first if no build.gradle found
    val buildGradle: Path = path.resolve("build.gradle") orFail "No build.gradle found"

    val settingsGradle: Path = path.resolve("settings.gradle") orFail "No settings.gradle found"

    val rootPackage: String by lazy {
        findRootPackage(path) ?: throw ProjectFileNotFoundException("Unable to find root package")
    }

    val rootPackageDirectory: String by lazy {
        rootPackage.replace('.', '/')
    }

    fun getModule(name: String): ModuleStructure = ModuleStructure(name, rootPackage, path)
}

class ModuleStructure(val name: String, val rootPackage: String, projectRoot: Path) {
    val path: Path = projectRoot.resolve("modules", name) orFail "Module $name not found"

    val src: Path by lazy {
        path.resolve("src") orFail "Module's $name src directory not found"
    }

    val rootPackageDirectory: Path by lazy {
        resolvePackagePath(rootPackage)
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
        val fileName = "web-screens.xml"
        src.resolve(fileName) orTry
                rootPackageDirectory.resolve(fileName) orFail
                "Module $name screens.xml not found"
    }
    val springXml: Path by lazy {
        val fileName = if (name == "core") "spring.xml" else "web-spring.xml"
        src.findFile(fileName) orFail "Module $name spring.xml not found"
    }

    fun resolvePackagePath(packageName: String) = src.resolve(packageName.replace('.', '/'))


    companion object {
        const val CORE_MODULE = "core"
        const val GLOBAL_MODULE = "global"
        const val WEB_MODULE = "web"
    }
}

class ProjectFileNotFoundException(message: String) : RuntimeException(message)

private infix fun Path?.orTry(another: Path?): Path? =
        this?.takeIf { Files.exists(this) } ?: another

private infix fun Path?.orFail(message: String): Path =
        this?.takeIf { Files.exists(this) } ?: throw ProjectFileNotFoundException(message)


private fun findRootPackage(projectRoot: Path): String? {
    val globalModuleSrc = projectRoot.resolve("modules/global/src")

    globalModuleSrc.takeIf { Files.exists(it) }
            ?.findFile("metadata.xml")?.let {
                parse(it).documentElement.xpath("//metadata-model").firstOrNull() as Element
            }?.let {
                return it["root-package"]
            } ?: throw ProjectFileNotFoundException("Unable to find root package")
}

private fun Path.findFile(name: String): Path? =
        Files.walk(this)
                .filter { Files.isRegularFile(it) && (it.fileName.toString() == name) }
                .findFirst()
                .orElse(null)
