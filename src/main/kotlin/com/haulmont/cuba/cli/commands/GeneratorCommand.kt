package com.haulmont.cuba.cli.commands

import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.Prompts
import com.haulmont.cuba.cli.prompting.QuestionsList

abstract class GeneratorCommand<out Model : Any> : AbstractCommand() {
    override fun execute() {
        super.execute()
        val questions = QuestionsList { prompting() }
        val answers = Prompts(questions).ask()
        val model = createModel(answers)
        context.addModel(getModelName(), model)

        val bindings: MutableMap<String, Any> = mutableMapOf()
        context.getModels().toMap(bindings)
        beforeGeneration(bindings)

        generate(bindings.toMap())
    }

    abstract fun getModelName(): String

    abstract fun QuestionsList.prompting()

    abstract fun createModel(answers: Answers): Model

    open fun beforeGeneration(bindings: MutableMap<String, Any>) {}

    abstract fun generate(bindings: Map<String, Any>)
}