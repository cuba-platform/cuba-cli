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
    requires kotlinx.coroutines.core;


    provides CliPlugin with CubaPlugin;

    uses com.haulmont.cuba.cli.CliPlugin;

    opens com.haulmont.cuba.cli.cubaplugin;
    opens com.haulmont.cuba.cli.commands;

    exports com.haulmont.cuba.cli.model;
}