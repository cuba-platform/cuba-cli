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

package com.haulmont.cuba.cli.cubaplugin.screenextension

import com.haulmont.cuba.cli.prompting.Answers

class ScreenExtensionModel(answers: Answers) {
    val screen: String by answers
    val packageName: String by answers
    val id: String
    val descriptor: String

    init {
        if (screen == "login") {
            id = "loginWindow"
            descriptor = "ext-loginWindow.xml"
        } else {
            id = "mainWindow"
            descriptor = "ext-mainwindow.xml"
        }
    }

    companion object {
        const val MODEL_NAME = "screen"
    }
}