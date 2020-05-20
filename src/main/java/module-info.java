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

import com.haulmont.cli.core.CliPlugin;
import com.haulmont.cli.core.MainCliPlugin;
import com.haulmont.cli.sample.JansiSupportWorkAround;
import com.haulmont.cli.sample.SamplePlugin;
import org.jline.terminal.spi.JansiSupport;

module com.haulmont.cli.sample {
    requires kotlin.stdlib.jdk8;
    requires kotlin.stdlib.jdk7;
    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires com.haulmont.cli.core;

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

    provides MainCliPlugin with SamplePlugin;

    uses com.haulmont.cli.sample.SamplePlugin;

    opens com.haulmont.cli.sample.commands;

    exports com.haulmont.cli.sample;
    exports com.haulmont.cli.sample.commands;

//    for debug
    requires jdk.jdwp.agent;

//    may be needed by plugins
    requires java.sql;

//    requires by sdk plugin
    requires java.desktop;
    requires java.management;
    requires java.naming;

    provides JansiSupport with JansiSupportWorkAround;
}