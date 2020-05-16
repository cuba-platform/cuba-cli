package com.haulmont.cuba.cli.core

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MainPlugin(val prompt: String = "", val priority: Int = 0)