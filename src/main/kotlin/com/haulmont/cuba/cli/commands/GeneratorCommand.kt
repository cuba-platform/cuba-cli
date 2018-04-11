package com.haulmont.cuba.cli.commands

import com.haulmont.cuba.cli.CliContext
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.Prompts
import com.haulmont.cuba.cli.prompting.QuestionsList

abstract class GeneratorCommand<out Model : Any> : AbstractCommand() {
    override fun execute(context: CliContext) {
        super.execute(context)
        val questions = QuestionsList { prompting(context) }
        val answers = Prompts(System.`in`, System.out, questions).ask()
        val model = createModel(context, answers)
        context.addModel(getModelName(), model)

        val bindings: MutableMap<String, Any> = mutableMapOf()
        context.getModels().toMap(bindings)
        beforeGeneration(context, bindings)

        generate(context, bindings.toMap())
    }

    abstract fun getModelName(): String

    abstract fun QuestionsList.prompting(context: CliContext)

    abstract fun createModel(context: CliContext, answers: Answers): Model

    open fun beforeGeneration(context: CliContext, bindings: MutableMap<String, Any>) {}

    abstract fun generate(context: CliContext, bindings: Map<String, Any>)
}