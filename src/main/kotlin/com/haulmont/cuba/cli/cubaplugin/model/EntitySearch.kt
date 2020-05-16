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

package com.haulmont.cuba.cli.cubaplugin.model

import com.haulmont.cuba.cli.cubaplugin.NamesUtils
import com.haulmont.cuba.cli.generation.getChildElements
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.core.readText
import net.sf.practicalxml.DomUtil
import org.kodein.di.Kodein
import org.kodein.di.generic.instance
import java.nio.file.Files

class EntitySearch(val kodein: Kodein = com.haulmont.cuba.cli.core.kodein) {
    private val namesUtils: NamesUtils by kodein.instance<NamesUtils>()

    fun getAllEntities(): List<Entity> {
        val globalModule = ProjectStructure().getModule(ModuleStructure.GLOBAL_MODULE)

        val persistenceXml = globalModule.persistenceXml

        val entityClasses = parse(persistenceXml).documentElement
                .let { DomUtil.getChild(it, "persistence-unit") }
                .getChildElements()
                .filter { element -> element.tagName == "class" }
                .map { it.textContent.trim() }

        return entityClasses.mapNotNull(::findEntity)
    }

    fun findEntity(fqn: String): Entity? {
        val globalModule = ProjectStructure().getModule(ModuleStructure.GLOBAL_MODULE)

        val directoriesSubPath = namesUtils.packageToDirectory(fqn)
        val code = listOf("java", "groovy", "kt").map { "$directoriesSubPath.$it" }.map { relativePath ->
            globalModule.src.resolve(relativePath)
        }.firstOrNull {
            Files.exists(it)
        }?.readText() ?: return null

        return Entity(fqn, code)
    }
}