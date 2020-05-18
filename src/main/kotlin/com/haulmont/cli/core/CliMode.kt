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

package com.haulmont.cli.core

/**
 * Cli may be started in two modes.
 *
 * [SHELL] is an interactive mode, when cli takes user command, evaluate it and waiting for next command.
 *
 * [SINGLE_COMMAND] is mode, that runs only one command, that user specified in command line parameters.
 */
enum class CliMode {
    SHELL,
    SINGLE_COMMAND
}