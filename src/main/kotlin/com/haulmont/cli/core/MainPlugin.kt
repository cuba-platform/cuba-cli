package com.haulmont.cli.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MainPlugin(val prompt: String = "", val priority: Int = 0)