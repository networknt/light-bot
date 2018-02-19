package com.networknt.bot.regex.replace;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class RegexReplaceTaskTest {
    @Test
    public void testRegexReplace() throws IOException, InterruptedException {
        RegexReplaceTask cmd = new RegexReplaceTask();
        int result = cmd.execute();
        Assert.assertEquals(0, result);
    }
}
