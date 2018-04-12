package com.haulmont.cuba.cli.cubaplugin

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import com.haulmont.cuba.cli.template.TemplateProcessor
import java.io.File
import java.nio.file.Paths

@Parameters(commandDescription = "Create new entity")
class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    override fun getModelName(): String = EntityModel.MODEL_NAME

    override fun QuestionsList.prompting() {

        val projectModel = context.getModel<ProjectModel>("project")

        question("entityName", "Entity Name") {
            validate {
                checkRegex("\\b[A-Z]+[\\w\\d_$]*", "Invalid entity name")
            }
        }

        question("packageName", "Package Name") {
            default { "${projectModel.rootPackage}.entity" }
            validate {
                checkIsPackage()
            }
        }

        options("entityType", "Entity type", entityTypes) {
            default(2)
        }
    }

    override fun createModel(answers: Answers): EntityModel {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)
        val entityName = answers["entityName"] as String

        val tableName = buildString {
            append(projectModel.namespace.toUpperCase())
            append("_")
            append(getTableName(entityName))
        }

        return EntityModel(
                entityName,
                answers["packageName"] as String,
                answers["entityType"] as String,
                tableName
        )
    }

    override fun checkPreconditions() {
        onlyInProject(context)
    }

    override fun beforeGeneration(bindings: MutableMap<String, Any>) {
        val entityModel = context.getModel<EntityModel>(EntityModel.MODEL_NAME)

        bindings["packageDirectoryName"] = entityModel.packageName.replace('.', File.separatorChar)
        bindings["entityName"] = entityModel.name

        super.beforeGeneration(bindings)
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor("templates/entity")
                .copyTo(Paths.get(""), bindings)
    }
}

data class EntityModel(
        val name: String, val packageName: String, val type: String, val tableName: String) {
    companion object {
        const val MODEL_NAME = "entity"
    }
}

private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")

private fun getTableName(entityName: String): String {
    val camelCaseRegex = Regex("[A-Z][a-z0-9]+")

    return camelCaseRegex.findAll(entityName).map { it.value }
            .fold(listOf<String>() to entityName) { (tableNameParts, restEntityName), camelPart ->
                val camelPartIndex = restEntityName.indexOf(camelPart)
                when (camelPartIndex) {
                    0 -> (tableNameParts + camelPart) to
                            restEntityName.substring(camelPart.length)
                    else -> (tableNameParts + restEntityName.substring(0, camelPartIndex) + camelPart) to
                            restEntityName.substring(camelPartIndex + camelPart.length)
                }
            }
            .let { (tableNameParts, restEntityName) ->
                if (restEntityName.isNotBlank())
                    tableNameParts + restEntityName
                else
                    tableNameParts
            }.joinToString("_") { it.toUpperCase() }
}