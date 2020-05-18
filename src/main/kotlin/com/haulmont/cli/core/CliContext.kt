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

import com.google.common.eventbus.EventBus
import com.haulmont.cli.core.event.ModelRegisteredEvent
import org.kodein.di.generic.instance

/**
 * [CliContext] stores models, that are used for artifact generation.
 *
 * In common case, models are added to the context by plugins, during [com.haulmont.cli.core.event.BeforeCommandExecutionEvent],
 * and by commands, during execution, before generation is started.
 *
 * After every command execution the context is cleared.
 */
class CliContext {
    private val bus: EventBus by kodein.instance<EventBus>()

    private val models: MutableMap<String, Any> = mutableMapOf()

    internal val plugins: List<CliPlugin> = mutableListOf()

    internal var mainPlugin: CliPlugin? = null

    /**
     * Retrieves model by [key].
     * The method produces exception, if the model doesn't exist,
     * so if there is no assurance of model existence, firstly check it with [hasModel].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getModel(key: String): T = (models[key]!! as T)

    fun hasModel(key: String): Boolean = models.containsKey(key)

    /**
     * Saves model in the context and fires [ModelRegisteredEvent].
     */
    fun <T : Any> addModel(key: String, model: T) {
        models[key] = model
        bus.post(ModelRegisteredEvent(key))
    }

    fun mainPlugin() = mainPlugin

    fun prompt() = readMainPluginAnnotation(mainPlugin)?.prompt ?: ">"

    fun clearModels() = models.clear()

    fun registerPlugin(plugin: CliPlugin) {
        (plugins as MutableList).add(plugin)
        readMainPluginAnnotation(plugin)?.let {
            if (mainPlugin == null || readMainPluginAnnotation(mainPlugin!!)!!.priority < it.priority) {
                mainPlugin = plugin
            }
        }
    }

    private fun readMainPluginAnnotation(plugin: CliPlugin?) =
            plugin?.let {
                it::class.java.annotations.find { it is MainPlugin } as? MainPlugin
            }

    fun getResources(pluginClass: Class<out CliPlugin>): Resources {
        return plugins.filterIsInstance(pluginClass)
                .firstOrNull()?.let {
                    Resources(it)
                } ?: throw RuntimeException("Plugin $pluginClass was not loaded")
    }

    /**
     * Returns all containing models.
     */
    fun getModels(): Map<String, Any> = models.toMap()
}