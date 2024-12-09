package com.vcs.Utils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.Setter;

public class Commit {
    // Formatter for RFC 2822 style date used by Git
    private static final DateTimeFormatter GIT_DATE_FORMATTER = DateTimeFormatter
            .ofPattern("EEE MMM dd HH:mm:ss yyyy Z");

    public Commit(String author, String tree_hash, String parent_commit_hash, LocalDateTime timestamp,
            String message) {
        this.author = author;
        this.tree_hash = tree_hash;
        this.parent_commit_hash = parent_commit_hash;
        this.timestamp = timestamp;
        this.message = message;
        // Default committer to author if not specified
        this.committer = author;
    }

    @Getter
    @Setter
    private String author;

    @Getter
    @Setter
    private String committer;

    @Getter
    @Setter
    private String tree_hash;

    @Setter
    private String parent_commit_hash;

    @Setter
    private LocalDateTime timestamp;

    @Getter
    @Setter
    private String message;

    public String getParent_commit_hash() {
        return parent_commit_hash;
    }

    public String getAuthor() {
        return author;
    }

    public String getTimeStamp() {
        return timestamp.toString();
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        // Construct a Git-like commit format
        StringBuilder commitString = new StringBuilder();

        // Add tree hash
        commitString.append("tree ").append(tree_hash).append("\n");

        // Add parent commit hash (if exists)
        if (parent_commit_hash != null && !parent_commit_hash.isEmpty()) {
            commitString.append("parent ").append(parent_commit_hash).append("\n");
        }

        // Format author line
        commitString.append("author ").append(author)
                .append(" ").append(formatTimestamp(timestamp))
                .append("\n");

        // Format committer line
        commitString.append("committer ").append(committer)
                .append(" ").append(formatTimestamp(timestamp))
                .append("\n");

        // Add empty line before commit message (Git standard)
        commitString.append("\n");

        // Add commit message
        commitString.append(message);

        return commitString.toString();
    }

    /**
     * Format timestamp to match Git's commit timestamp format
     * Example: "Wed Feb 22 10:38:36 2023 +0000"
     * 
     * @param dateTime LocalDateTime to format
     * @return Formatted timestamp string
     */
    private String formatTimestamp(LocalDateTime dateTime) {
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneOffset.UTC);
        return zonedDateTime.format(GIT_DATE_FORMATTER);
    }

    /**
     * Create a shortened commit hash representation
     * 
     * @return First 7 characters of the commit hash (if available)
     */
    public String getShortHash() {
        return tree_hash != null && tree_hash.length() > 7
                ? tree_hash.substring(0, 7)
                : tree_hash;
    }
}