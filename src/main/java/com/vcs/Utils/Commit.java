package com.vcs.Utils;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

public class Commit {

    public Commit(String author, String committer, String tree_hash, String parent_commit_hash, LocalDateTime timestamp,
            String message) {
        this.author = author;
        this.committer = committer;
        this.tree_hash = tree_hash;
        this.parent_commit_hash = parent_commit_hash;
        this.timestamp = timestamp;
        this.message = message;
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
    @Getter
    @Setter
    private String parent_commit_hash;
    @Getter
    @Setter
    private LocalDateTime timestamp;
    @Getter
    @Setter
    private String message;

    @Override
    public String toString() {
        return String.format("tree %s\nparent %s\nauthor %s %s\ncommitter %s %s\n\n%s\n", tree_hash, parent_commit_hash,
                author, timestamp.toString(), author, timestamp.toString(), message);
    }

}
