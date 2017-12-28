package com.networknt.bot.core;

public class GitUtil {
    public static boolean branchChanged(String branch, String message) {
        return message.indexOf(branch + "    -> origin/" + branch) > 0 ? true : false;
    }
}
