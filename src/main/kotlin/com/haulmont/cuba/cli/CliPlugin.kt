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

package com.haulmont.cuba.cli

/**
 * Base interface for all plugins.
 *
 * To make CUBA CLI able to load your plugin your should add
 * ```
 * provides com.haulmont.cuba.cli.CliPlugin with `your module class`;
 * ```
 * to module-info.java.
 *
 * Lifecycle is served by Guava EventBus. After plugin loading it will be registered in event bus.
 *
 * To subscribe an event simply add void method with single parameter of the event type and
 * mark the method with an annotation [com.google.common.eventbus.Subscribe].
 *
 * Available events are:
 * * [com.haulmont.cuba.cli.event.InitPluginEvent]
 * * [com.haulmont.cuba.cli.event.BeforeCommandExecutionEvent]
 * * [com.haulmont.cuba.cli.event.AfterCommandExecutionEvent]
 * * [com.haulmont.cuba.cli.event.ModelRegisteredEvent]
 * * [com.haulmont.cuba.cli.event.DestroyPluginEvent]
 * * [com.haulmont.cuba.cli.event.ErrorEvent]
 *
 * After CLI is launched it fires [InitPluginEvent], and all subscribed plugins may register their commands.
 * Before CLI is closed it fires [DestroyEvent].
 *
 * We use [Kodein-DI](http://kodein.org/Kodein-DI/) as a dependency injection container.
 *
 * All default dependencies are available throw the [com.haulmont.cuba.cli.kodein] instance.
 * But if you need to provide your own dependencies in your plugin, you can extend default kodein,
 * and use it inside your plugin.
 *
 * ```
 * import com.haulmont.cuba.cli.kodein
 * val localKodein = Kodein {
 *  extend(kodein)
 *
 *  bind<Worker>() with singleton { Worker() }
 * }
 *
 * ...
 *
 * private val worker: Worker by localKodein.instance()
 * ```
 *
 * @see com.haulmont.cuba.cli.cubaplugin.CubaPlugin
 */
interface CliPlugin {
    val resources: ResourcesPath
        get() = NoResources

    val apiVersion: Int
}

sealed class ResourcesPath
object NoResources: ResourcesPath()
class HasResources(val resourcesBasePath: String) : ResourcesPath()