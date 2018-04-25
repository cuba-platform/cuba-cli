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

package com.haulmont.cuba.cli.cubaplugin

import com.google.common.base.CaseFormat
import java.io.File

class NamesUtils {
    fun packageToDirectory(packageName: String) = packageName.replace('.', File.separatorChar)

    fun entityNameToTableName(entityName: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entityName)

    fun join(one: String, another: String, on: String): String =
            if (one.isEmpty())
                another
            else
                "$one$on$another"
}