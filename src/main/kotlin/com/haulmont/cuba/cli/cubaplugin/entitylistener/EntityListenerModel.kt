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

import com.haulmont.cuba.cli.prompting.Answers

class EntityListenerModel(answers: Answers) {
    val className: String by answers
    val packageName: String by answers
    val beanName: String by answers

    private val interfaces: Answers by answers

    val beforeInsert: Boolean by interfaces
    val beforeUpdate: Boolean by interfaces
    val beforeDelete: Boolean by interfaces
    val afterInsert: Boolean by interfaces
    val afterUpdate: Boolean by interfaces
    val afterDelete: Boolean by interfaces
    val beforeAttach: Boolean by interfaces
    val beforeDetach: Boolean by interfaces

    val entityName: String
    val entityPackageName: String

    init {
        val entityType: String by answers
        val lastDotIndex = entityType.lastIndexOf('.')
        entityName = if (lastDotIndex == -1) entityType else entityType.substring(lastDotIndex + 1)
        entityPackageName = entityType.removeSuffix(".$entityName")
    }

    companion object {
        const val MODEL_NAME = "listener"
    }
}