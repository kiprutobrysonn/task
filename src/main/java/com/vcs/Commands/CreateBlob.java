package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.zip.DeflaterOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "hash-object", description = "Creates a new blob object", mixinStandardHelpOptions = true)
public class CreateBlob implements Runnable {

    private static final byte[] OBJECT_TYPE_BLOB = "blob".getBytes();
    private static final byte[] SPACE = " ".getBytes();
    private static final byte[] NULL = { 0 };
    private static final Logger LOGGER = LogManager.getLogger(CreateBlob.class);

    @Option(names = { "-w", "--write" }, description = "Writes the blob to the object store")
    private boolean write = false;

    @Parameters(index = "0", description = "File to hash and store")
    private File file;

    @Override
    public void run() {
        try {
            String hash = hashFile(file);
            LOGGER.info("Object hash: {}", hash);

            if (write) {
                LOGGER.info("Blob object created in the repository");
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            LOGGER.error("Error processing file", e);
        }
    }

    public String hashFile(File file) throws NoSuchAlgorithmException, IOException {

        // check if the file exits
        if (!file.exists()) {
            LOGGER.error("File does not exist");

            // exit
            System.exit(1);

        }
        byte[] bytes = Files.readAllBytes(file.toPath());
        return hashObject(bytes, write);
    }

    /**
     * Computes the SHA-1 hash of the given byte array and optionally writes the
     * blob.
     * 
     * @param bytes       the byte array representing the contents of the blob
     *                    object
     * @param shouldWrite whether to write the blob to the object store
     * @return the SHA-1 hash of the blob object
     * @throws NoSuchAlgorithmException if the SHA-1 algorithm is not available
     * @throws IOException              if an I/O error occurs
     */
    public static String hashObject(byte[] bytes, boolean shouldWrite)
            throws NoSuchAlgorithmException, IOException {
        byte[] lengthOfBytes = String.valueOf(bytes.length).getBytes();
        MessageDigest hash = MessageDigest.getInstance("SHA-1");
        hash.update(OBJECT_TYPE_BLOB);
        hash.update(SPACE);
        hash.update(lengthOfBytes);
        hash.update(NULL);
        hash.update(bytes);

        byte[] hashedBytes = hash.digest();
        String hashedString = HexFormat.of().formatHex(hashedBytes);

        if (shouldWrite) {
            String dirName = ".vcs/objects/" + hashedString.substring(0, 2);
            String fileName = hashedString.substring(2);

            // Ensure the directory exists
            new File(dirName).mkdirs();
            File blobFile = new File(dirName, fileName);

            try (var outPutStream = Files.newOutputStream(blobFile.toPath());
                    DeflaterOutputStream deflater = new DeflaterOutputStream(outPutStream)) {
                deflater.write(OBJECT_TYPE_BLOB);
                deflater.write(SPACE);
                deflater.write(lengthOfBytes);
                deflater.write(NULL);
                deflater.write(bytes);
                deflater.finish();
            }
        }

        return hashedString;
    }
}