package com.haulmont.cuba.cli.cubaplugin

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleType
import com.haulmont.cuba.cli.ProjectFiles
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.entityNameToTableName
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.generation.save
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.model.ProjectModel
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil
import org.kodein.di.generic.instance
import java.io.File
import java.io.PrintWriter
import java.nio.file.Path
import java.nio.file.Paths

@Parameters(commandDescription = "Create new entity")
class CreateEntityCommand : GeneratorCommand<EntityModel>() {
    val writer: PrintWriter by kodein.instance()

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
            default(1)
        }
    }

    override fun createModel(answers: Answers): EntityModel {
        val projectModel = context.getModel<ProjectModel>(ProjectModel.MODEL_NAME)
        val entityName = answers["entityName"] as String

        val tableName = buildString {
            append(projectModel.namespace.toUpperCase())
            append("_")
            append(entityNameToTableName(entityName))
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
        val entityModel = context.getModel<EntityModel>(EntityModel.MODEL_NAME)

        val projectFiles = ProjectFiles()

        TemplateProcessor("templates/entity")
                .copyTo(Paths.get(""), bindings)

        if (entityModel.type == "Not persistent") {
            val metadataXml = projectFiles.getModule(ModuleType.GLOBAL).metadataXml
            addEntityToConfig(metadataXml, "metadata-model", entityModel)
        } else {
            val persistenceXml = projectFiles.getModule(ModuleType.GLOBAL).persistenceXml
            addEntityToConfig(persistenceXml, "persistence-unit", entityModel)
        }
    }

    private fun addEntityToConfig(configPath: Path, elementName: String, entityModel: EntityModel) {
        val document = parse(configPath)

        val targetNode = DomUtil.getChild(document.documentElement, elementName)

        DomUtil.appendChild(targetNode, "class").apply {
            textContent = entityModel.packageName + "." + entityModel.name
        }

        save(document, configPath)

        writer.println("\t@|green altered|@\t$configPath")
    }
}

data class EntityModel(val name: String, val packageName: String, val type: String, val tableName: String) {
    companion object {
        const val MODEL_NAME = "entity"
    }
}

private val entityTypes = listOf("Persistent", "Persistent embedded", "Not persistent")