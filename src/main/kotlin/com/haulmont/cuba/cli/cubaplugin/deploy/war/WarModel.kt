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

package com.haulmont.cuba.cli.cubaplugin.deploy.war

import com.haulmont.cuba.cli.cubaplugin.model.ProjectModel
import com.haulmont.cuba.cli.cubaplugin.deploy.ContextXmlParams
import com.haulmont.cuba.cli.prompting.Answers

class WarModel(answers: Answers, projectModel: ProjectModel) {
    val appHome: String by answers
    val includeJdbc: Boolean by answers

    val includeTomcatContextXml: Boolean by answers

    val generateContextXml: Boolean by answers.withDefault { false }
    val warContextParams = answers["warContextParams"]?.let { ContextXmlParams(it as Answers, projectModel) }
    val customContextXmlPath: String? by answers.withDefault { null }

    val singleWar: Boolean by answers
    val generateWebXml: Boolean by answers.withDefault { false }

    val customWebXmlPath: String? by answers.withDefault { null }

    val generateLogback: Boolean by answers
    val customLogback: String? by answers.withDefault { null }
}

