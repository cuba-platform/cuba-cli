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

package com.haulmont.cuba.cli.cubaplugin.editscreen

import com.haulmont.cuba.cli.prompting.Answers

class EditScreenModel(answers: Answers) {
    val screenName: String by answers
    val packageName: String by answers
    val fullEntityName: String = answers["entityName"] as String
    val entityName: String = fullEntityName.split('.').last()
    val controllerName: String = "${entityName}Edit"
    val entityPackageName: String = fullEntityName.split('.').let {
        it.take(it.size - 1).joinToString(".")
    }

    companion object {
        const val MODEL_NAME = "screen"
    }
}