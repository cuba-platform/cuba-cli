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

package com.haulmont.cuba.cli.cubaplugin.service

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.updateXml
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList

@Parameters(commandDescription = "Create new CUBA service")
class CreateServiceCommand : GeneratorCommand<ServiceModel>() {
    override fun getModelName(): String = ServiceModel.MODEL_NAME

    override fun preExecute() = checkProjectExistence()

    override fun QuestionsList.prompting() {
        question("interfaceName", "Service interface name") {
            validate {
                if (!value.endsWith("Service")) {
                    fail("Service name should ends with \"Service\"")
                }
                checkIsClass()
            }
        }
        question("beanName", "Service bean name") {
            validate {
                checkIsClass()
            }
            default { (it["interfaceName"] as String) + "Bean" }
        }
        question("packageName", "Package name") {
            default { projectModel.rootPackage + ".service" }
            validate {
                checkIsPackage()
            }
        }
        question("serviceName", "Service name") {
            default { projectModel.namespace + "_" + it["interfaceName"] }
        }
    }

    override fun createModel(answers: Answers): ServiceModel = ServiceModel(answers)

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "service", bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val springXml = projectStructure.getModule(WEB_MODULE).springXml

        updateXml(springXml) {
            "bean" {
                "class" mustBe "com.haulmont.cuba.web.sys.remoting.WebRemoteProxyBeanCreator"
                "property" {
                    "name" mustBe "serverSelector"
                    "ref" mustBe "cuba_ServerSelector"
                }
                "property" {
                    "name" mustBe "remoteServices"
                    "map" {
                        add("entry") {
                            "key" mustBe model.serviceName
                            "value" mustBe "${model.packageName}.${model.interfaceName}"
                        }
                    }
                }
            }
        }
    }
}

