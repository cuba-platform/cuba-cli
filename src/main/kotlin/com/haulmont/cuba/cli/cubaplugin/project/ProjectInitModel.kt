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

import com.haulmont.cuba.cli.commands.CommandExecutionException
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.localMessages
import com.haulmont.cuba.cli.prompting.Answers
import java.io.File

class ProjectInitModel(answers: Answers) {
    val projectName: String by answers
    val namespace: String by answers
    val rootPackage: String by answers
    val platformVersion: String by answers
    val rootPackageDirectory: String = rootPackage.replace('.', File.separatorChar)
    val database: DatabaseModel = DatabaseModel("database" from answers)
}

class DatabaseModel(val database: String) {
    private val messages by localMessages()

    private val DATABASES = messages["databases"].split(',')

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