package com.haulmont.cuba.cli

import com.google.common.eventbus.EventBus
import com.haulmont.cuba.cli.event.CliEvent
import com.haulmont.cuba.cli.event.FailEvent
import com.haulmont.cuba.cli.event.ModelRegisteredEvent
import java.io.File

class CliContext {
    private val models: MutableMap<String, Any> = mutableMapOf()

    private val eventBus: EventBus = EventBus { throwable: Throwable, _ ->
        fail(throwable)
    }

    val currentDir: File = File("").absoluteFile

    val tempDir: File by lazy(::createTempDir)

    fun <T : Any> getModel(key: String): T = (models[key] as T)

    fun hasModel(key: String): Boolean = models.containsKey(key)

    fun <T : Any> addModel(key: String, model: T) {
        models[key] = model
        postEvent(ModelRegisteredEvent(key))
    }

    internal fun clearModels() = models.clear()

    fun registerListener(listener: Any) = eventBus.register(listener)
    fun unregisterListener(listener: Any) = eventBus.unregister(listener)

    fun getModels(): Map<String, Any> = models.toMap()

    fun postEvent(event: CliEvent) = eventBus.post(event)

    fun fail(cause: String?) {
        postEvent(FailEvent())
        println(cause)
        System.exit(1)
    }

    fun fail(cause: Throwable) {
        postEvent(FailEvent(cause))
        cause.printStackTrace(System.err)
        System.exit(1)
    }
}

private fun createTempDir(): File = TODO()