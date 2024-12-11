package com.vcs.Commands;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DeflaterOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vcs.Utils.StagingArea;
import com.vcs.Utils.TreeEntry;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "write-tree", description = "Creates a new tree object from staged files", mixinStandardHelpOptions = true)
public class CreateTree implements Runnable {
    private static final byte[] OBJECT_TYPE_TREE = "tree".getBytes();
    private static final byte[] SPACE = " ".getBytes();
    private static final byte[] NULL = { 0 };
    private static final Logger LOGGER = LogManager.getLogger(CreateTree.class);

    @Option(names = { "-w", "--write" }, description = "Writes the tree to the object store")
    private boolean write = true;

    @Override
    public void run() {
        try {
            String treeHash = writeTreeFromStagedFiles();
            LOGGER.info("Tree object hash: {}", treeHash);
        } catch (Exception e) {
            LOGGER.error("Error creating tree object", e);
        }
    }

    /**
     * Creates a tree object from staged files.
     * 
     * @return SHA-1 hash of the tree object
     * @throws IOException              If an I/O error occurs
     * @throws NoSuchAlgorithmException If hash computation fails
     */
    public static String writeTreeFromStagedFiles() throws IOException, NoSuchAlgorithmException {
        // Get staged entries from the staging area
        StagingArea stagingArea = new StagingArea();
        Map<String, String> stagedEntries = stagingArea.getStagedFiles();

        // Collect tree entries from staged files and directories
        List<TreeEntry> entries = collectTreeEntriesFromStaged(stagedEntries);

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

        // Write to object store
        writeTreeObject(hashedString, treeContent);

        return hashedString;
    }

    /**
     * Creates a tree object for a specific directory.
     * 
     * @param dirPath Path to the directory
     * @return SHA-1 hash of the tree object
     * @throws IOException              If an I/O error occurs
     * @throws NoSuchAlgorithmException If hash computation fails
     */
    public static String createTreeForDirectory(Path dirPath) throws IOException, NoSuchAlgorithmException {
        // Collect all files and subdirectories in this directory
        List<TreeEntry> entries = new ArrayList<>();

        try (Stream<Path> stream = Files.list(dirPath)) {
            for (Path path : (Iterable<Path>) stream::iterator) {
                // Skip hidden files and directories
                if (isHiddenPath(path.toString())) {
                    continue;
                }

                String name = path.getFileName().toString();
                if (Files.isDirectory(path)) {
                    // Recursive tree creation for subdirectories
                    String subTreeHash = createTreeForDirectory(path);
                    entries.add(new TreeEntry(new File(dirPath.toFile(), name)));
                } else if (Files.isRegularFile(path)) {
                    // For files, create a blob
                    byte[] bytes = Files.readAllBytes(path);
                    String fileHash = CreateBlob.hashObject(bytes, true);
                    entries.add(new TreeEntry(new File(dirPath.toFile(), name)));
                }
            }
        }

        // Sort entries lexicographically
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

        // Write tree object to object store
        writeTreeObject(hashedString, treeContent);

        return hashedString;
    }

    /**
     * Collects tree entries from staged files and directories.
     * 
     * @param stagedEntries Map of staged entries
     * @return List of tree entries
     * @throws IOException              If an I/O error occurs
     * @throws NoSuchAlgorithmException If hash computation fails
     */
    private static List<TreeEntry> collectTreeEntriesFromStaged(Map<String, String> stagedEntries)
            throws IOException, NoSuchAlgorithmException {
        List<TreeEntry> entries = new ArrayList<>();

        // Group staged entries by their top-level directory
        Map<String, List<Map.Entry<String, String>>> groupedEntries = stagedEntries.entrySet().stream()
                .filter(entry -> !isHiddenPath(entry.getKey()))
                .collect(Collectors.groupingBy(
                        entry -> getTopLevelDirectory(entry.getKey()),
                        Collectors.toList()));

        // Process each top-level directory group
        for (Map.Entry<String, List<Map.Entry<String, String>>> dirGroup : groupedEntries.entrySet()) {
            String topLevelDir = dirGroup.getKey();
            List<Map.Entry<String, String>> dirEntries = dirGroup.getValue();

            // If it's a directory with multiple entries
            if (dirEntries.size() > 1 || hasSubdirectories(dirEntries)) {
                // Create a subtree for this directory
                List<TreeEntry> subEntries = new ArrayList<>();

                for (Map.Entry<String, String> entry : dirEntries) {
                    Path filePath = Paths.get(entry.getKey());
                    String relativePath = topLevelDir.isEmpty() ? filePath.getFileName().toString()
                            : filePath.toString().substring(topLevelDir.length() + 1);

                    // Create TreeEntry with the file
                    subEntries.add(new TreeEntry(filePath.toFile()));
                }

                // Sort subtree entries
                subEntries.sort(Comparator.comparing(TreeEntry::getName));

                // Compute subtree content and hash
                byte[] subTreeContent = computeTreeContent(subEntries);
                MessageDigest hash = MessageDigest.getInstance("SHA-1");
                hash.update(OBJECT_TYPE_TREE);
                hash.update(SPACE);
                hash.update(String.valueOf(subTreeContent.length).getBytes());
                hash.update(NULL);
                hash.update(subTreeContent);
                byte[] hashedBytes = hash.digest();
                String subTreeHash = HexFormat.of().formatHex(hashedBytes);

                // Write subtree to object store
                writeTreeObject(subTreeHash, subTreeContent);

                // Add subtree to main tree entries
                entries.add(new TreeEntry(
                        new File(topLevelDir),
                        TreeEntry.EntryType.TREE,
                        subTreeHash));
            } else {
                // For single file or root-level files
                for (Map.Entry<String, String> entry : dirEntries) {
                    Path filePath = Paths.get(entry.getKey());
                    entries.add(new TreeEntry(filePath.toFile()));
                }
            }
        }

        return entries;
    }

    /**
     * Get the top-level directory of a path.
     * 
     * @param path Path to get top-level directory for
     * @return Top-level directory path
     */
    private static String getTopLevelDirectory(String path) {
        // Remove leading slash if present
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;

        // Find the first slash
        int firstSlash = normalizedPath.indexOf('/');

        // If no slash, it's a root-level file
        return firstSlash > 0 ? normalizedPath.substring(0, firstSlash) : "";
    }

    /**
     * Check if the entries contain subdirectories.
     * 
     * @param entries List of entries to check
     * @return true if entries contain subdirectories, false otherwise
     */
    private static boolean hasSubdirectories(List<Map.Entry<String, String>> entries) {
        return entries.stream()
                .anyMatch(entry -> entry.getKey().contains("/"));
    }

    /**
     * Check if a path is hidden
     * 
     * @param path Path to check
     * @return true if path is hidden, false otherwise
     */
    private static boolean isHiddenPath(String path) {
        return path.contains("/.") || // Unix-like hidden files/directories
                path.contains("\\.") || // Windows hidden files/directories
                path.startsWith(".") ||
                path.contains("/.");
    }

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