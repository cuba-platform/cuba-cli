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

fun String.red(): String = "@|red $this|@"

fun String.white(): String = "@|white $this|@"

fun String.green(): String = "@|green $this|@"

fun String.blue(): String = "@|blue $this|@"

fun String.bgRed(): String = "@|bg_red $this|@"

fun String.bgWhite(): String = "@|bg_white $this|@"

fun String.bgGreen(): String = "@|bg_green $this|@"

fun String.bgBlue(): String = "@|bg_blue $this|@"

fun String.attention() = bgRed()