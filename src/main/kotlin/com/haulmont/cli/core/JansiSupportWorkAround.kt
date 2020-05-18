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

package com.haulmont.cli.core

import org.jline.terminal.Attributes
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.impl.jansi.freebsd.FreeBsdNativePty
import org.jline.terminal.impl.jansi.linux.LinuxNativePty
import org.jline.terminal.impl.jansi.osx.OsXNativePty
import org.jline.terminal.impl.jansi.win.JansiWinSysTerminal
import org.jline.terminal.spi.JansiSupport
import org.jline.terminal.spi.Pty
import java.io.IOException
import java.nio.charset.Charset

class JansiSupportWorkAround : JansiSupport {

    @Throws(IOException::class)
    override fun current(): Pty {
        val osName = System.getProperty("os.name")
        if (osName.startsWith("Linux")) {
            if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
                return LinuxNativePty.current()
            }
        } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
            if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 12) {
                return OsXNativePty.current()
            }
        } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
            // Solaris is not supported by jansi
            // return SolarisNativePty.current();
        } else if (osName.startsWith("FreeBSD")) {
            if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
                return FreeBsdNativePty.current()
            }
        }
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun open(attributes: Attributes, size: Size): Pty {
        if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 16) {
            val osName = System.getProperty("os.name")
            if (osName.startsWith("Linux")) {
                return LinuxNativePty.open(attributes, size)
            } else if (osName.startsWith("Mac") || osName.startsWith("Darwin")) {
                return OsXNativePty.open(attributes, size)
            } else if (osName.startsWith("Solaris") || osName.startsWith("SunOS")) {
                // Solaris is not supported by jansi
                // return SolarisNativePty.current();
            } else if (osName.startsWith("FreeBSD")) {
                return FreeBsdNativePty.open(attributes, size)
            }
        }
        throw UnsupportedOperationException()
    }

    @Throws(IOException::class)
    override fun winSysTerminal(name: String?, type: String?, ansiPassThrough: Boolean, encoding: Charset?, codepage: Int, nativeSignals: Boolean, signalHandler: Terminal.SignalHandler?): Terminal {
        if (JANSI_MAJOR_VERSION > 1 || JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION >= 12) {
            val terminal = JansiWinSysTerminal(name, type, ansiPassThrough, encoding, codepage, nativeSignals, signalHandler)
            if (JANSI_MAJOR_VERSION == 1 && JANSI_MINOR_VERSION < 16) {
                terminal.disableScrolling()
            }
            return terminal
        }
        throw UnsupportedOperationException()
    }

    companion object {
        internal val JANSI_MAJOR_VERSION: Int = 1
        internal val JANSI_MINOR_VERSION: Int = 17
    }

}
