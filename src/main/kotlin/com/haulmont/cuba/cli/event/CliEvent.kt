package com.haulmont.cuba.cli.event

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.commands.CommandsRegistry
import com.haulmont.cuba.cli.commands.CliCommand

interface CliEvent

class InitPluginEvent(val commandsRegistry: CommandsRegistry) : CliEvent

class BeforeCommandExecutionEvent(val command: CliCommand) : CliEvent

class ModelRegisteredEvent(val modelName: String) : CliEvent

class AfterCommandExecutionEvent(val command: CliCommand) : CliEvent

class DestroyPluginEvent : CliEvent

class FailEvent(val cause: Throwable? = null) : CliEvent