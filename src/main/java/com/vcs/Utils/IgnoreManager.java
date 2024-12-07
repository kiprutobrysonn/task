package com.vcs.Utils;

import java.nio.file.Path;
import java.util.Set;

public class IgnoreManager {
    private Set<String> ignoredPatterns;

    public void addIgnorePattern(String pattern) {
    }

    public boolean isIgnored(Path file) {
        return false;

    }

    public void loadIgnoreFile() {

    } // Read .vcsignore
}