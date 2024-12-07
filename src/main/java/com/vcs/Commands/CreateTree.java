package com.vcs.Commands;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.DeflaterOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vcs.Utils.TreeEntry;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "hash-tree", description = "Creates a new tree object", mixinStandardHelpOptions = true)
public class CreateTree implements Runnable {
    private static final byte[] OBJECT_TYPE_TREE = "tree".getBytes();
    private static final byte[] SPACE = " ".getBytes();
    private static final byte[] NULL = { 0 };
    private static final Logger LOGGER = LogManager.getLogger(CreateTree.class);

    @Option(names = { "-w", "--write" }, description = "Writes the tree to the object store")
    private static boolean write = false;

    @Parameters(index = "0", description = "Directory to hash into a tree object")
    private File directory;

    @Override
    public void run() {
        try {
            String treeHash = hashTree(directory);
            LOGGER.info("Tree object hash: {}", treeHash);

            if (write) {
                LOGGER.info("Tree object created in the repository");
            }
        } catch (Exception e) {
            LOGGER.error("Error processing directory", e);
        }
    }

    /**
     * Computes the SHA-1 hash of the tree object and optionally writes it.
     * 
     * @param directory Root directory to hash
     * @return SHA-1 hash of the tree object
     * @throws IOException              If an I/O error occurs
     * @throws NoSuchAlgorithmException If hash computation fails
     */
    public static String hashTree(File directory) throws IOException, NoSuchAlgorithmException {
        // Recursively collect tree entries
        List<TreeEntry> entries = collectTreeEntries(directory);

        // Sort entries lexicographically by name
        entries.sort(Comparator.comparing(TreeEntry::getName));

        // Compute raw tree content
        byte[] treeContent = computeTreeContent(entries);

        // Compute hash
        MessageDigest hash = MessageDigest.getInstance("SHA-1");
        hash.update(OBJECT_TYPE_TREE);
        hash.update(SPACE);
        hash.update(String.valueOf(treeContent.length).getBytes());
        hash.update(NULL);
        hash.update(treeContent);

        byte[] hashedBytes = hash.digest();
        String hashedString = HexFormat.of().formatHex(hashedBytes);

        // Write to object store if write flag is true
        if (write) {
            writeTreeObject(hashedString, treeContent);
        }

        return hashedString;
    }

    /**
     * Collects tree entries from a directory recursively.
     * 
     * @param directory Root directory
     * @return List of tree entries
     * @throws IOException              If an I/O error occurs
     * @throws NoSuchAlgorithmException If hash computation fails
     */
    private static List<TreeEntry> collectTreeEntries(File directory)
            throws IOException, NoSuchAlgorithmException {
        List<TreeEntry> entries = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files == null)
            return entries;

        for (File file : files) {
            // Skip .vcs and hidden directories
            if (file.isHidden() || file.getName().startsWith("."))
                continue;

            entries.add(new TreeEntry(file));
        }

        return entries;
    }

    /**
     * Computes the raw bytes for a tree object.
     * 
     * @param entries Sorted list of tree entries
     * @return Raw bytes of the tree object
     */
    private static byte[] computeTreeContent(List<TreeEntry> entries)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (TreeEntry entry : entries) {
            byte[] entryBytes = entry.toRawBytes();
            baos.write(entryBytes);
        }

        return baos.toByteArray();
    }

    /**
     * Writes the tree object to the object store.
     * 
     * @param hashedString SHA-1 hash of the tree object
     * @param treeContent  Raw tree content bytes
     * @throws IOException If an I/O error occurs
     */
    private static void writeTreeObject(String hashedString, byte[] treeContent)
            throws IOException {
        String dirName = ".vcs/objects/" + hashedString.substring(0, 2);
        String fileName = dirName + "/" + hashedString.substring(2);
        File dir = new File(dirName);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(fileName);
        try (var outPutStream = Files.newOutputStream(file.toPath());
                DeflaterOutputStream deflater = new DeflaterOutputStream(outPutStream)) {
            deflater.write(OBJECT_TYPE_TREE);
            deflater.write(SPACE);
            deflater.write(String.valueOf(treeContent.length).getBytes());
            deflater.write(NULL);
            deflater.write(treeContent);
            deflater.finish();
        }
    }

}