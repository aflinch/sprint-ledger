package com.tracker.cli.command;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "auth",
        description = "Authentication commands",
        mixinStandardHelpOptions = true,
        subcommands = {AuthCommand.Login.class})
public class AuthCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    @Command(name = "login", description = "Authenticate and cache a JWT", mixinStandardHelpOptions = true)
    static class Login extends StubCommand {
    }
}
