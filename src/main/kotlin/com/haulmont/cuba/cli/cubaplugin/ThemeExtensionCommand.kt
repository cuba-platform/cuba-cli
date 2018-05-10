package com.haulmont.cuba.cli.cubaplugin

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.Messages
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.PrintHelper
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.kodein
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

@Parameters(commandDescription = "Generates halo or havana themes extension")
class ThemeExtensionCommand : GeneratorCommand<ThemeExtensionModel>() {
    private val messages = Messages(javaClass)

    private val writer: PrintWriter by kodein.instance()

    private val printHelper: PrintHelper by kodein.instance()

    private val themesToExtend: List<String> by lazy {
        val themesDirectory = projectStructure.getModule(WEB_MODULE).path.resolve("themes")

        val alreadyExtended = if (Files.exists(themesDirectory)) {
            Files.walk(themesDirectory, 1)
                    .filter { it != themesDirectory && Files.isDirectory(it) }
                    .map { it.fileName.toString() }
                    .collect(Collectors.toList())
        } else listOf<String>()


        val restThemes = listOf("halo", "havana") - alreadyExtended

        restThemes.isNotEmpty() || fail("Halo and havana themes already extended")

        return@lazy restThemes
    }

    override fun getModelName(): String = ThemeExtensionModel.MODEL_NAME

    override fun QuestionsList.prompting() {
        if (themesToExtend.size > 1) {
            options("themeName", "Choose theme to extend", themesToExtend)
        } else {
            writer.println("Only ${themesToExtend.first()} theme rest to extend.")
        }

        confirmation("confirmed", messages.getMessage("themeExtension.confirmationMessage"))
    }

    override fun createModel(answers: Answers): ThemeExtensionModel {
        val confirmed = answers["confirmed"] as Boolean

        confirmed || fail("User rejected", silent = true)

        if (themesToExtend.size == 1) {
            return ThemeExtensionModel(themesToExtend.first())
        }
        return ThemeExtensionModel("themeName" from answers)
    }

    override fun generate(bindings: Map<String, Any>) {
        val targetDirectory = projectStructure.getModule(WEB_MODULE).path
                .resolve(Paths.get("themes", model.themeName))

        TemplateProcessor(
                CubaPlugin.TEMPLATES_BASE_PATH + "themes/" + model.themeName,
                bindings, projectModel.platformVersion) {
            transformWhole(to = targetDirectory)
        }

        val moduleRegistered = projectStructure.settingsGradle
                .toFile()
                .readLines()
                .contains(messages.getMessage("themeExtension.settingsGradle.moduleRegistration"))

        if (!moduleRegistered) {
            registerModule()
        }
    }

    private fun registerModule() {
        val settingsGradle = projectStructure.settingsGradle

        val lines = settingsGradle.toFile().readLines().map {
            if (it.startsWith("include(")) {
                it.replace(Regex("include\\((.*)\\)")) {
                    val modules = it.groupValues[1].split(",")
                    (modules + "\":\${modulePrefix}-web-themes\"").joinToString(" ,", "include(", ")")
                }
            } else it
        } + messages.getMessage("themeExtension.settingsGradle.moduleRegistration")

        settingsGradle.toFile().writeText(lines.joinToString("\n"))

        printHelper.fileAltered(settingsGradle)


        val buildGradle = projectStructure.buildGradle
        buildGradle.toFile()
                .readText()
                .replace(messages.getMessage("themeExtension.buildGradle.webModuleSearch"),
                        messages.getMessage("themeExtension.buildGradle.webModuleReplace"))
                .replace(
                        messages.getMessage("themeExtension.buildGradle.configureWebModuleSearch").replace("\t", "    "),
                        messages.getMessage("themeExtension.buildGradle.configureWebModuleReplace").replace("\t", "    "))
                .let {
                    buildGradle.toFile().writeText(it)
                }
        printHelper.fileAltered(buildGradle)
    }
}

class ThemeExtensionModel(val themeName: String) {
    companion object {
        const val MODEL_NAME = "theme"
    }
}