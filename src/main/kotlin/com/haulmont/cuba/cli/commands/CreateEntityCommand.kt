package com.haulmont.cuba.cli.commands

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.GeneratorCommand
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import java.io.File


class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    override fun QuestionsList.prompting(context: CliContext) {
        question("entityName", "Entity Name") {
            validate {
                checkRegex("\\b[A-Z]+[\\w\\d_$]*", "Invalid entity getName")
            }
        }

        question("packageName", "Package Name") {
            default { getDefaultPackageName() }
        }

        options("entityType", "Entity type", entityTypes)
        options("idType", "Id type", idTypes)
    }

    override fun createModel(context: CliContext, answers: Answers): EntityModel {
        return EntityModel(
                answers["entityName"] as String,
                answers["packageName"] as String,
                answers["entityType"] as String,
                answers["idType"] as String
        )
    }

    override fun checkPreconditions(context: CliContext) {
        onlyInProject(context)
    }

    override fun generate(context: CliContext, bindings: Map<String, Any>) {

    }

    override fun getModelName(): String = "entity"
}


data class EntityModel(val name: String, val packageName: String, val type: String, val idType: String)

private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")
private val idTypes = listOf("UUID", "String", "Integer", "Long", "Long Identity", "Integer Identity")
private val globalModuleDir = File("modules/global/src")

private fun getDefaultPackageName(): String {
//    assumption that persistence.xml would lay in root package
    val maybeRootPackageDirectory = globalModuleDir.walkTopDown()
            .filter { it.name == "persistence.xml" }
            .firstOrNull()
            ?.parentFile

    val maybeRootPackage = maybeRootPackageDirectory
            ?.relativeTo(globalModuleDir)
            ?.toString()

    return maybeRootPackage?.replace(File.separatorChar, '.')?.let { "$it.entity" }
            ?: "com.company.entity"
}
