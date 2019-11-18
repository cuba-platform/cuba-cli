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

package com.haulmont.cuba.cli.cubaplugin.installcomponent

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.cubaplugin.ProjectService
import com.haulmont.cuba.cli.cubaplugin.di.cubaKodein
import com.haulmont.cuba.cli.localMessages
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.Kodein
import org.kodein.di.generic.instance

@Parameters(commandDescription = "Adds CUBA application component to the project")
class AddComponentCommand(override val kodein: Kodein = cubaKodein) : GeneratorCommand<ComponentModel>() {

    private val messages by localMessages()

    private val projectService: ProjectService by kodein.instance()

    override fun getModelName(): String = ComponentModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        question("artifactCoordinates", messages["artifactCoordinatesQuestionCaption"]) {
            validate {
                value.split(':').size == 3 || fail(messages["invalidArtifactCoordinates"])
            }
        }
    }

    override fun createModel(answers: Answers): ComponentModel = ComponentModel("artifactCoordinates" from answers)

    override fun generate(bindings: Map<String, Any>) {
        projectService.registerAppComponent(model.artifactCoordinates)
    }
}