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

package com.haulmont.cuba.cli.core

import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Class to read .properties files.
 */
class Messages(clazz: Class<*>, messagesFileName: String = "messages.properties") {

    private val properties: Properties by lazy {
        messagesFileName.run {
            if (!endsWith(".properties")) {
                this + ".properties"
            } else this
        }.let {
            clazz.getResource(it)?.openStream()?.reader()
                    ?: throw RuntimeException("Unable to find messages file $messagesFileName for $clazz")
        }.let { reader ->
            Properties().apply {
                load(reader)
            }
        }
    }

    fun getMessage(name: String) = properties.getProperty(name) ?: ""

    fun getMessage(name: String, vararg args: Any) = properties.getProperty(name)?.format(*args) ?: ""

    operator fun get(name: String): String = getMessage(name)

    operator fun get(name: String, vararg args: Any): String = getMessage(name, *args)
}

fun localMessages(): ReadOnlyProperty<Any, Messages> = object : ReadOnlyProperty<Any, Messages> {
    override fun getValue(thisRef: Any, property: KProperty<*>) = Messages(thisRef.javaClass)
}