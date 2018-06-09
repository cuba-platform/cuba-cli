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

package com.haulmont.cuba.cli.cubaplugin.updatescript

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import com.haulmont.cuba.cli.resolve
import org.kodein.di.generic.instance
import java.nio.file.Files
import java.util.*

@Parameters(commandDescription = "Creates new sql update script.")
class UpdateScript : GeneratorCommand<String>() {

    private val calendar = Calendar.getInstance()

    private val printHelper: PrintHelper by kodein.instance()

    override fun getModelName(): String = "updateScriptName"

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun QuestionsList.prompting() {
        question("scriptName", "Script name") {
            default("updateSomeTable")

            validate {
                checkRegex("[\\w\\-]+", "Script name may contain only letters, digits, dashes and underscores.")
            }
        }
    }

    override fun createModel(answers: Answers): String = answers["scriptName"] as String

    override fun generate(bindings: Map<String, Any>) {
        val name = model

        val dbPath = projectStructure.getModule(ModuleStructure.CORE_MODULE).path.resolve("db")


        val currentYearUpdateDir = dbPath.resolve("update", projectModel.database.type, getYear())
        if (!Files.exists(currentYearUpdateDir)) {
            Files.createDirectories(currentYearUpdateDir)
        }

        val todayPrefix = getTodayPrefix()

        val todayScriptsCount = Files.walk(currentYearUpdateDir, 1)
                .filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".sql") && it.fileName.toString().startsWith(todayPrefix) }
                .count()

        val scriptName = "$todayPrefix${1 + todayScriptsCount}-${name}.sql"

        val updateScriptPath = currentYearUpdateDir.resolve(scriptName)
        updateScriptPath.toFile().also { it.createNewFile() }
        printHelper.fileCreated(updateScriptPath)
    }

    private fun getYear() = (calendar[Calendar.YEAR] - 2000).toString()

    private fun getTodayPrefix() = "%s%02d%02d-".format(getYear(), calendar[Calendar.MONTH], calendar[Calendar.DAY_OF_MONTH])
}