package com.haulmont.cuba.cli.model

class ProjectModel {
    lateinit var name: String

    lateinit var namespace: String

    lateinit var rootPackage: String

    lateinit var rootPackageDirectory: String

    lateinit var group: String

    lateinit var version: String

    var copyright: String? = null

    lateinit var cubaVersion: String

    companion object {
        const val MODEL_NAME = "project"
    }
}