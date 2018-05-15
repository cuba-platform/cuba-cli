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

package com.haulmont.cuba.cli.event

import com.haulmont.cuba.cli.CliMode
import com.haulmont.cuba.cli.commands.CliCommand
import com.haulmont.cuba.cli.commands.CommandsRegistry

interface CliEvent

/**
 * Fires after plugin loaded. Provides commandRegistry to make plugin able to register its commands.
 */
class InitPluginEvent(val commandsRegistry: CommandsRegistry, val cliMode: CliMode) : CliEvent

/**
 * Fires before every command execution.
 */
class BeforeCommandExecutionEvent(val command: CliCommand) : CliEvent

/**
 * Fires after every command execution.
 */
class AfterCommandExecutionEvent(val command: CliCommand) : CliEvent

class ModelRegisteredEvent(val modelName: String) : CliEvent

/**
 * Fires before cli exit.
 */
class DestroyPluginEvent : CliEvent

class FailEvent(val cause: Throwable? = null) : CliEvent