package com.networknt.bot.regex.replace;

import org.junit.jupiter.api.Assertions;

import java.io.IOException;

public class RegexReplaceTaskTest {
    //@Test
    public void testRegexReplace() throws IOException, InterruptedException {
        RegexReplaceTask cmd = new RegexReplaceTask();
        int result = cmd.execute();
        Assertions.assertEquals(0, result);
    }
}
