package com.tracker.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "project",
        description = "Project commands",
        mixinStandardHelpOptions = true,
        subcommands = {ProjectCommand.Create.class, ProjectCommand.List.class})
public class ProjectCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "create", description = "Create a new project", mixinStandardHelpOptions = true)
    static class Create extends StubCommand {
    }

    @Command(name = "list", description = "List projects", mixinStandardHelpOptions = true)
    static class List extends StubCommand {
    }
}
