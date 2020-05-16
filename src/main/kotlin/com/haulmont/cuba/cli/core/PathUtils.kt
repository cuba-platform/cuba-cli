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

package com.haulmont.cuba.cli.core

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors

fun Path.resolve(vararg parts: String): Path = parts.fold(this) { currentPath, part ->
    currentPath.resolve(part)
}

fun Path.walk(depth: Int = Int.MAX_VALUE): List<Path> =
        Files.walk(this, depth)
                .collect(Collectors.toList())!!

fun Path.readText(): String = Files.newInputStream(this).bufferedReader().use { it.readText() }

fun Path.writeText(text: String) = Files.newOutputStream(this).bufferedWriter().use {
    it.write(text)
}

fun Path.appendText(text: String) = writeText(readText() + text)