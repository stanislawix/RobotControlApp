package com.sj.manipulatorcontrol;

public class CommandFilter {
    public final long INTERVAL_BETWEEN_COMMANDS_MS;
    public final Komenda zeroCommand = new Komenda();

    private Komenda recentCommand = new Komenda();
    private long lastCommandSentTime = 0;
    private long timeSinceLastSentCommand = 0;
    private long lastZeroCommandSentTime = 0;
    private long timeSinceLastSentZeroCommand = 0;

    public CommandFilter() {
        this(100L);
    }

    public CommandFilter(long interval_between_commands) {
        this.INTERVAL_BETWEEN_COMMANDS_MS = interval_between_commands;
    }

    boolean isRecommendedToSendCommand(Komenda command) {
        timeSinceLastSentCommand = System.currentTimeMillis() - lastCommandSentTime;
        timeSinceLastSentZeroCommand = System.currentTimeMillis() - lastZeroCommandSentTime;
        recentCommand = command;
        if(timeSinceLastSentCommand > INTERVAL_BETWEEN_COMMANDS_MS && !recentCommand.equals(zeroCommand)) {
            return true;
        } else if(timeSinceLastSentZeroCommand > INTERVAL_BETWEEN_COMMANDS_MS && recentCommand.equals(zeroCommand)) {
            lastZeroCommandSentTime = System.currentTimeMillis();
            System.out.println("Zero command sent");
            return true;
        } else
            return false;
    }

    String sendCommand() {
        lastCommandSentTime = System.currentTimeMillis();
        return recentCommand.toString();
    }
}
