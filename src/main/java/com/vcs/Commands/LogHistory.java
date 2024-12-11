package com.vcs.Commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.InflaterInputStream;

import com.vcs.Utils.Commit;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "log", description = "Show commit logs with advanced navigation")
public class LogHistory implements Runnable {
    private static final int COMMITS_PER_PAGE = 5;

    @Option(names = { "-n", "--number" }, description = "Number of commits to display")
    private int numberOfCommits = Integer.MAX_VALUE;

    @Option(names = { "-p", "--page" }, description = "Page number of commits")
    private int pageNumber = 1;

    @Override
    public void run() {
        logHistory();
    }

    public void logHistory() {
        try {
            List<Commit> commits = getAllCommits();

            int startIndex = (pageNumber - 1) * COMMITS_PER_PAGE;
            int endIndex = Math.min(startIndex + COMMITS_PER_PAGE,
                    Math.min(commits.size(), numberOfCommits));

            for (int i = startIndex; i < endIndex; i++) {
                displayCommit(commits.get(i));
            }

            System.out.printf("\nShowing commits %d-%d of %d total commits\n",
                    startIndex + 1, endIndex, commits.size());

            if (commits.size() > endIndex) {
                navigateCommits(commits, pageNumber);
            }
        } catch (Exception e) {
            System.err.println("Error retrieving commit history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Commit> getAllCommits() throws IOException {
        List<Commit> commits = new ArrayList<>();
        String currentCommitHash = getCurrentCommitHash();

        while (currentCommitHash != null) {
            Commit commit = readCommitInfo(currentCommitHash);
            if (commit == null)
                break;

            commits.add(commit);

            currentCommitHash = commit.getParent_commit_hash();
        }

        return commits;
    }

    private String getCurrentCommitHash() throws IOException {

        Path headFile = Paths.get(".vcs/refs/heads/" + getCurrentBranchName());
        if (!Files.exists(headFile)) {
            throw new IOException("HEAD file not found");
        }
        return new String(Files.readAllBytes(headFile)).trim();
    }

    public static String getCurrentBranchName() throws IOException {
        Path gitHeadPath = Paths.get(".vcs", "HEAD");

        try (BufferedReader reader = new BufferedReader(new FileReader(gitHeadPath.toFile()))) {
            String headContent = reader.readLine();

            if (headContent != null && headContent.startsWith("ref: refs/heads/")) {
                return headContent.substring("ref: refs/heads/".length());
            }

            return null;
        }
    }

    private Commit readCommitInfo(String commitHash) {
        try {
            String dirName = ".vcs/objects/" + commitHash.substring(0, 2);
            String fileName = commitHash.substring(2);
            File objectFile = new File(dirName, fileName);

            byte[] decompressedContent = decompressFile(objectFile);

            return parseCommitContent(new String(decompressedContent), commitHash);
        } catch (Exception e) {
            System.err.println("Error reading commit " + commitHash + ": " + e.getMessage());
            return null;
        }
    }

    private Commit parseCommitContent(String content, String treeHash) {
        String[] lines = content.split("\n");

        // Default values
        String author = "Unknown Author";
        String committer = "Unknown Committer";
        String parentHash = "";
        LocalDateTime timestamp = LocalDateTime.now();
        StringBuilder messageBuilder = new StringBuilder();

        boolean messageStarted = false;
        for (String line : lines) {
            if (line.startsWith("tree ")) {
                // We already have the tree hash from the filename
                continue;
            } else if (line.startsWith("parent ")) {
                parentHash = line.substring(7).trim();
            } else if (line.startsWith("author ")) {
                // Parse author info and timestamp
                String[] authorParts = line.substring(7).split(" ");
                author = String.join(" ",
                        java.util.Arrays.copyOfRange(authorParts, 0, authorParts.length - 2));
                timestamp = parseTimestamp(
                        String.join(" ",
                                java.util.Arrays.copyOfRange(authorParts, authorParts.length - 2, authorParts.length)));
            } else if (line.startsWith("committer ")) {
                // Parse committer info
                String[] committerParts = line.substring(10).split(" ");
                committer = String.join(" ",
                        java.util.Arrays.copyOfRange(committerParts, 0, committerParts.length - 2));
            } else if (line.isEmpty() && !messageStarted) {
                // First empty line indicates start of commit message
                messageStarted = true;
                continue;
            } else if (messageStarted) {
                // Collect commit message
                messageBuilder.append(line).append("\n");
            }
        }

        // Create and return Commit object
        Commit commit = new Commit(
                author,
                treeHash,
                parentHash,
                timestamp,
                messageBuilder.toString().trim());
        // commit.setCommitter(committer);

        return commit;
    }

    private LocalDateTime parseTimestamp(String timestampStr) {
        try {
            // Remove timezone offset
            String timestampNoOffset = timestampStr.substring(0, timestampStr.lastIndexOf(' '));

            // Parse timestamp
            return LocalDateTime.parse(
                    timestampNoOffset,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS"));
        } catch (Exception e) {
            System.err.println("Error parsing timestamp: " + timestampStr);
            return LocalDateTime.now();
        }
    }

    private void displayCommit(Commit commit) {
        // Colorful commit display
        String ANSI_RESET = "\u001B[0m";
        String ANSI_YELLOW = "\u001B[33m";
        String ANSI_BLUE = "\u001B[34m";
        String ANSI_GREEN = "\u001B[32m";

        System.out.println(ANSI_YELLOW + "commit " + commit.getShortHash() + ANSI_RESET);
        System.out.println(ANSI_BLUE + "Author: " + commit.getAuthor() + ANSI_RESET);
        System.out.println(ANSI_GREEN + "Date: " + commit.getTimeStamp() + ANSI_RESET);
        System.out.println("\n    " + commit.getMessage() + "\n");
    }

    private void navigateCommits(List<Commit> commits, int currentPage) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("\nCommit Navigation:");
                System.out.println("n - Next page");
                System.out.println("p - Previous page");
                System.out.println("q - Quit");
                System.out.print("Enter your choice: ");

                String choice = scanner.nextLine().trim().toLowerCase();

                switch (choice) {
                    case "n":
                        if ((currentPage * COMMITS_PER_PAGE) < commits.size()) {
                            pageNumber = currentPage + 1;
                            logHistory();
                            return;
                        } else {
                            System.out.println("No more commits to display.");
                        }
                        break;
                    case "p":
                        if (currentPage > 1) {
                            pageNumber = currentPage - 1;
                            logHistory();
                            return;
                        } else {
                            System.out.println("Already at the first page.");
                        }
                        break;
                    case "q":
                        return;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            }
        }
    }

    private byte[] decompressFile(File objectFile) throws IOException {
        try (InflaterInputStream inflater = new InflaterInputStream(Files.newInputStream(objectFile.toPath()))) {
            return inflater.readAllBytes();
        }
    }
}