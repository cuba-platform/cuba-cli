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

        options("platformVersion", "Platform version", availablePlatformVersions) {
            default(0)
        }

        options("database", "Choose database", availableDatabases) {
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

//        TODO move texts to resources

        writer.println("""

            @|white Project generated at ${cwd.toAbsolutePath()} directory
            To build project execute `gradlew assemble`
            To import project into IDE execute `gradlew ide` and open generated *.iml file|@

        """.trimIndent())

        val model = context.getModel<ProjectInitModel>("project")
        when (model.database.database) {
//            oracle
            availableDatabases[5] -> writer.println("""
            @|white You have selected Oracle Database.

            Oracle does not allow to redistribute its JDBC driver, so do the following:
            > Download ojdbc6.jar from www.oracle.com.
            > Run Deploy from the main menu, then copy ojdbc6.jar into {your_project}/deploy/tomcat/lib directory.
            > Copy ojdbc6.jar into ${'$'}{user.home}/.haulmont/studio/lib directory.
            > Stop Studio server (press Exit button).
            > Kill Gradle daemon by running gradle --stop, or killing its process in task manager, or restarting the operating system.
            > Launch Studio server again.|@

        """.trimIndent())
//            mysql
            availableDatabases[6] -> writer.println("""
            @|white You have selected MySQL database.

            MySQL does not allow to redistribute its JDBC driver, so do the following:
            > Download MySQL Connector/J archive from dev.mysql.com/downloads/connector/j.
            > Extract mysql-connector-java-*.jar file from the archive and rename it to mysql-connector-java.jar
            > Copy mysql-connector-java.jar into ${'$'}{user.home}/.haulmont/studio/lib directory.
            > Stop Studio server (press Exit button).
            > Kill Gradle daemon by running gradle --stop, or killing its process in task manager, or restarting the operating system.
            > Launch Studio server again.|@

        """.trimIndent())
        }
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
    val connectionParams: String = if (database == availableDatabases.last()) {
        "?useSSL=false&amp;allowMultiQueries=true"
    } else ""

    init {
        when (database) {
            availableDatabases[0] -> {
                schema = "jdbc:hsqldb:hsql:"
                driver = "org.hsqldb.jdbc.JDBCDriver"
                driverDependency = "\"org.hsqldb:hsqldb:2.2.9\""
                driverDependencyName = "hsql"
            }
            availableDatabases[1] -> {
                schema = "jdbc:postgresql:"
                driver = "org.postgresql.Driver"
                driverDependency = "\"org.postgresql:postgresql:9.4.1212\""
                driverDependencyName = "postgres"
            }
            availableDatabases[2] -> {
                schema = "jdbc:sqlserver:"
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                driverDependency = "\"com.microsoft.sqlserver:mssql-jdbc:6.2.1.jre8\""
                driverDependencyName = "mssql"
            }
            availableDatabases[3] -> {
                schema = "jdbc:jtds:sqlserver:"
                driver = "net.sourceforge.jtds.jdbc.Driver"
                driverDependency = "\"net.sourceforge.jtds:jtds:1.3.1\""
                driverDependencyName = "mssql"
            }
            availableDatabases[4] -> {
                schema = "jdbc:sqlserver:"
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                driverDependency = "\"com.microsoft.sqlserver:mssql-jdbc:6.2.1.jre8\""
                driverDependencyName = "mssql"
            }
            availableDatabases[5] -> {
                schema = "jdbc:oracle:thin:@"
                driver = "oracle.jdbc.OracleDriver"
                driverDependency = "files(\"\$cuba.tomcat.dir/lib/ojdbc6.jar\")"
                driverDependencyName = "oracle"
            }
            availableDatabases[6] -> {
                schema = "jdbc:mysql:"
                driver = "com.mysql.jdbc.Driver"
                driverDependency = "\"mysql:mysql-connector-java:5.1.38\""
                driverDependencyName = "mysql"
            }
            else -> throw CommandExecutionException("Unsupported database")
        }
    }
}

private val availablePlatformVersions = listOf("6.8.5", "6.9-SNAPSHOT")
private val availableDatabases = listOf("HSQLDB", "PostgreSQL", "Microsoft SQL Server", "Microsoft SQL Server 2005", "Microsoft SQL Server 2012+", "Oracle Database", "MySQL")