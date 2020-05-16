package com.haulmont.cuba.cli

import com.haulmont.cli.core.Resources
import java.nio.file.Path

    fun Resources.getTemplate(templateName: String): Path {
        return getResourcePath(resourcesBasePath + "templates/" + templateName)
                ?: throw RuntimeException("Template $templateName not found in ${cliPlugin.javaClass} plugin")
    }

    fun Resources.getSnippets(snippetsBasePath: String): Path {
        return getResourcePath(resourcesBasePath + "snippets/" + snippetsBasePath)
                ?: throw RuntimeException("Snippets $snippetsBasePath not found in ${cliPlugin.javaClass} plugin")

    }

    fun Resources.getResourcePath(resourceName: String): Path? {
        return Resources.getResourcePath(resourceName, cliPlugin.javaClass)
    }
