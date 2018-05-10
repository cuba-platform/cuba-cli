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

package com.haulmont.cuba.cli.cubaplugin.enumeration

import com.haulmont.cuba.cli.commands.nameFrom
import com.haulmont.cuba.cli.prompting.Answers

class EnumerationModel(answers: Answers) {
    val className: String by nameFrom(answers)
    val packageName: String by nameFrom(answers)
    val idType: String by nameFrom(answers)
    val values: List<EnumValue> = (answers["values"] as List<Answers>).map(::EnumValue)

    class EnumValue(mapRepresentation: Answers) {
        val name: String by nameFrom(mapRepresentation)
        val id: String by nameFrom(mapRepresentation)
    }

    companion object {
        const val MODEL_NAME = "enumeration"
    }

}