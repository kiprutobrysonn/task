package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine.Command;

/**
 * Command to initialize a new repository.
 * 
 * This command creates the necessary directory structure for a new repository,
 * including the `.vcs` directory, `objects` and `refs` subdirectories, and the
 * `HEAD` file with a reference to the main branch.
 * 
 * If a repository already exists in the current directory, an error message is
 * logged.
 * 
 * Usage:
 * 
 * <pre>
 * {@code
 * InitialzieRepo init = new InitialzieRepo();
 * init.run();
 * }
 * </pre>
 * 
 * Logging:
 * - Logs an info message when initializing the repository.
 * - Logs an error message if the repository already exists.
 * - Logs an info message when the git directory is successfully initialized.
 * 
 * Exceptions:
 * - Throws a RuntimeException if an IOException occurs during the creation of
 * the `HEAD` file.
 */
@Command(name = "init")
public class InitialzieRepo implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(InitialzieRepo.class);

    @Override
    public void run() {
        // TODO Auto-generated method stub
        LOGGER.info("Initializing repository");
        initRepo();
    }

    private void initRepo() {

        // CHek first if the dir exits
        if (new File(".vcs").exists()) {
            LOGGER.error("Repository already exists");
            return;
        }
        final File root = new File(".vcs");
        new File(root, "objects").mkdirs();
        new File(root, "refs").mkdirs();
        final File head = new File(root, "HEAD");

        try {
            head.createNewFile();
            Files.write(head.toPath(), "ref: refs/heads/main\n".getBytes());
            LOGGER.info("Initialized git directory");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}