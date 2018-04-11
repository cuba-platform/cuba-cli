package com.haulmont.cuba.cli.event

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.CommandsRegistry
import com.haulmont.cuba.cli.commands.CliCommand

interface CliEvent

class InitPluginEvent(val cliContext: CliContext, val commandsRegistry: CommandsRegistry) : CliEvent

class BeforeCommandExecutionEvent(val command: CliCommand) : CliEvent

class ModelRegisteredEvent(val modelName: String) : CliEvent

class AfterCommandExecutionEvent(val command: CliCommand) : CliEvent

class DestroyPluginEvent : CliEvent

class FailEvent(val cause: Throwable? = null) : CliEvent