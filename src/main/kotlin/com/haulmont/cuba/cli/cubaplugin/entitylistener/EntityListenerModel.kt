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

package com.haulmont.cuba.cli.cubaplugin.entitylistener

import com.haulmont.cuba.cli.commands.from
import com.haulmont.cuba.cli.prompting.Answers

class EntityListenerModel(answers: Answers) {
    val className: String by answers
    val packageName: String by answers
    val beanName: String by answers

    val beforeInsert: Boolean by answers
    val beforeUpdate: Boolean by answers
    val beforeDelete: Boolean by answers
    val afterInsert: Boolean by answers
    val afterUpdate: Boolean by answers
    val afterDelete: Boolean by answers
    val beforeAttach: Boolean by answers
    val beforeDetach: Boolean by answers

    val entityName: String
    val entityPackageName: String

    init {
        val entity: String = "entityType" from answers
        val lastDotIndex = entity.lastIndexOf('.')
        entityName = if (lastDotIndex == -1) entity else entity.substring(lastDotIndex + 1)
        entityPackageName = entity.removeSuffix(".$entityName")
    }

    companion object {
        const val MODEL_NAME = "listener"
    }
}