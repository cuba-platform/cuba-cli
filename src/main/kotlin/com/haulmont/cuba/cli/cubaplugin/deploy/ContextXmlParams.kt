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

package com.haulmont.cuba.cli.cubaplugin.deploy

import com.haulmont.cuba.cli.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

class ContextXmlParams(answers: Answers, projectModel: ProjectModel) {
    val dbUser: String by answers
    val dbPassword: String by answers
    val dbUrl : String = kotlin.run {
        val dbNamePart = if (projectModel.database.type == "mssql") {
            ";databaseName=" + answers["dbName"]
        } else "/" + answers["dbName"]
        projectModel.database.urlPrefix + answers["dbHost"] + dbNamePart + answers["connectionParams"]
    }

    companion object {
        fun QuestionsList.askContextXmlParams(projectModel: ProjectModel, questionName: String, askCondition: String? = null) {
            questionList(questionName) {
                askCondition?.let(::askIf)

                question("dbUser", "Database user") {
                    default(projectModel.database.username)
                }
                question("dbPassword", "Database password") {
                    default(projectModel.database.password)
                }
                val hostName = projectModel.database.connectionString
                        .removePrefix(projectModel.database.urlPrefix)
                        .replaceAfter(';', "").removeSuffix(";")
                        .replaceAfter('/', "").removeSuffix("/")
                val dbName = projectModel.database.connectionString
                        .removePrefix(projectModel.database.urlPrefix)
                        .removePrefix(hostName).removePrefix("/")
                        .removePrefix(";databaseName=")
                        .replaceAfter('?', "").removeSuffix("?")
                question("dbHost", "Host name") {
                    default(hostName)
                }
                question("dbName", "Database name") {
                    default(dbName)
                }
                question("connectionParams", "Connection params") {
                    default(projectModel.database.connectionString.replaceBefore('?', "", ""))
                }
            }
        }
    }
}