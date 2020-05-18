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

import com.haulmont.cli.core.MainCliPlugin;
import com.haulmont.cuba.cli.JansiSupportWorkAround;
import com.haulmont.cuba.cli.cubaplugin.CubaPlugin;
import org.jline.terminal.spi.JansiSupport;

module com.haulmont.cuba.cli {
    requires kotlin.stdlib.jdk8;
    requires kotlin.stdlib.jdk7;
    requires kotlin.stdlib;
    requires kotlin.reflect;

    requires jcommander;
    requires jansi;
    requires jline;

    requires velocity;

    requires com.google.common;
    requires gson;

    requires commons.configuration;
    requires commons.lang;

    requires jdk.zipfs;
    requires kodein.di.core.jvm;
    requires kodein.di.generic.jvm;

    requires practicalxml;
    requires java.xml;

    provides MainCliPlugin with CubaPlugin;

    uses com.haulmont.cli.core.CliPlugin;

    opens com.haulmont.cuba.cli.cubaplugin;
    opens com.haulmont.cuba.cli.commands;
    opens com.haulmont.cuba.cli;

    opens com.haulmont.cuba.cli.cubaplugin.model;

    opens com.haulmont.cuba.cli.cubaplugin.appcomponentxml;
    opens com.haulmont.cuba.cli.cubaplugin.componentbean;
    opens com.haulmont.cuba.cli.cubaplugin.entity;
    opens com.haulmont.cuba.cli.cubaplugin.entitylistener;
    opens com.haulmont.cuba.cli.cubaplugin.enumeration;
    opens com.haulmont.cuba.cli.cubaplugin.project;
    opens com.haulmont.cuba.cli.cubaplugin.screen;
    opens com.haulmont.cuba.cli.cubaplugin.screen.blankscreen;
    opens com.haulmont.cuba.cli.cubaplugin.screen.screenextension;
    opens com.haulmont.cuba.cli.cubaplugin.screen.entityscreen;
    opens com.haulmont.cuba.cli.cubaplugin.service;
    opens com.haulmont.cuba.cli.cubaplugin.statictemplate;
    opens com.haulmont.cuba.cli.cubaplugin.theme;
    opens com.haulmont.cuba.cli.cubaplugin.installcomponent;
    opens com.haulmont.cuba.cli.cubaplugin.front;
    opens com.haulmont.cuba.cli.cubaplugin.front.polymer;
    opens com.haulmont.cuba.cli.cubaplugin.front.react;
    opens com.haulmont.cuba.cli.cubaplugin.config;
    opens com.haulmont.cuba.cli.cubaplugin.updatescript;
    opens com.haulmont.cuba.cli.cubaplugin.premiumrepo;
    opens com.haulmont.cuba.cli.cubaplugin.prefixchange;
    opens com.haulmont.cuba.cli.cubaplugin.deploy;
    opens com.haulmont.cuba.cli.cubaplugin.deploy.war;
    opens com.haulmont.cuba.cli.cubaplugin.deploy.uberjar;
    opens com.haulmont.cuba.cli.cubaplugin.gradle;
    opens com.haulmont.cuba.cli.cubaplugin.idea;

    exports com.haulmont.cuba.cli;
    exports com.haulmont.cuba.cli.event;
    exports com.haulmont.cuba.cli.commands;
    exports com.haulmont.cuba.cli.generation;
    exports com.haulmont.cuba.cli.registration;
    exports com.haulmont.cuba.cli.cubaplugin.model;

//    for debug
    requires jdk.jdwp.agent;

//    may be needed by plugins
    requires java.sql;

//    requires by sdk plugin
    requires java.desktop;
    requires java.management;
    requires java.naming;
    requires com.haulmont.cli.core;

//    jansi support workaround
    provides JansiSupport with JansiSupportWorkAround;
}