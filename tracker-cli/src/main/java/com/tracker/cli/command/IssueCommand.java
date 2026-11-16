package com.tracker.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "issue",
        description = "Issue commands",
        mixinStandardHelpOptions = true,
        subcommands = {
                IssueCommand.Create.class,
                IssueCommand.List.class,
                IssueCommand.Move.class,
                IssueCommand.Assign.class,
                IssueCommand.History.class
        })
public class IssueCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "create", description = "Create an issue", mixinStandardHelpOptions = true)
    static class Create extends StubCommand {
    }

    @Command(name = "list", description = "List and filter issues", mixinStandardHelpOptions = true)
    static class List extends StubCommand {
    }

    @Command(name = "move", description = "Transition an issue to a new status", mixinStandardHelpOptions = true)
    static class Move extends StubCommand {
    }

    @Command(name = "assign", description = "Assign an issue to a user", mixinStandardHelpOptions = true)
    static class Assign extends StubCommand {
    }

    @Command(name = "history", description = "Show the audit history of an issue", mixinStandardHelpOptions = true)
    static class History extends StubCommand {
    }
}
