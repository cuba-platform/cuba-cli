package com.haulmont.cuba.cli.cubaplugin

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

@Parameters(commandDescription = "Create new entity")
class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    override fun QuestionsList.prompting(context: CliContext) {

        val projectModel = context.getModel<ProjectModel>("project")

        question("entityName", "Entity Name") {
            validate {
                checkRegex("\\b[A-Z]+[\\w\\d_$]*", "Invalid entity name")
            }
        }

        question("packageName", "Package Name") {
            default { "${projectModel.rootPackage}.entity" }
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