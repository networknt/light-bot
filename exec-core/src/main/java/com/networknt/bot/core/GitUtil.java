package com.networknt.bot.core;

public class GitUtil {
    public static boolean developBranchChanged(String message) {
        return message.indexOf("develop    -> origin/develop") > 0 ? true : false;
    }
}
