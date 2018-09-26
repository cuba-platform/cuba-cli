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

package com.haulmont.cuba.cli.generation.properties

import com.google.common.base.Joiner
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.lang.ArrayUtils
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.UnhandledException
import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.util.*

class MessagesWriter(writer: Writer, private val delimiter: Char) : PropertiesConfiguration.PropertiesWriter(writer, delimiter) {

    init {
        lineSeparator = "\n"
    }

    @Throws(IOException::class)
    override fun writeProperty(key: String, value: Any, forceSingleLine: Boolean) {
        write(escapeKey(key))
        write(fetchSeparator(key, value))
        write(escapeValue(value)!!)

        writeln(null)
    }

    /**
     * Escape the separators in the [key].
     *
     * @param key the key
     * @return the escaped key
     */
    private fun escapeKey(key: String): String {
        val newkey = StringBuilder()

        for (i in 0 until key.length) {
            val c = key[i]

            if (ArrayUtils.contains(SEPARATORS, c) || ArrayUtils.contains(WHITE_SPACE, c)) {
                // escape the separator
                newkey.append('\\')
                newkey.append(c)
            } else {
                newkey.append(c)
            }
        }

        return newkey.toString()
    }

    /**
     * Escapes the given property value. Delimiter characters in the value
     * will be escaped.
     *
     * @param value the property value
     * @return the escaped property value
     */
    private fun escapeValue(value: Any): String? {
        var value = value
        if (value is Collection<*>) {
            value = Joiner.on(", ").skipNulls().join(value)
        }
        var escapedValue = escapeJavaStyleString(value.toString(), false, false)
        if (delimiter.toInt() != 0) {
            escapedValue = StringUtils.replace(escapedValue, delimiter.toString(), ESCAPE + delimiter)
        }
        return escapedValue
    }

    companion object {

        /**
         * Constant for the escaping character.
         */
        private val ESCAPE = "\\"

        /**
         * The list of possible key/value separators
         */
        private val SEPARATORS = charArrayOf('=', ':')

        /**
         * The white space characters used as key/value separators.
         */
//        \u000C is for \f as \f is not supported by Kotlin
        private val WHITE_SPACE = charArrayOf(' ', '\t', '\u000C')

        /**
         * @param str                String to escape values in, may be null
         * @param escapeSingleQuotes escapes single quotes if `true`
         * @param escapeForwardSlash
         * @return the escaped string
         */
        private fun escapeJavaStyleString(str: String?, escapeSingleQuotes: Boolean, escapeForwardSlash: Boolean): String? {
            if (str == null) {
                return null
            }
            try {
                val writer = StringWriter(str.length * 2)
                escapeJavaStyleString(writer, str, escapeSingleQuotes, escapeForwardSlash)
                return writer.toString()
            } catch (ioe: IOException) {
                // this should never ever happen while writing to a StringWriter
                throw UnhandledException(ioe)
            }

        }

        @Throws(IOException::class)
        private fun escapeJavaStyleString(out: Writer?, str: String?, escapeSingleQuote: Boolean,
                                          escapeForwardSlash: Boolean) {
            if (out == null) {
                throw IllegalArgumentException("The Writer must not be null")
            }
            if (str == null) {
                return
            }
            val sz: Int = str.length
            var startSpace = true
            for (i in 0 until sz) {
                val ch = str[i]

                if (startSpace) {
                    if (ch.toInt() == 32) {
                        out.write("\\u0020")
                        continue
                    }
                    startSpace = false
                }
                // handle unicode
                /*if (ch > 0xfff) {
                   out.write("\\u" + hex(ch));
               } else if (ch > 0xff) {
                   out.write("\\u0" + hex(ch));
               } else if (ch > 0x7f) {
                   out.write("\\u00" + hex(ch));
               } else*/ if (ch.toInt() < 32) {
                    when (ch) {
                        '\b' -> {
                            out.write('\\')
                            out.write('b')
                        }
                        '\n' -> {
                            out.write('\\')
                            out.write('n')
                        }
                        '\t' -> {
                            out.write('\\')
                            out.write('t')
                        }
                        '\u000C' -> {
                            out.write('\\')
                            out.write('f')
                        }
                        '\r' -> {
                            out.write('\\')
                            out.write('r')
                        }
                        else -> if (ch.toInt() > 0xf) {
                            out.write("\\u00" + hex(ch))
                        } else {
                            out.write("\\u000" + hex(ch))
                        }
                    }
                } else {
                    when (ch) {
                        '\'' -> {
                            if (escapeSingleQuote) {
                                out.write('\\')
                            }
                            out.write('\'')
                        }
                        '"' -> {
                            out.write('\\')
                            out.write('"')
                        }
                        '\\' -> {
                            out.write('\\')
                            out.write('\\')
                        }
                        '/' -> {
                            if (escapeForwardSlash) {
                                out.write('\\')
                            }
                            out.write('/')
                        }
                        else -> out.write(ch.toInt())
                    }
                }
            }
        }

        /**
         *
         * Returns an upper case hexadecimal `String` for the given
         * character.
         *
         * @param ch The character to convert.
         * @return An upper case hexadecimal `String`
         */
        private fun hex(ch: Char): String {
            return Integer.toHexString(ch.toInt()).toUpperCase(Locale.ENGLISH)
        }


        private fun Writer.write(ch: Char) {
            this.write(ch.toInt())
        }
    }

}