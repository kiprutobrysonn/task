package com.vcs.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import com.vcs.Commands.CreateBlob;

// lets make it static
public class StagingArea {

    private static final String OBJECTS_DIR = ".vcs/objects";
    private Map<String, String> stagedFiles;

    private String INDEX_FILE = ".vcs/index";

    public StagingArea() {
        this.stagedFiles = new HashMap<>();
        loadIndex();
    }

    public static StagingArea getInstanceArea() {
        return new StagingArea();
    }

    public void add(Path filePath) throws IOException, NoSuchAlgorithmException {
        // Validate file exists and is readable
        if (!Files.exists(filePath) || !Files.isReadable(filePath)) {
            throw new IOException("Cannot read file: " + filePath);
        }

        // Differentiate between a file and dir
        if (Files.isDirectory(filePath)) {
            Files.walk(filePath).filter(arg0 -> {
                try {
                    return Files.isHidden(arg0);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return false;
            }).filter(Files::isRegularFile).forEach(file -> {
                try {
                    addFile(file);
                } catch (IOException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
            });
        } else {
            addFile(filePath);
        }
    }

    private void addFile(Path filePath) throws IOException, NoSuchAlgorithmException {
        // Calculate file hash
        byte[] bytes = Files.readAllBytes(filePath);
        String fileHash = CreateBlob.hashObject(bytes, true);

        // Add to staged files
        stagedFiles.put(filePath.toString(), fileHash);

        // Update index file
        saveIndex();
    }

    // Remove a file from staging area
    public void remove(Path filePath) throws IOException {
        if (stagedFiles.remove(filePath.toString()) != null) {
            saveIndex();
        }
    }

    // Clear staging area
    public void clear() {
        stagedFiles.clear();
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
                stagedFiles = new HashMap<>();
                Files.lines(indexPath).forEach(line -> {
                    String[] parts = line.split(":");
                    stagedFiles.put(parts[0], parts[1]);
                });
            }
        } catch (IOException e) {
            stagedFiles = new HashMap<>();
        }
    }

    private void saveIndex() throws IOException {
        Path indexPath = Paths.get(INDEX_FILE);
        Files.createDirectories(indexPath.getParent());

        Stream<String> indexEntries = stagedFiles.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue());

        Files.write(indexPath, (Iterable<String>) indexEntries::iterator);
    }

    public Map<String, String> getStagedFiles() {
        return new HashMap<>(stagedFiles);
    }
}
