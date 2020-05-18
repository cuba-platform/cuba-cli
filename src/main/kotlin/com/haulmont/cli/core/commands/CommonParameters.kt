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

package com.haulmont.cli.core.commands

import com.beust.jcommander.DynamicParameter
import com.beust.jcommander.Parameter

object CommonParameters {
    @Parameter(names = ["--stacktrace"], description = "Show stacktrace", hidden = true)
    var stacktrace: Boolean = false
        private set

    @Parameter(names = ["--help"], help = true, description = "Show help", hidden = true)
    var help: Boolean = false
        private set

    @DynamicParameter(names = ["-P"], description = "Non interactive mode parameters", hidden = true)
    var nonInteractive: Map<String, Any> = mutableMapOf()

    fun reset() {
        stacktrace = false
        help = false
        (nonInteractive as MutableMap).clear()
    }
}