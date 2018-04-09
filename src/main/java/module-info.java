import com.haulmont.cuba.cli.CliPlugin;
import com.haulmont.cuba.cli.ProjectScanPlugin;

module cuba.cli.main {
    requires kotlin.stdlib;
    requires jcommander;
    requires velocity;
    requires java.base;
    requires jdk.zipfs;
    requires jansi;
    requires guava;
    requires slf4j.simple;
    requires slf4j.api;


    provides CliPlugin with ProjectScanPlugin;

    uses com.haulmont.cuba.cli.CliPlugin;

    opens com.haulmont.cuba.cli;
    opens com.haulmont.cuba.cli.commands;
}