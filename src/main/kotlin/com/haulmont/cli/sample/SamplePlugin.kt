package com.haulmont.cli.sample

import com.google.common.eventbus.Subscribe
import com.haulmont.cli.core.*
import com.haulmont.cli.core.event.InitPluginEvent
import com.haulmont.cli.sample.commands.HelloWorldCommand
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Suppress("UNUSED_PARAMETER")
class SamplePlugin : MainCliPlugin {

    override val priority: Int = 900
    override val prompt: String = "sample>"

    override fun welcome() {
        printWelcome()
    }

    override val apiVersion: Int = API_VERSION

    private val writer: PrintWriter by kodein.instance<PrintWriter>()

    private val context: CliContext by kodein.instance<CliContext>()

    @Subscribe
    fun onInit(event: InitPluginEvent) {
        event.commandsRegistry {
            command("hello", HelloWorldCommand())
        }
    }

    private fun printWelcome() {
        writer.println("SAMPLE CLI".blue())
        writer.println()
    }
}