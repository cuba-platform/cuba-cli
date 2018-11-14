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

package com.haulmont.cuba.cli.registration

import com.haulmont.cuba.cli.cubaplugin.model.ModuleStructure
import com.haulmont.cuba.cli.cubaplugin.model.ProjectStructure
import com.haulmont.cuba.cli.generation.appendChild
import com.haulmont.cuba.cli.generation.set
import com.haulmont.cuba.cli.generation.updateXml
import com.haulmont.cuba.cli.generation.xpath
import org.w3c.dom.Element

class ServiceRegistrationHelper {
    fun registerService(serviceName: String, packageName: String, interfaceName: String) {
        val springXml = ProjectStructure().getModule(ModuleStructure.WEB_MODULE).springXml
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
                this["key"] = serviceName
                this["value"] = "$packageName.$interfaceName"
            }
        }
    }
}