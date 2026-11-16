package com.tracker.cli;

import com.tracker.cli.command.AuthCommand;
import com.tracker.cli.command.IssueCommand;
import com.tracker.cli.command.ProjectCommand;
import com.tracker.cli.command.SprintCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "tracker",
        mixinStandardHelpOptions = true,
        version = "tracker 0.0.1-SNAPSHOT",
        description = "Terminal client for the sprint tracker.",
        subcommands = {AuthCommand.class, ProjectCommand.class, SprintCommand.class, IssueCommand.class})
public class TrackerCliApplication implements Runnable {

    @Option(
            names = "--server-url",
            defaultValue = "http://localhost:8080",
            description = "Base URL of the tracker API (default: ${DEFAULT-VALUE})")
    private String serverUrl;

    public String getServerUrl() {
        return serverUrl;
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new TrackerCliApplication()).execute(args));
    }
}
