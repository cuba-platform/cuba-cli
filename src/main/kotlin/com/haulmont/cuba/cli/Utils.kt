package com.haulmont.cuba.cli

import com.google.common.base.CaseFormat


/**
 * Converts EntityName to ENTITY_NAME
 */
fun entityNameToTableName(entityName: String): String = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, entityName)