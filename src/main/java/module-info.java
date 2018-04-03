import com.haulmont.cuba.cli.commands.ProjectInitCommand;

module cuba.cli.main {
    requires kotlin.stdlib;
    requires jcommander;
    requires velocity;
    requires java.base;
    requires jdk.zipfs;

    provides com.haulmont.cuba.cli.CliCommand with ProjectInitCommand;

    uses com.haulmont.cuba.cli.CliCommand;

    opens com.haulmont.cuba.cli;

    opens com.haulmont.cuba.cli.commands to jcommander;
}