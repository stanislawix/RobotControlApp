package com.sj.manipulatorcontrol;

public class CommandFilter {
    private final int INTERVAL_BETWEEN_COMMANDS;
    private String recentCommand = "";
    private long lastCommandSentTime = 0;
    private long timeSinceLastSentCommand = 0;

    public CommandFilter() {
        this(200);
    }

    public CommandFilter(int interval_between_commands) {
        this.INTERVAL_BETWEEN_COMMANDS = interval_between_commands;
    }

    boolean isRecommendedToSendCommand(String command) {
        timeSinceLastSentCommand = System.currentTimeMillis() - lastCommandSentTime;
        recentCommand = command;
        if(timeSinceLastSentCommand > INTERVAL_BETWEEN_COMMANDS && !recentCommand.equals("{\"spd\":0,\"dir\":0}")) {
            lastCommandSentTime = (int) System.currentTimeMillis();
            return true;
        } else {
            return false;
        }
    }

    String sendCommand() {
        lastCommandSentTime = System.currentTimeMillis();
        return recentCommand;
    }
}
