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

import java.net.URI
import java.nio.file.FileSystemNotFoundException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths

class Resources {
    fun getResourcePath(name: String, clazz: Class<*> = javaClass): Path? {
        val uri = clazz.getResource(name)?.toURI()

        return if (uri != null) {
            if (uri.scheme == "jar") {
                val fileSystem = getFileSystem(uri)
                fileSystem.getPath(name)
            } else {
                Paths.get(uri)
            }
        } else null
    }

    private fun getFileSystem(templateUri: URI?) = try {
        FileSystems.getFileSystem(templateUri)
    } catch (e: FileSystemNotFoundException) {
        FileSystems.newFileSystem(templateUri, mutableMapOf<String, Any>())
    }
}