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

package com.haulmont.cuba.cli.commands

import com.haulmont.cli.core.commands.AbstractCommand
import com.haulmont.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.cubaplugin.model.ProjectModel
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import org.kodein.di.Kodein

abstract class AbstractCubaCommand(kodein: Kodein = com.haulmont.cli.core.kodein) : AbstractCommand(kodein) {

    protected val projectStructure: ProjectStructure by lazy { ProjectStructure() }

    /**
     * Returns project model if it already generated. Otherwise, it will raise an exception,
     * so call it only after [checkProjectExistence].
     */
    protected val projectModel: ProjectModel
        get() = run {
            if (context.hasModel(ProjectModel.MODEL_NAME)) {
                context.getModel(ProjectModel.MODEL_NAME)
            } else fail("No project module found")
        }

    /**
     * It is implied, that method invokes in [preExecute] to fail fast, if command is started outside of CUBA Platform project.
     *
     * @throws CommandExecutionException - if command is started outside of CUBA Platform project.
     */
    @Throws(CommandExecutionException::class)
    protected fun checkProjectExistence() {
        if (!context.hasModel("project")) {
            fail("Command should be started in project directory")
        }
    }
}