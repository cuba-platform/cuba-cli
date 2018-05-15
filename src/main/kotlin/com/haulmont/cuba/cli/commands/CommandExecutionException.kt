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

package com.haulmont.cuba.cli.commands

/**
 * Represents exception occurred during command execution.
 *
 * Normally, exception message will be showed to user.
 * In shell mode exception will be stored and it stacktrace will be available to user by `stacktrace` command.
 *
 * If {@param silent} is true, user won't see any error message, and won't be able to access it by `stacktrace` command.
 * May be used to stop command execution by non-error causes, for example in case,
 * when user doesn't confirm some critical project change.
 */
class CommandExecutionException @JvmOverloads constructor(
        message: String,
        cause: Throwable? = null,
        val silent: Boolean = false) : Exception(message, cause)