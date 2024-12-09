package com.vcs.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.vcs.Commands.CreateBlob;

public class DiffTool {
    private static final String OBJECTS_DIR = ".vcs/objects/";
    private static final String HEAD_FILE = ".vcs/HEAD";
    private final StagingArea stage;

    public DiffTool(StagingArea stage) {
        this.stage = stage;
    }

    public DiffTool() {
        this.stage = new StagingArea();
    }

    // Diff between working directory and staged files
    public void diffWorkingDirectory(StagingArea stagingArea) throws IOException, NoSuchAlgorithmException {
        System.out.println("Changes in working directory:");

        // Get all files in current directory
        List<Path> allFiles = Files.walk(Paths.get("."))
                .filter(Files::isRegularFile)
                .filter(path -> !path.toString().startsWith("./.vcs"))
                .filter(path -> !path.toString().startsWith("./.git"))
                .filter(path -> !path.toString().startsWith("./target"))
                .collect(Collectors.toList());

        for (Path file : allFiles) {
            String filePath = file.toString();

            // Check if file is in staging area
            String stagedHash = stagingArea.getStagedFiles().get(filePath);

            if (stagedHash == null) {
                // Unstaged new file
                System.out.println("  + (new) " + filePath);
            } else {
                // Compare current file with staged version
                String currentHash = CreateBlob.hashObject(Files.readAllBytes(file), false);
                if (!currentHash.equals(stagedHash)) {
                    System.out.println("  * (modified) " + filePath);
                    printFileDiff(file, getFileContentFromHash(stagedHash));
                }
            }
        }

        // Check for staged files that no longer exist
        stagingArea.getStagedFiles().keySet().forEach(stagedFilePath -> {
            if (!Files.exists(Paths.get(stagedFilePath))) {
                System.out.println("  - (deleted) " + stagedFilePath);
            }
        });
    }

    // Diff between staged files and last commit
    public void diffStagedVsLastCommit(StagingArea stagingArea) throws IOException {
        System.out.println("Changes to be committed:");

        // Get last commit
        String lastCommitHash = getLastCommitHash();
        if (lastCommitHash == null) {
            // No previous commit, all staged files are new
            stagingArea.getStagedFiles().forEach((path, hash) -> System.out.println("  + (new) " + path));
            return;
        }

        // Get tree from last commit
        Map<String, String> lastCommitTree = getTreeFromCommit(lastCommitHash);

        // Compare staged files with last commit
        stagingArea.getStagedFiles().forEach((path, hash) -> {
            String lastCommitFileHash = lastCommitTree.get(path);

            if (lastCommitFileHash == null) {
                // New file in staging
                System.out.println("  + (new) " + path);
            } else if (!hash.equals(lastCommitFileHash)) {
                // Modified file
                System.out.println("  * (modified) " + path);
                try {
                    printFileDiff(
                            getFileContentFromHash(lastCommitFileHash),
                            getFileContentFromHash(hash));
                } catch (IOException e) {
                    System.err.println("Error reading file contents: " + e.getMessage());
                }
            }
        });
    }

    // Diff between two commits
    public void diffCommits(String commit1Hash, String commit2Hash) throws IOException {
        System.out.println("Diff between commits " + commit1Hash + " and " + commit2Hash);

        // Get trees from both commits
        Map<String, String> tree1 = getTreeFromCommit(commit1Hash);
        Map<String, String> tree2 = getTreeFromCommit(commit2Hash);

        // Find files in first commit
        tree1.forEach((path, hash) -> {
            String hash2 = tree2.get(path);

            if (hash2 == null) {
                // File deleted in second commit
                System.out.println("  - (deleted) " + path);
            } else if (!hash.equals(hash2)) {
                // File modified
                System.out.println("  * (modified) " + path);
                try {
                    printFileDiff(
                            getFileContentFromHash(hash),
                            getFileContentFromHash(hash2));
                } catch (IOException e) {
                    System.err.println("Error reading file contents: " + e.getMessage());
                }
            }
        });

        // Find new files in second commit
        tree2.forEach((path, hash) -> {
            if (!tree1.containsKey(path)) {
                System.out.println("  + (new) " + path);
            }
        });
    }

    // Utility method to hash file contents
    private String hashFile(Path filePath) throws IOException, NoSuchAlgorithmException {
        return Files.exists(filePath) ? CreateBlob.hashObject(Files.readAllBytes(filePath), false).toString() : null;
    }

    // Get last commit hash from HEAD
    private String getLastCommitHash() throws IOException {
        Path headPath = Paths.get(HEAD_FILE);
        return Files.exists(headPath) ? new String(Files.readAllBytes(headPath)).trim() : null;
    }

    // Extract tree from commit object
    private Map<String, String> getTreeFromCommit(String commitHash) throws IOException {
        Map<String, String> treeFiles = new HashMap<>();

        // Read commit object
        Path commitPath = Paths.get(OBJECTS_DIR + commitHash.substring(0, 2), commitHash.substring(2));
        List<String> commitLines = Files.readAllLines(commitPath);

        // Find tree hash
        String treeHash = commitLines.stream()
                .filter(line -> line.startsWith("tree "))
                .findFirst()
                .map(line -> line.substring(5))
                .orElseThrow(() -> new IOException("No tree found in commit"));

        // Read tree object
        Path treePath = Paths.get(OBJECTS_DIR, treeHash);
        List<String> treeEntries = Files.readAllLines(treePath);

        // Parse tree entries
        for (String entry : treeEntries) {
            String[] parts = entry.split(" ");
            if (parts.length == 3) {
                // parts[1] is path, parts[2] is hash
                treeFiles.put(parts[1], parts[2]);
            }
        }

        return treeFiles;
    }

    // Get file contents from object hash
    private List<String> getFileContentFromHash(String hash) throws IOException {
        Path filePath = Paths.get(OBJECTS_DIR, hash);
        return Files.readAllLines(filePath);
    }

    // Print diff between two file contents
    private void printFileDiff(Path file1, List<String> content2) throws IOException {
        printFileDiff(Files.readAllLines(file1), content2);
    }

    private void printFileDiff(List<String> content1, List<String> content2) {
        // Simple line-by-line diff
        int maxLines = Math.max(content1.size(), content2.size());

        for (int i = 0; i < maxLines; i++) {
            String line1 = i < content1.size() ? content1.get(i) : null;
            String line2 = i < content2.size() ? content2.get(i) : null;

            if (line1 == null) {
                System.out.println("+ " + line2); // Added line
            } else if (line2 == null) {
                System.out.println("- " + line1); // Removed line
            } else if (!line1.equals(line2)) {
                System.out.println("- " + line1); // Changed line
                System.out.println("+ " + line2);
            }
        }
    }
}