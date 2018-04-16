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

package com.haulmont.cuba.cli.generation

import net.sf.practicalxml.ParseUtil
import org.w3c.dom.Document
import org.xml.sax.InputSource
import java.nio.file.Files
import java.nio.file.Path


fun parse(path: Path): Document =
        Files.newInputStream(path).let(::InputSource).let(ParseUtil::parse)


fun save(document: Document, path: Path) = DOMSerializer(numIndentSpaces = 4).run {
    serialize(document, Files.newOutputStream(path))
}
