package com.tracker.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "sprint",
        description = "Sprint commands",
        mixinStandardHelpOptions = true,
        subcommands = {
                SprintCommand.Create.class,
                SprintCommand.Start.class,
                SprintCommand.Close.class,
                SprintCommand.Active.class
        })
public class SprintCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "create", description = "Create a planned sprint", mixinStandardHelpOptions = true)
    static class Create extends StubCommand {
    }

    @Command(name = "start", description = "Start a planned sprint", mixinStandardHelpOptions = true)
    static class Start extends StubCommand {
    }

    @Command(name = "close", description = "Close an active sprint and roll over unfinished issues", mixinStandardHelpOptions = true)
    static class Close extends StubCommand {
    }

    @Command(name = "active", description = "Show the active sprint and its issues", mixinStandardHelpOptions = true)
    static class Active extends StubCommand {
    }
}
