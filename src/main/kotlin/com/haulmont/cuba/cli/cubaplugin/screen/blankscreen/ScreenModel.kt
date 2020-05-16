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

package com.haulmont.cuba.cli.cubaplugin.screen.blankscreen

import com.haulmont.cli.core.prompting.Answers

class ScreenModel(answers: Answers) {
    val screenId: String by answers
    val descriptorName: String by answers
    val controllerName: String by answers
    val packageName: String by answers
    val addToMenu: Boolean by answers

    companion object {
        const val MODEL_NAME = "screen"
    }
}