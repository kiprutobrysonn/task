package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.vcs.Utils.StagingArea;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "commit", description = "Create a new commit object")
public class Commit implements Runnable {

    StagingArea stagingArea = StagingArea.getInstanceArea();
    private String OBJECTS_DIR = ".vcs/objects";

    @Option(names = "-m", description = "Commit message")
    private String commitMessage;

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            commit(commitMessage);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

    }

    private void commit(String message) throws IOException, NoSuchAlgorithmException {
        // Check if there are any staged files
        if (stagingArea.getStagedFiles().isEmpty()) {
            System.out.println("No changes to commit");
            return;
        }

        // Create commit
        createCommit(stagingArea);

        // Clear staging area after commit
        stagingArea.clear();

    }

    public void createCommit(StagingArea stagingArea) throws IOException, NoSuchAlgorithmException {
        // Create tree object from staged files
        String treeHash = CreateTree.hashTree(new File("src"));

        // Hash and store commit object
        CommitTree.commitTreeCommand(treeHash, null, commitMessage);
    }

    private String createTreeObject(StagingArea stagingArea) throws IOException {
        StringBuilder treeContent = new StringBuilder();

        for (Map.Entry<String, String> entry : stagingArea.getStagedFiles().entrySet()) {
            Path filePath = Paths.get(entry.getKey());
            String fileHash = entry.getValue();

            // Format: mode path hash
            treeContent.append(String.format("100644 %s %s\n",
                    filePath.getFileName(),
                    fileHash));
        }

        // Hash and store tree object
        return hashTreeObject(treeContent.toString());
    }

    // Hash tree object
    private String hashTreeObject(String treeContent) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] treeBytes = treeContent.getBytes();
            byte[] hashBytes = digest.digest(treeBytes);

            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }

            // Store tree object
            Path treePath = Paths.get(OBJECTS_DIR, hexString.toString());
            Files.createDirectories(treePath.getParent());
            Files.writeString(treePath, treeContent);

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }
}