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

package com.haulmont.cuba.cli.registration

import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.ProjectStructure
import com.haulmont.cuba.cli.generation.appendChild
import com.haulmont.cuba.cli.generation.findFirstChild
import com.haulmont.cuba.cli.generation.updateXml
import java.nio.file.Path

class EntityRegistrationHelper {
    fun registerEntity(className: String, persistent: Boolean = true) {
        if (!persistent) {
            val metadataXml = ProjectStructure().getModule(ModuleStructure.GLOBAL_MODULE).metadataXml
            addEntityToConfig(metadataXml, "metadata-model", className)
        } else {
            val persistenceXml = ProjectStructure().getModule(ModuleStructure.GLOBAL_MODULE).persistenceXml
            addEntityToConfig(persistenceXml, "persistence-unit", className)
        }
    }

    private fun addEntityToConfig(configPath: Path, elementName: String, className: String) {
        updateXml(configPath) {
            val configElement = findFirstChild(elementName) ?: appendChild(elementName)
            configElement.appendChild("class") {
                textContent = className
            }
        }
    }
}