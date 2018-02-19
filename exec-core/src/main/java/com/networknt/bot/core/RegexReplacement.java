package com.networknt.bot.core;

import java.util.regex.Pattern;

public class RegexReplacement {
    private final Pattern regex;
    private final String original;
    private final String replacement;

    public RegexReplacement(String regex, String original, String replacement) {
        this.regex = Pattern.compile(regex);
        this.original = original;
        this.replacement = replacement;
    }

    public boolean match(String in) {
        return regex.matcher(in).find();
    }

    public String replace(String in) {
        boolean matches = regex.matcher(in).find();
        if(matches) {
            return in.replaceAll(original, replacement);
        } else {
            return in;
        }
    }
}
