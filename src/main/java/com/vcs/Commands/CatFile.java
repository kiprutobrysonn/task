package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.InflaterInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "cat-file", description = "Retrieve and display contents of a blob object", mixinStandardHelpOptions = true)
public class CatFile implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger(CatFile.class);

    @Option(names = { "-p", "--print" }, description = "Pretty-print the contents of the object")
    private boolean print;

    @Option(names = { "-t", "--type" }, description = "Show the type of the object")
    private boolean type;

    @Option(names = { "-s", "--size" }, description = "Show the size of the object")
    private boolean size;

    @Parameters(index = "0", description = "SHA-1 hash of the object")
    private String objectHash;

    @Override
    public void run() {
        try {
            // Validate the hash
            if (objectHash == null || objectHash.length() != 40) {
                throw new IllegalArgumentException("Invalid object hash. Must be 40 characters long.");
            }

            // Construct the file path
            String dirName = ".vcs/objects/" + objectHash.substring(0, 2);
            String fileName = objectHash.substring(2);
            File objectFile = new File(dirName, fileName);

            if (!objectFile.exists()) {
                LOGGER.error("Object file not found: {}", objectFile.getPath());
                return;
            }

            // Read and decompress the file
            byte[] decompressedContent = decompressFile(objectFile);

            // Process the content
            if (type) {
                // Extract and print object type
                String objectType = extractObjectType(decompressedContent);
                System.out.println(objectType);
            } else if (size) {
                // Extract and print object size
                int objectSize = extractObjectSize(decompressedContent);
                System.out.println(objectSize);
            } else if (print) {

                byte[] content = extractContent(decompressedContent);
                System.out.write(content);
                System.out.println();
            }

        } catch (Exception e) {
            LOGGER.error("Error processing object", e);
        }
    }

    /**
     * Decompresses the contents of a blob object file.
     *
     * @param objectFile The file to decompress
     * @return Decompressed byte array
     * @throws IOException If an I/O error occurs during decompression
     */
    private byte[] decompressFile(File objectFile) throws IOException {
        try (InflaterInputStream inflater = new InflaterInputStream(Files.newInputStream(objectFile.toPath()))) {
            return inflater.readAllBytes();
        }
    }

    /**
     * Extracts the object type from the decompressed content.
     *
     * @param decompressedContent Decompressed byte array
     * @return Object type as a string
     */
    private String extractObjectType(byte[] decompressedContent) {
        int spaceIndex = 0;
        while (spaceIndex < decompressedContent.length && decompressedContent[spaceIndex] != ' ') {
            spaceIndex++;
        }
        return new String(decompressedContent, 0, spaceIndex);
    }

    /**
     * Extracts the object size from the decompressed content.
     *
     * @param decompressedContent Decompressed byte array
     * @return Object size as an integer
     */
    private int extractObjectSize(byte[] decompressedContent) {
        int spaceIndex = 0;
        while (spaceIndex < decompressedContent.length && decompressedContent[spaceIndex] != ' ') {
            spaceIndex++;
        }

        int nullIndex = spaceIndex + 1;
        while (nullIndex < decompressedContent.length && decompressedContent[nullIndex] != 0) {
            nullIndex++;
        }

        return Integer.parseInt(new String(decompressedContent, spaceIndex + 1, nullIndex - spaceIndex - 1));
    }

    /**
     * Extracts the actual content from the decompressed blob.
     *
     * @param decompressedContent Decompressed byte array
     * @return Byte array of the actual content
     */
    private byte[] extractContent(byte[] decompressedContent) {
        int nullIndex = 0;
        while (nullIndex < decompressedContent.length && decompressedContent[nullIndex] != 0) {
            nullIndex++;
        }

        byte[] content = new byte[decompressedContent.length - nullIndex - 1];
        System.arraycopy(decompressedContent, nullIndex + 1, content, 0, content.length);
        return content;
    }
}