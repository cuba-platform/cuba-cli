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

package com.haulmont.cuba.cli.cubaplugin

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.Messages
import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.commands.nameFrom
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

private val messages: Messages = Messages(ProjectInitCommand::class.java)

private val PLATFORM_VERSIONS = messages.getMessage("createProject.platformVersions").split(',')
private val DATABASES = messages.getMessage("createProject.databases").split(',')

@Parameters(commandDescription = "Create new project")
class ProjectInitCommand : GeneratorCommand<ProjectInitModel>() {
    private val writer: PrintWriter by kodein.instance()

    override fun getModelName(): String = "project"

    override fun checkPreconditions() {
        super.checkPreconditions()

        check(!context.hasModel("project")) { "There is an existing project found in current directory." }
    }

    override fun QuestionsList.prompting() {
        question("projectName", "Project Name") {
            default { System.getProperty("user.dir").split(File.separatorChar).last() }

            validate {
                val invalidNameRegex = Regex("[^\\w\\-]")

                if (invalidNameRegex.find(value) != null) {
                    fail("Project name should contain only Latin letters, digits, dashes and underscores.")
                }

                if (value.isBlank()) {
                    fail("Empty names not allowed")
                }
            }
        }

        question("namespace", "Project Namespace") {
            default { answers ->
                val notAlphaNumeric = Regex("[^a-zA-Z0-9]")

                notAlphaNumeric.replace(answers["projectName"] as String, "")
            }

            validate {
                checkRegex("[a-zA-Z][a-zA-Z0-9]*", "Project namespace can contain only alphanumeric characters and start with a letter.")
            }
        }

        question("rootPackage", "Root package") {
            default { answers -> "com.company.${answers["namespace"]}" }
            validate {
                checkIsPackage()
            }
        }

        options("platformVersion", "Platform version", PLATFORM_VERSIONS) {
            default(0)
        }

        options("database", "Choose database", DATABASES) {
            default(0)
        }
    }

    override fun createModel(answers: Answers): ProjectInitModel = ProjectInitModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        val cwd = Paths.get("")

        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "project", bindings) {
            listOf("modules", "build.gradle", "settings.gradle").forEach {
                transform(it)
            }
            copy("gitignore")
            Files.move(Paths.get("gitignore"), Paths.get(".gitignore"))

            listOf("gradle", "gradlew", "gradlew.bat").forEach {
                copy(it)
            }
            try {
                Files.setPosixFilePermissions(Paths.get("gradlew"), setOf(PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_READ))
            } catch (e: Exception) {
                //todo warn if current system is *nix
            }
        }

        writer.println(messages.getMessage("createProject.createProjectTips", cwd.toAbsolutePath()))

        val model = context.getModel<ProjectInitModel>("project")
        val dpTipsMessageName = when (model.database.database) {
            DATABASES[5] -> "createProject.oracleDbTips"
            DATABASES[6] -> "createProject.mysqlDbTips"
            else -> return
        }

        writer.println(messages.getMessage(dpTipsMessageName))
    }
}

class ProjectInitModel(answers: Answers) {
    val projectName: String by nameFrom(answers)
    val namespace: String by nameFrom(answers)
    val rootPackage: String by nameFrom(answers)
    val platformVersion: String by nameFrom(answers)
    val rootPackageDirectory: String = rootPackage.replace('.', File.separatorChar)
    val database: DatabaseModel = DatabaseModel("database" from answers)
}

class DatabaseModel(val database: String) {
    val schema: String
    val driver: String
    val driverDependency: String
    val driverDependencyName: String
    val connectionParams: String = if (database == DATABASES.last()) {
        "?useSSL=false&amp;allowMultiQueries=true"
    } else ""

    init {
        when (database) {
            DATABASES[0] -> {
                schema = "jdbc:hsqldb:hsql:"
                driver = "org.hsqldb.jdbc.JDBCDriver"
                driverDependency = "\"org.hsqldb:hsqldb:2.2.9\""
                driverDependencyName = "hsql"
            }
            DATABASES[1] -> {
                schema = "jdbc:postgresql:"
                driver = "org.postgresql.Driver"
                driverDependency = "\"org.postgresql:postgresql:9.4.1212\""
                driverDependencyName = "postgres"
            }
            DATABASES[2] -> {
                schema = "jdbc:sqlserver:"
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                driverDependency = "\"com.microsoft.sqlserver:mssql-jdbc:6.2.1.jre8\""
                driverDependencyName = "mssql"
            }
            DATABASES[3] -> {
                schema = "jdbc:jtds:sqlserver:"
                driver = "net.sourceforge.jtds.jdbc.Driver"
                driverDependency = "\"net.sourceforge.jtds:jtds:1.3.1\""
                driverDependencyName = "mssql"
            }
            DATABASES[4] -> {
                schema = "jdbc:sqlserver:"
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                driverDependency = "\"com.microsoft.sqlserver:mssql-jdbc:6.2.1.jre8\""
                driverDependencyName = "mssql"
            }
            DATABASES[5] -> {
                schema = "jdbc:oracle:thin:@"
                driver = "oracle.jdbc.OracleDriver"
                driverDependency = "files(\"\$cuba.tomcat.dir/lib/ojdbc6.jar\")"
                driverDependencyName = "oracle"
            }
            DATABASES[6] -> {
                schema = "jdbc:mysql:"
                driver = "com.mysql.jdbc.Driver"
                driverDependency = "\"mysql:mysql-connector-java:5.1.38\""
                driverDependencyName = "mysql"
            }
            else -> throw CommandExecutionException("Unsupported database")
        }
    }
}