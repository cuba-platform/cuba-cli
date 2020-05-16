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

import com.haulmont.cuba.cli.core.prompting.Option
import com.haulmont.cuba.cli.core.prompting.ValidationException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class PlainQuestionsTest : QuestionsTestBase() {

    @Test(expected = ValidationException::class)
    fun testPlainValidation() {
        appendInputLine("Not alex")
        appendInputLine("Alex")

        val answers = interactivePrompts {
            question("name", "What is your name") {
                validate {
                    value == "Alex" || fail("Not alex")
                }
            }
        }
    }

    @Test
    fun testPlainQuestions() {
        appendInputLine("Alex")
        appendInputLine("Russia")

        val answers = interactivePrompts {
            question("name", "Enter name")
            question("country", "Enter country")
        }

        assertTrue(answers["name"] == "Alex")
        assertTrue(answers["country"] == "Russia")
    }

    @Test
    fun testDefaultStringQuestion() {
        appendEmptyLine()
        appendEmptyLine()

        val answers = interactivePrompts {
            question("name", "Enter name") {
                default("Alex")
            }

            question("nameTwice", "") {
                default {
                    val name by it
                    "$name$name"
                }
            }
        }

        assertTrue(answers["name"] == "Alex")
        assertTrue(answers["nameTwice"] == "AlexAlex")
    }

    @Test
    fun testConfirmationQuestion() {
        appendInputLine("y")
        appendInputLine("Alex")

        val ask = {
            interactivePrompts {
                confirmation("askName", "Ask name?")

                question("name", "Enter name") {
                    askIf("askName")
                }
            }
        }

        var answers = ask()

        assertTrue(answers["askName"] as Boolean)
        assertTrue(answers["name"] == "Alex")

        reset()

        appendInputLine("n")
        appendInputLine("Alex")

        answers = ask()

        assertFalse(answers["askName"] as Boolean)
        assertTrue("name" !in answers)
    }

    @Test
    fun testStringOptions() {
        val ask = {
            interactivePrompts {
                textOptions("opts", "", listOf("a", "b", "c"))
            }
        }

        appendInputLine("1")
        assertTrue(ask()["opts"] == "a")

        reset()

        appendInputLine("2")
        assertTrue(ask()["opts"] == "b")

        reset()

        appendInputLine("3")
        assertTrue(ask()["opts"] == "c")
    }

    @Test
    fun testDefaultOption() {
        appendEmptyLine()

        val answers = interactivePrompts {
            textOptions("opts", "", listOf("a", "b", "c")) {
                default(1)
            }
        }

        assertTrue(answers["opts"] == "b")
    }

    @Test
    fun testItemOptions() {
        class Item(val id: String)

        appendInputLine("2")

        val answers = interactivePrompts {
            options("opts", "", listOf(
                    Option("a", null, Item("A")),
                    Option("b", null, Item("B")),
                    Option("c", null, Item("C"))
            ))
        }

        val opts by answers

        assertTrue(opts is Item)
        assertTrue((opts as Item).id == "B")
    }
}