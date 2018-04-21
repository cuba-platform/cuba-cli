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

import com.haulmont.cuba.cli.CliPlugin;
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin;

module cuba.cli.main {
    requires java.base;
    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires jcommander;
    requires jansi;
    requires jline;

    requires velocity;

    requires guava;

    requires slf4j.simple;
    requires slf4j.api;

    requires jdk.zipfs;
    requires kodein.di.core.jvm;
    requires kodein.di.generic.jvm;

    requires practicalxml;
    requires java.xml;

    provides CliPlugin with CubaPlugin;

    uses com.haulmont.cuba.cli.CliPlugin;

    opens com.haulmont.cuba.cli.cubaplugin;
    opens com.haulmont.cuba.cli.commands;

    exports com.haulmont.cuba.cli.model;
}