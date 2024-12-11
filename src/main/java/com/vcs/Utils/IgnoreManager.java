package com.vcs.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class IgnoreManager {
    private Set<PathMatcher> ignoredPatterns;
    private Path rootPath;

    /**
     * Constructs an IgnoreManager that dynamically uses the current working
     * directory
     */
    public IgnoreManager() {
        // Dynamically get the current working directory at runtime
        this.rootPath = getCurrentWorkingDirectory();
        this.ignoredPatterns = new HashSet<>();
        loadIgnoreFile();
    }

    /**
     * Gets the current working directory dynamically
     * 
     * @return Path of the current working directory
     */
    private Path getCurrentWorkingDirectory() {
        try {
            // Use System property to get current working directory
            String userDir = System.getProperty("user.dir");
            return Paths.get(userDir).toAbsolutePath().normalize();
        } catch (Exception e) {
            // Fallback to default directory if unable to get user directory
            System.err.println("Could not determine current working directory. Falling back to default.");
            return Paths.get(".").toAbsolutePath().normalize();
        }
    }

    /**
     * Reloads ignore patterns using the current working directory
     */
    public void refreshIgnorePatterns() {
        // Update root path to current working directory
        this.rootPath = getCurrentWorkingDirectory();

        // Clear existing patterns
        this.ignoredPatterns.clear();

        // Reload ignore file
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
        // Ensure we have an absolute, normalized path
        file = file.toAbsolutePath().normalize();

        // Refresh the root path to ensure it's current
        rootPath = getCurrentWorkingDirectory();

        // Check if the file is within the root path
        if (!file.startsWith(rootPath)) {
            return false;
        }

        // Create relative path from root
        Path relativePath = rootPath.relativize(file);

        // Check against all patterns
        return ignoredPatterns.stream()
                .anyMatch(matcher -> matcher.matches(relativePath));
    }

    /**
     * Loads ignore patterns from the .vcsignore file in the current working
     * directory.
     */
    public void loadIgnoreFile() {
        File ignoreFile = new File(rootPath.toFile(), ".vcsignore");

        // If ignore file doesn't exist, just return
        if (!ignoreFile.exists()) {
            System.out.println("No .vcsignore file found in " + rootPath);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(ignoreFile))) {
            reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("#"))
                    .forEach(this::addIgnorePattern);
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

    /**
     * Get the current root path
     * 
     * @return The current working directory path
     */
    public Path getRootPath() {
        return getCurrentWorkingDirectory();
    }
}