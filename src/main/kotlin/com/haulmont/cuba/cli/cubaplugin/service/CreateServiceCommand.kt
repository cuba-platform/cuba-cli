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
import com.haulmont.cuba.cli.ModuleStructure.Companion.CORE_MODULE
import com.haulmont.cuba.cli.ModuleStructure.Companion.GLOBAL_MODULE
import com.haulmont.cuba.cli.ModuleStructure.Companion.WEB_MODULE
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin
import com.haulmont.cuba.cli.generation.*
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil
import net.sf.practicalxml.xpath.XPathWrapper
import org.w3c.dom.Element

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

    override fun beforeGeneration() {
        projectStructure.getModule(CORE_MODULE)
                .resolvePackagePath(model.packageName)
                .resolve(model.beanName + ".java").let {
            ensureFileAbsence(it, "Bean \"${model.packageName}.${model.beanName}\" already exists")
        }
        projectStructure.getModule(GLOBAL_MODULE)
                .resolvePackagePath(model.packageName)
                .resolve(model.interfaceName + ".java").let {
            ensureFileAbsence(it, "Service interface \"${model.packageName}.${model.interfaceName}\" already exists")
        }
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(CubaPlugin.TEMPLATES_BASE_PATH + "service", bindings, projectModel.platformVersion) {
            transformWhole()
        }

        val springXml = projectStructure.getModule(WEB_MODULE).springXml
        updateXml(springXml) {
            val proxyCreator = xpath("//bean[@class='com.haulmont.cuba.web.sys.remoting.WebRemoteProxyBeanCreator']").firstOrNull() as Element?
                    ?: appendChild("bean") {
                        this["class"] = "com.haulmont.cuba.web.sys.remoting.WebRemoteProxyBeanCreator"
                        appendChild("property") {
                            this["name"] = "serverSelector"
                            this["ref"] = "cuba_ServerSelector"
                        }
                        appendChild("property") {
                            this["name"] = "remoteServices"
                            appendChild("map")
                        }
                    }

            val servicesMap = proxyCreator.xpath("//property[@name='remoteServices']/map").first() as Element
            servicesMap.appendChild("entry") {
                this["key"] = model.serviceName
                this["value"] = "${model.packageName}.${model.interfaceName}"
            }
        }
    }
}

