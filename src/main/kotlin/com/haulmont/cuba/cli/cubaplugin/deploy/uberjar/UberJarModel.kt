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

package com.haulmont.cuba.cli.cubaplugin.deploy.uberjar

import com.haulmont.cuba.cli.ProjectModel
import com.haulmont.cuba.cli.cubaplugin.deploy.ContextXmlParams
import com.haulmont.cuba.cli.prompting.Answers

class UberJarModel(answers: Answers, projectModel: ProjectModel) {
    val singleUberJar: Boolean by answers
    val generateLogback: Boolean by answers
    val customLogback: String? by answers.withDefault { null }

    val generateCustomJetty: Boolean by answers
    val jettyContextParams = answers["customJettyContextParams"]?.let { ContextXmlParams(it as Answers, projectModel) }
    val customJettyPath: String? by answers.withDefault { null }

    val corePort = (answers["corePort"] as String?)?.toInt() ?: 8079
    val webPort = (answers["webPort"] as String?)?.toInt() ?: 8080
    val portalPort = (answers["portalPort"] as String?)?.toInt() ?: 8081
}
