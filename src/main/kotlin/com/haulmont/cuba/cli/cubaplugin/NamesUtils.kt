package com.haulmont.cuba.cli.cubaplugin

import com.google.common.base.CaseFormat
import java.io.File

class NamesUtils {
    fun packageToDirectory(packageName: String) = packageName.replace('.', File.separatorChar)

    fun entityNameToTableName(entityName: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entityName)
}