package com.vcs.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreManager {
    private Set<PathMatcher> ignoredPatterns;
    private Path rootPath = new File(".").toPath();

    public IgnoreManager() {
        this.ignoredPatterns = new HashSet<>();
        loadIgnoreFile();
    }

    /**
     * Adds a single ignore pattern to the set of ignored patterns.
     * Converts the pattern to a PathMatcher for efficient matching.
     * 
     * @param pattern The pattern to ignore (e.g., "*.log", "target/", ".git")
     */
    public void addIgnorePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return;
        }

        // Trim and ignore comments and empty lines
        pattern = pattern.trim();
        if (pattern.startsWith("#") || pattern.isEmpty()) {
            return;
        }

        // Convert glob pattern to PathMatcher
        try {
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
            ignoredPatterns.add(matcher);
        } catch (Exception e) {
            // Log or handle pattern conversion errors
            System.err.println("Invalid ignore pattern: " + pattern);
        }
    }

    /**
     * Checks if a given file should be ignored based on the loaded patterns.
     * 
     * @param file Path to check against ignore patterns
     * @return true if the file should be ignored, false otherwise
     */
    public boolean isIgnored(Path file) {
        // Ensure we have a relative path from the root
        Path relativePath = rootPath.relativize(file);

        // Check against all patterns
        return ignoredPatterns.stream()
                .anyMatch(matcher -> matcher.matches(relativePath));
    }

    /**
     * Loads ignore patterns from the .vcsignore file.
     * Reads from the .vcs directory relative to the root path.
     */
    public void loadIgnoreFile() {
        try {
            File ignoreFile = new File(".vcsignore");

            // If ignore file doesn't exist, just return
            if (!ignoreFile.exists()) {
                return;
            }

            // Read patterns from the file
            try (BufferedReader reader = new BufferedReader(new FileReader(ignoreFile))) {
                reader.lines()
                        .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                        .forEach(this::addIgnorePattern);
            }
        } catch (IOException e) {
            // Log the error, but don't throw to allow continuing without ignore file
            System.err.println("Error reading .vcsignore file: " + e.getMessage());
        }
    }

    /**
     * Get the current set of ignore patterns as strings.
     * 
     * @return Set of ignore pattern strings
     */
    public Set<String> getIgnorePatterns() {
        return ignoredPatterns.stream()
                .map(PathMatcher::toString)
                .collect(Collectors.toSet());
    }

    /**
     * Clear all existing ignore patterns.
     */
    public void clearIgnorePatterns() {
        ignoredPatterns.clear();
    }
}