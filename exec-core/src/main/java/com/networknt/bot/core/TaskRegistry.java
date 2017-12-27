package com.networknt.bot.core;

import com.networknt.service.SingletonServiceFactory;

import java.util.*;

public class TaskRegistry {
    private final Map<String, Command> tasks;

    private static final TaskRegistry INSTANCE = new TaskRegistry();

    private TaskRegistry() {
        Command[] commands = SingletonServiceFactory.getBeans(Command.class);
        final Map<String, Command> map = new HashMap<>();
        if(commands != null) Arrays.stream(commands).forEach(s -> map.put(s.getName(), s));
        this.tasks = Collections.unmodifiableMap(map);
    }

    public Set<String> getTasks() {
        return tasks.keySet();
    }

    public Command getCommand(String name) {
        return tasks.get(name);
    }

    public static TaskRegistry getInstance() {
        return INSTANCE;
    }
}
