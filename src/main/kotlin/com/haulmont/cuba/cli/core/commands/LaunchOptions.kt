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

package com.haulmont.cuba.cli.core.commands

import com.beust.jcommander.Parameter

object LaunchOptions {
    @Parameter(hidden = true)
    var shell: String = ""

    @Parameter(names = ["--versionsConfigUrl"], description = "Url of file with available cuba platform versions")
    var versionsConfigUrl: String = "http://files.cuba-platform.com/cuba/studio/studio-config.json?source=cuba-cli"
        private set

    @Parameter(names = ["--skipVersionLoading"], description = "If is set, will skip available versions loading and uses default")
    var skipVersionLoading: Boolean = false
        private set

    @Parameter(names = ["--debug"], description = "If is set, will print CUBA CLI debug info")
    var debug: Boolean = false
        private set
}