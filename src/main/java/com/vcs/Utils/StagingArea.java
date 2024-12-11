package com.vcs.Utils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.vcs.Commands.CreateBlob;
import com.vcs.Commands.CreateTree;

public class StagingArea {
    private static final String OBJECTS_DIR = ".vcs/objects";
    private Map<String, String> stagedEntries;
    private static final String INDEX_FILE = ".vcs/index";
    private static IgnoreManager ignore = new IgnoreManager();
    private Path projectRoot;

    public StagingArea() {
        this.stagedEntries = new HashMap<>();
        this.projectRoot = Paths.get(System.getProperty("user.dir"));
        loadIndex();
    }

    public void add(Path path) throws IOException, NoSuchAlgorithmException {
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
                    stagedEntries.putIfAbsent(relativePath.toString(), treeHash);

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
                    stagedEntries.putIfAbsent(relativePath.toString(), fileHash);

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

        // Update index file
        saveIndex();
    }

    // Helper method to check if a path is hidden
    private boolean isHiddenPath(String path) {
        return path.contains("/.") || // Unix-like hidden files/directories
                path.contains("\\.") || // Windows hidden files/directories
                path.startsWith(".") ||
                path.contains("/.");
    }

    // Remove a file from staging area
    public void remove(Path filePath) throws IOException {
        if (stagedEntries.remove(filePath.toString()) != null) {
            saveIndex();
        }
    }

    // Clear staging area
    public void clear() {
        stagedEntries.clear();
        try {
            saveIndex();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void loadIndex() {
        try {
            Path indexPath = Paths.get(INDEX_FILE);
            if (Files.exists(indexPath)) {
                stagedEntries = new HashMap<>();
                Files.lines(indexPath).forEach(line -> {
                    String[] parts = line.split(":");
                    stagedEntries.put(parts[0], parts[1]);
                });
            }
        } catch (IOException e) {
            stagedEntries = new HashMap<>();
        }
    }

    private void saveIndex() throws IOException {
        Path indexPath = Paths.get(INDEX_FILE);
        Files.createDirectories(indexPath.getParent());

        Stream<String> indexEntries = stagedEntries.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue());

        Files.write(indexPath, (Iterable<String>) indexEntries::iterator);
    }

    public Map<String, String> getStagedFiles() {
        return new HashMap<>(stagedEntries);
    }
}