package com.networknt.bot.cli;

import org.junit.jupiter.api.Test;
import com.networknt.bot.core.Command;
import com.networknt.bot.core.Constants;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class CliTest {
    private static class RecordingCli extends Cli {
        int notifications;
        boolean notificationFails;

        @Override
        void notifyFailure() {
            notifications++;
            if (notificationFails) throw new IllegalStateException("Notification failed");
        }
    }

    private Command command(int result, Exception failure) {
        return new Command() {
            @Override
            public int execute() throws IOException, InterruptedException {
                if (failure instanceof IOException) throw (IOException)failure;
                if (failure instanceof InterruptedException) throw (InterruptedException)failure;
                if (failure != null) throw (RuntimeException)failure;
                return result;
            }

            @Override
            public String getName() { return "test"; }
        };
    }

    @Test
    void exceptionsFailAndUseFailureNotification() {
        RecordingCli cli = new RecordingCli();
        assertEquals(1, cli.runCommand(command(0, new IOException("missing prev_tag"))));
        assertEquals(1, cli.notifications);
    }

    @Test
    void nonzeroCommandStatusIsPreserved() {
        RecordingCli cli = new RecordingCli();
        assertEquals(22, cli.runCommand(command(22, null)));
        assertEquals(1, cli.notifications);
    }

    @Test
    void successAndNoChangesDoNotNotify() {
        RecordingCli cli = new RecordingCli();
        assertEquals(0, cli.runCommand(command(0, null)));
        assertEquals(0, cli.runCommand(command(Constants.NO_REPO_CHANGE, null)));
        assertEquals(0, cli.notifications);
    }

    @Test
    void notificationErrorDoesNotTurnFailureIntoSuccess() {
        RecordingCli cli = new RecordingCli();
        cli.notificationFails = true;
        assertEquals(1, cli.runCommand(command(0, new IllegalStateException("task failed"))));
        assertEquals(1, cli.notifications);
    }

    @Test
    void interruptionRemainsVisibleToCaller() {
        RecordingCli cli = new RecordingCli();
        try {
            assertEquals(1, cli.runCommand(command(0, new InterruptedException("stop"))));
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(1, cli.notifications);
        } finally {
            Thread.interrupted();
        }
    }
}
