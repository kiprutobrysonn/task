package com.vcs.Commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.zip.DeflaterOutputStream;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Bytes;
import com.vcs.Utils.Commit;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "commit-tree", description = "Create a new commit object")
public class CommitTree implements Runnable {

    private static final String HEAD_FILE = ".vcs/HEAD";

    @Option(names = "-m", description = "Commit message")
    private String commitMessage;

    @Option(names = "-p", description = "Parent commit hash")
    private String parentCommitHash;

    @Parameters(index = "0", description = "Tree hash")
    private String treeHash;

    @Override

    public void run() {

        commitTreeCommand(treeHash, parentCommitHash, commitMessage);

    }

    // Update HEAD reference
    private static void updateHEAD(String commitHash) throws IOException {
        Path headPath = Paths.get(HEAD_FILE);
        Files.createDirectories(headPath.getParent());
        Files.writeString(headPath, commitHash);
    }

    public static void commitTreeCommand(String tree_hash, String parent_commit_hash,
            String message) {
        final String author = "test author";
        Commit commit = new Commit(author, author, tree_hash, parent_commit_hash, LocalDateTime.now(), message);

        byte[] content = commit.toString().getBytes();
        int length = content.length;
        byte[] blob_bytes = Bytes.concat("commit ".getBytes(), Integer.toString(length).getBytes(), new byte[] { 0 },
                content);

        String hash = BytesToHash(blob_bytes);

        File blob_file = new File(".vcs/objects/" + hash.substring(0, 2) + "/" + hash.substring(2));
        try {
            com.google.common.io.Files.createParentDirs(blob_file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // BE aware - not closing the output streams properly would cause incorrect
        // content
        // written to file (should close deflaterOutputStream first, then
        // FileOutputStream)
        try (OutputStream outputStream = new FileOutputStream(blob_file);
                DeflaterOutputStream deflaterOutputStream = new DeflaterOutputStream(outputStream)) {
            deflaterOutputStream.write(blob_bytes);
            updateHEAD(tree_hash);
            System.out.print(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String BytesToHash(byte[] bytes) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            byte[] sha = messageDigest.digest(bytes);
            return BaseEncoding.base16().lowerCase().encode(sha);
            // return HexFormat.of().formatHex(sha);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}
