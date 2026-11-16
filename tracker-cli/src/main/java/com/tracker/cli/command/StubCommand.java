package com.tracker.cli.command;

import java.util.concurrent.Callable;

import picocli.CommandLine.Command;

abstract class StubCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        Command command = getClass().getAnnotation(Command.class);
        String name = command != null ? command.name() : getClass().getSimpleName();
        System.err.printf("'%s' is not implemented yet.%n", name);
        return 2;
    }
}
