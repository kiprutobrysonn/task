package com.vcs.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import com.vcs.Commands.CreateBlob;
import com.vcs.Commands.CreateTree;

/**
 * Represents an entry in a tree object, which can be a file or a directory.
 */
public class TreeEntry {
    private final String mode; // File mode (e.g., 100644 for regular file, 040000 for directory)
    private final String type; // Object type (blob or tree)
    private final String hash; // SHA-1 hash of the object
    private final String name; // Name of the file or directory

    /**
     * Constructs a TreeEntry for a file or blob.
     * 
     * @param file The file to create a tree entry for
     * @throws IOException              If there's an error reading file permissions
     * @throws NoSuchAlgorithmException If hash computation fails
     */
    public TreeEntry(File file) throws IOException, NoSuchAlgorithmException {
        this.name = file.getName();

        // Determine file mode based on file permissions
        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(file.toPath());

        if (file.isDirectory()) {
            this.mode = "040000"; // Directory
            this.type = "tree";
            this.hash = CreateTree.hashTree(file);
        } else {
            // Regular file mode
            this.mode = determineFileMode(permissions);
            this.type = "blob";
            this.hash = CreateBlob.hashObject(Files.readAllBytes(file.toPath()), true);
        }
    }

    /**
     * Determines the file mode based on POSIX permissions.
     * 
     * @param permissions Set of file permissions
     * @return File mode as a string
     */
    private String determineFileMode(Set<PosixFilePermission> permissions) {
        boolean isExecutable = permissions.contains(PosixFilePermission.OWNER_EXECUTE) ||
                permissions.contains(PosixFilePermission.GROUP_EXECUTE) ||
                permissions.contains(PosixFilePermission.OTHERS_EXECUTE);

        return isExecutable ? "100755" : "100644";
    }

    //
    /**
     * Converts the tree entry to its git-style raw representation.
     * 
     * @return Byte array representation of the tree entry
     */
    public byte[] toRawBytes() {
        try {
            byte[] modeBytes = mode.getBytes();
            byte[] spaceBytes = " ".getBytes();
            byte[] nameBytes = name.getBytes();
            byte[] nullBytes = new byte[] { 0 };
            byte[] hashBytes = hexStringToByteArray(hash);

            byte[] rawEntry = new byte[modeBytes.length + spaceBytes.length +
                    nameBytes.length + nullBytes.length +
                    hashBytes.length];

            int pos = 0;
            System.arraycopy(modeBytes, 0, rawEntry, pos, modeBytes.length);
            pos += modeBytes.length;

            System.arraycopy(spaceBytes, 0, rawEntry, pos, spaceBytes.length);
            pos += spaceBytes.length;

            System.arraycopy(nameBytes, 0, rawEntry, pos, nameBytes.length);
            pos += nameBytes.length;

            System.arraycopy(nullBytes, 0, rawEntry, pos, nullBytes.length);
            pos += nullBytes.length;

            System.arraycopy(hashBytes, 0, rawEntry, pos, hashBytes.length);

            return rawEntry;
        } catch (Exception e) {
            throw new RuntimeException("Error converting tree entry to raw bytes", e);
        }
    }

    /**
     * Converts a hex string to a byte array.
     * 
     * @param hexString Hex-encoded string
     * @return Byte array representation
     */
    private byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    // Getters
    public String getMode() {
        return mode;
    }

    public String getType() {
        return type;
    }

    public String getHash() {
        return hash;
    }

    public String getName() {
        return name;
    }
}
