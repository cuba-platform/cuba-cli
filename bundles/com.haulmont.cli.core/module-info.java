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

module com.haulmont.cli.core {
    requires kotlin.stdlib.jdk8;
    requires kotlin.stdlib.jdk7;
    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires jcommander;
    requires jansi;
    requires jline;

    requires com.google.common;
    requires gson;

    requires commons.configuration;
    requires commons.lang;

    requires jdk.zipfs;
    requires kodein.di.core.jvm;
    requires kodein.di.generic.jvm;

    requires java.xml;

    uses com.haulmont.cli.core.CliPlugin;
    uses com.haulmont.cli.core.MainCliPlugin;

    opens com.haulmont.cli.core.commands;
    opens com.haulmont.cli.core;

    exports com.haulmont.cli.core;
    exports com.haulmont.cli.core.event;
    exports com.haulmont.cli.core.commands;
    exports com.haulmont.cli.core.prompting;

//    for debug
    requires jdk.jdwp.agent;

//    may be needed by plugins
    requires java.sql;

//    requires by sdk plugin
    requires java.desktop;
    requires java.management;
    requires java.naming;
}