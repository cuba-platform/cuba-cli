package com.haulmont.cuba.cli.event

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.CommandsRegistry

abstract class CliEvent(val cliContext: CliContext)

class LoadingEndEvent(cliContext: CliContext) : CliEvent(cliContext)

class ModelRegisteredEvent(cliContext: CliContext, val modelName: String) : CliEvent(cliContext)

class CommandRegisterEvent(cliContext: CliContext, val commandsRegistry: CommandsRegistry) : CliEvent(cliContext)

class BeforeGenerationEvent(cliContext: CliContext, val bindings: MutableMap<String, Any>) : CliEvent(cliContext)
