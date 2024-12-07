package com.vcs.Commands;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.InflaterInputStream;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "ls-tree", description = "List contents of a tree object", mixinStandardHelpOptions = true)
public class ReadTree implements Runnable {
    @Parameters(index = "0", description = "Hash of the tree object to list")
    private String treeHash;

    @Option(names = { "-n", "--name-only" }, description = "List contents recursively")
    private boolean nameOnly;

    @Override
    public void run() {
        try {
            List<TreeEntryDisplay> entries = listTreeContents(treeHash);

            if (nameOnly) {
                for (TreeEntryDisplay entry : entries) {
                    System.out.println(entry.name);
                }
                return;
            }
            for (TreeEntryDisplay entry : entries) {
                System.out.printf("%s %s %s\t%s%n",
                        entry.mode,
                        entry.type,
                        entry.hash,
                        entry.name);
            }
        } catch (Exception e) {
            System.err.println("Error listing tree contents: " + e.getMessage());
        }
    }

    /**
     * Reads and lists contents of a tree object from the object store.
     * 
     * @param treeHash SHA-1 hash of the tree object
     * @return List of tree entry details
     * @throws IOException If an I/O error occurs reading the object
     */
    public static List<TreeEntryDisplay> listTreeContents(String treeHash) throws IOException {
        // Construct object file path
        String dirName = ".vcs/objects/" + treeHash.substring(0, 2);
        String fileName = dirName + "/" + treeHash.substring(2);
        File objectFile = new File(fileName);

        if (!objectFile.exists()) {
            throw new IOException("Tree object not found: " + treeHash);
        }

        // Read and decompress object file
        byte[] compressedContent = Files.readAllBytes(objectFile.toPath());
        byte[] decompressedContent = decompressContent(compressedContent);

        // Parse decompressed content
        return parseTreeContent(decompressedContent);
    }

    /**
     * Decompresses the content of a compressed object file.
     * 
     * @param compressedContent Compressed byte array
     * @return Decompressed byte array
     * @throws IOException If decompression fails
     */
    private static byte[] decompressContent(byte[] compressedContent) throws IOException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedContent);
                InflaterInputStream inflater = new InflaterInputStream(bais)) {
            return inflater.readAllBytes();
        }
    }

    /**
     * Parses the raw content of a tree object.
     * 
     * @param content Raw bytes of the tree object
     * @return List of tree entry details
     */
    private static List<TreeEntryDisplay> parseTreeContent(byte[] content) {
        List<TreeEntryDisplay> entries = new ArrayList<>();
        int offset = 0;

        // Skip header (type + space + length + null byte)
        while (content[offset] != 0)
            offset++;
        offset++; // Skip null byte

        while (offset < content.length) {
            // Parse mode
            int modeEnd = findNextByte(content, offset, (byte) ' ');
            String mode = new String(content, offset, modeEnd - offset);

            // Parse name
            offset = modeEnd + 1;
            int nameEnd = findNextByte(content, offset, (byte) 0);
            String name = new String(content, offset, nameEnd - offset);

            // Parse hash
            offset = nameEnd + 1;
            String hash = bytesToHex(content, offset, offset + 20);

            // Determine type based on mode
            String type = mode.startsWith("04") ? "tree" : "blob";

            entries.add(new TreeEntryDisplay(mode, type, hash, name));

            // Move to next entry
            offset += 20;
        }

        return entries;
    }

    /**
     * Finds the index of the next specified byte.
     * 
     * @param content Byte array to search
     * @param start   Starting index
     * @param target  Byte to find
     * @return Index of the target byte
     */
    private static int findNextByte(byte[] content, int start, byte target) {
        for (int i = start; i < content.length; i++) {
            if (content[i] == target)
                return i;
        }
        return content.length;
    }

    /**
     * Converts a portion of a byte array to a hex string.
     * 
     * @param content Byte array
     * @param start   Start index
     * @param end     End index
     * @return Hex representation of the byte array section
     */
    private static String bytesToHex(byte[] content, int start, int end) {
        StringBuilder hex = new StringBuilder();
        for (int i = start; i < end; i++) {
            hex.append(String.format("%02x", content[i]));
        }
        return hex.toString();
    }

    /**
     * Represents a tree entry for display purposes.
     */
    public static class TreeEntryDisplay {
        public final String mode;
        public final String type;
        public final String hash;
        public final String name;

        public TreeEntryDisplay(String mode, String type, String hash, String name) {
            this.mode = mode;
            this.type = type;
            this.hash = hash;
            this.name = name;
        }
    }
}