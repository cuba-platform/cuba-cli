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

package com.haulmont.cuba.cli.cubaplugin.project

import com.haulmont.cuba.cli.core.commands.CommandExecutionException
import com.haulmont.cuba.cli.core.commands.CommonParameters
import com.haulmont.cuba.cli.core.localMessages
import com.haulmont.cuba.cli.core.prompting.Answers

class ProjectInitModel(answers: Answers) {
    val projectName: String by answers
    val namespace: String by answers
    val rootPackage: String by answers
    val repo by answers
    val platformVersion: String = run {
        if ("platformVersion" in answers)
            answers["platformVersion"]
        else
            answers["predefinedPlatformVersion"]
    } as String
    val rootPackageDirectory: String = rootPackage.replace('.', '/')
    val database: DatabaseModel = DatabaseModel(answers)
    val kotlinSupport: Boolean by answers.withDefault { false }
    val kotlinVersion: String by answers.withDefault { "1.3.41" }
}

class DatabaseModel(answers: Answers) {
    private val messages by localMessages()

    private val databases = messages["databases"].split(',')
    private val aliases = messages["databaseAliases"].split(',')

    val database: String = (answers["database"] as String).let {
        if (CommonParameters.nonInteractive.isEmpty())
            it
        else
            aliases.zip(databases).toMap()[it]!!
    }

    val schema: String
    val driver: String
    val driverDependency: String
    val driverDependencyName: String
    val username: String
    val password: String
    val connectionParams: String = if (database == databases.last()) {
        "?useSSL=false&amp;allowMultiQueries=true"
    } else ""

    init {
        when (database) {
            databases[0] -> {
                schema = "jdbc:hsqldb:hsql:"
                driver = "org.hsqldb.jdbc.JDBCDriver"
                driverDependency = "'org.hsqldb:hsqldb:2.4.1'"
                driverDependencyName = "hsql"
                username = "sa"
                password = ""
            }
            databases[1] -> {
                schema = "jdbc:postgresql:"
                driver = "org.postgresql.Driver"
                driverDependency = "'org.postgresql:postgresql:9.4.1212'"
                driverDependencyName = "postgres"
                username = "cuba"
                password = "cuba"
            }
            databases[2] -> {
                schema = "jdbc:sqlserver:"
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                driverDependency = "'com.microsoft.sqlserver:mssql-jdbc:6.4.0.jre8'"
                driverDependencyName = "mssql"
                username = "sa"
                password = "saPass1"
            }
            databases[3] -> {
                schema = "jdbc:jtds:sqlserver:"
                driver = "net.sourceforge.jtds.jdbc.Driver"
                driverDependency = "'net.sourceforge.jtds:jtds:1.3.1'"
                driverDependencyName = "mssql"
                username = "sa"
                password = "saPass1"
            }
            databases[4] -> {
                schema = "jdbc:sqlserver:"
                driver = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
                driverDependency = "'com.microsoft.sqlserver:mssql-jdbc:6.4.0.jre8'"
                driverDependencyName = "mssql"
                username = "sa"
                password = "saPass1"
            }
            databases[5] -> {
                schema = "jdbc:oracle:thin:@"
                driver = "oracle.jdbc.OracleDriver"
                driverDependency = "files(\"\$cuba.tomcat.dir/lib/ojdbc6.jar\")"
                driverDependencyName = "oracle"
                username = (answers["projectName"] as String).replace('-', '_')
                password = "cuba"
            }
            databases[6] -> {
                schema = "jdbc:mysql:"
                driver = "com.mysql.jdbc.Driver"
                driverDependency = "'mysql:mysql-connector-java:5.1.38'"
                driverDependencyName = "mysql"
                username = "cuba"
                password = "cuba"
            }
            else -> throw CommandExecutionException("Unsupported database")
        }
    }
}