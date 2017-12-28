package com.networknt.bot.core;

import org.junit.Assert;
import org.junit.Test;

public class RegexReplacementTest {
    @Test
    public void testReplacement() {
        RegexReplacement rr = new RegexReplacement("<version.light[a-z-]+4j>\\d*\\.\\d*\\.\\d*</version.light[a-z-]+4j>", "1.5.5", "1.5.6");
        String result = rr.replace("<version.light-4j>1.5.5</version.light-4j>");
        System.out.println(result);
        Assert.assertEquals("<version.light-4j>1.5.6</version.light-4j>", result);

        result = rr.replace("abc");
        System.out.println(result);
        Assert.assertEquals("abc", result);

        result = rr.replace("abc<version.light-rest-4j>1.5.5</version.light-rest-4j>def");
        System.out.println(result);
        Assert.assertEquals("abc<version.light-rest-4j>1.5.6</version.light-rest-4j>def", result);
    }

}
