package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.InflaterInputStream;

import com.vcs.Commands.ReadTree.TreeEntryDisplay;
import com.vcs.Utils.IgnoreManager;
import com.vcs.Utils.StagingArea;

import picocli.CommandLine.Command;

@Command(name = "status", description = "Show the working tree status", mixinStandardHelpOptions = true)
public class ShowStatus implements Runnable {

    private StagingArea stagingArea = new StagingArea();
    private IgnoreManager ignore = new IgnoreManager();
    final String ANSI_YELLOW = "\u001B[33m";
    final String ANSI_BLUE = "\u001B[34m";
    final String ANSI_GREEN = "\u001B[32m";

    @Override
    public void run() {
        // TODO Auto-generated method stub

        // showStatus();
        statusReport();

    }

    private void showStatus() {
        var stagedFiles = stagingArea.getStagedFiles();

        System.out.println("Staged files:");
        if (stagedFiles.isEmpty()) {
            System.out.println("  (no files staged)");
        } else {
            stagedFiles.forEach((path, hash) -> System.out.println("  " + path));
        }
    }

    private void statusReport() {
        // Show staged files (as you've already implemented)
        var stagedFiles = stagingArea.getStagedFiles();

        System.out.println("Staged files:");
        if (stagedFiles.isEmpty()) {
            System.out.println("  (no files staged)");
        } else {
            stagedFiles.forEach((path, hash) -> System.out.println("  " + path));
        }

        System.out.println("\nModified files:");
        Map<String, String> workingTreeFiles = listWorkingTreeFiles();
        Map<String, String> headTreeFiles = getHeadTreeFiles();
        System.out.println(workingTreeFiles);
        System.out.println(headTreeFiles);

        boolean modifiedFound = false;
        for (Map.Entry<String, String> entry : workingTreeFiles.entrySet()) {
            String path = entry.getKey();
            String currentFileHash = entry.getValue();

            String headFileHash = headTreeFiles.get(path);

            if (headFileHash == null) {
                // New file
                System.out.println("  (new)      " + path + ANSI_GREEN);
                modifiedFound = true;
            } else if (!currentFileHash.equals(headFileHash)) {
                // Modified file
                System.out.println("  (modified) " + path + ANSI_YELLOW);
                modifiedFound = true;
            }
        }

        // Check for deleted files
        for (String path : headTreeFiles.keySet()) {
            if (!workingTreeFiles.containsKey(path)) {
                System.out.println("  (deleted)  " + path);
                modifiedFound = true;
            }
        }

        if (!modifiedFound) {
            System.out.println("  (no changes)");
        }
    }

    // Helper method to get current working tree file hashes
    private Map<String, String> listWorkingTreeFiles() {
        Map<String, String> currentFiles = new HashMap<>();

        try {
            treeTraversal(getCurrentWorkingDirectory(), currentFiles);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
        }
        return currentFiles;
    }

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

    private void treeTraversal(Path path, Map<String, String> stagedEntries)
            throws IOException, NoSuchAlgorithmException {
        final Path projectRoot = Paths.get(System.getProperty("user.dir"));
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IOException("Cannot read path: " + path);
        }

        path = path.toAbsolutePath().normalize();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (isHiddenPath(dir.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                try {
                    String treeHash = CreateTree.createTreeForDirectory(dir);

                    Path relativePath = projectRoot.relativize(dir.toAbsolutePath());
                    stagedEntries.put(relativePath.toString(), treeHash);
                } catch (NoSuchAlgorithmException e) {
                    System.err.println("Error processing directory " + dir + ": " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (ignore.isIgnored(projectRoot.relativize(file))) {
                    return FileVisitResult.CONTINUE;
                }

                try {
                    // Calculate file hash
                    byte[] bytes = Files.readAllBytes(file);
                    String fileHash = CreateBlob.hashObject(bytes, true);

                    // Add to staged entries using relative path to project root
                    Path relativePath = projectRoot.relativize(file.toAbsolutePath());
                    stagedEntries.put(relativePath.toString(), fileHash);
                } catch (NoSuchAlgorithmException e) {
                    // Log error but continue traversal
                    System.err.println("Error processing file " + file + ": " + e.getMessage());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                // Log any access errors but continue traversal
                System.err.println("Failed to access " + file + ": " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

    }

    // Helper method to check if a path is hidden
    private boolean isHiddenPath(String path) {
        return path.contains("/.") || // Unix-like hidden files/directories
                path.contains("\\.") || // Windows hidden files/directories
                path.startsWith(".") ||
                path.contains("/.");
    }

    // Helper method to get last committed tree file hashes
    private Map<String, String> getHeadTreeFiles() {
        Map<String, String> headFiles = new HashMap<>();

        try {
            String headRef = readHeadReference();

            String commitHash = readCommitHashFromRef(headRef);

            String treeHash = readTreeHashFromCommit(commitHash);

            populateTreeFiles(treeHash, ".", headFiles);

            return headFiles;
        } catch (IOException e) {
            // Log the error or handle it appropriately
            System.err.println("Error retrieving HEAD tree files: " + e.getMessage());
            return headFiles;
        }
    }

    private String readHeadReference() throws IOException {
        Path headFile = Paths.get(".vcs", "HEAD");
        String headContent = new String(Files.readAllBytes(headFile), StandardCharsets.UTF_8).trim();

        // Extract the branch reference from the HEAD file
        if (headContent.startsWith("ref: ")) {
            return headContent.substring(5); // Return the branch reference path
        }

        throw new IOException("Unexpected HEAD file format: " + headContent);
    }

    // Helper method to read the commit hash from a branch reference
    private String readCommitHashFromRef(String branchRef) throws IOException {
        Path refFile = Paths.get(".vcs", branchRef);

        // Ensure the reference file exists
        if (!Files.exists(refFile)) {
            throw new IOException("Branch reference file not found: " + branchRef);
        }

        return new String(Files.readAllBytes(refFile), StandardCharsets.UTF_8).trim();
    }

    private String readTreeHashFromCommit(String commitHash) throws IOException {
        Path commitObjectPath = Paths.get(".vcs", "objects",
                commitHash.substring(0, 2),
                commitHash.substring(2));

        if (!Files.exists(commitObjectPath)) {
            throw new IOException("Commit object file not found: " + commitHash);
        }

        byte[] decompressedContent = decompressFile(commitObjectPath.toFile());
        byte[] content = extractContent(decompressedContent);

        for (String line : new String(content, StandardCharsets.UTF_8).split("\n")) {
            if (line.startsWith("tree ")) {
                return line.substring(5).trim();
            }
        }

        throw new IOException("No tree hash found in commit object");
    }

    private byte[] extractContent(byte[] decompressedContent) {
        int nullIndex = 0;
        while (nullIndex < decompressedContent.length && decompressedContent[nullIndex] != 0) {
            nullIndex++;
        }

        byte[] content = new byte[decompressedContent.length - nullIndex - 1];
        System.arraycopy(decompressedContent, nullIndex + 1, content, 0, content.length);
        return content;
    }

    private byte[] decompressFile(File objectFile) throws IOException {
        try (InflaterInputStream inflater = new InflaterInputStream(Files.newInputStream(objectFile.toPath()))) {
            return inflater.readAllBytes();
        }
    }

    // Helper method to recursively populate file hashes from a tree object
    private void populateTreeFiles(String treeHash, String currentPath, Map<String, String> fileHashes)
            throws IOException {

        List<TreeEntryDisplay> entries = ReadTree.listTreeContents(treeHash);
        for (TreeEntryDisplay treeEntryDisplay : entries) {
            if (treeEntryDisplay.type.equals("tree")) {
                populateTreeFiles(treeEntryDisplay.hash, treeEntryDisplay.name, fileHashes);

            } else {
                fileHashes.put(currentPath, treeEntryDisplay.hash);
            }

        }

    }
}
