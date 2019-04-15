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

package com.haulmont.cuba.cli.prompting

import org.junit.Assert.assertTrue
import org.junit.Test

class BigQuestionsTest : QuestionsTestBase() {
    @Test
    fun test() {

        appendInputLine("Alex") //name
        appendInputLine("Samara") //city
        appendInputLine("Russia") //country
        appendInputLine("y")
        appendInputLine("Gastello") //street
        appendInputLine("43") //houseNumber
        appendInputLine("y")
        appendInputLine("Moskovskoe shosse") //street
        appendInputLine("45") //houseNumber
        appendInputLine("n")

        appendInputLine("24r")
        appendInputLine("24")

        val answers = interactivePrompts {
            question("name", "Enter name")

            questionList("location") {
                question("city", "Enter city")
                question("country", "Enter country")

                repeating("addresses", "Add address?") {
                    question("street", "Enter street")
                    question("houseNumber", "Enter house number")
                }
            }

            question("age", "How old are you?") {
                validate {
                    try {
                        value.toInt()
                    } catch (e: NumberFormatException) {
                        fail("Enter valid age")
                    }
                }
            }
        }

        val location: Answers by answers

        val addresses: List<Answers> by location

        assertTrue(answers["name"] == "Alex")
        assertTrue(location["city"] == "Samara")
        assertTrue(location["country"] == "Russia")
        assertTrue(answers["age"] == "24")

        assertTrue(addresses[0]["street"] == "Gastello")
        assertTrue(addresses[0]["houseNumber"] == "43")

        assertTrue(addresses[1]["street"] == "Moskovskoe shosse")
        assertTrue(addresses[1]["houseNumber"] == "45")
    }

    override fun throwValidation(): Boolean = false
}