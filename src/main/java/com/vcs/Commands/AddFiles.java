package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import com.vcs.Utils.IgnoreManager;
import com.vcs.Utils.StagingArea;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "add", description = "Add files to the index")
public class AddFiles implements Runnable {
    StagingArea stagingArea = new StagingArea();
<<<<<<< HEAD
<<<<<<< HEAD
    IgnoreManager ignore = new IgnoreManager();
=======

>>>>>>> fc17812 (commit works and tree but now we have to work on grouping them to their dirs)
=======
    IgnoreManager ignore = new IgnoreManager();
>>>>>>> b9c9573 (Java)
    @Parameters(description = "Files to add to the index")
    private String[] fileNames;

    @Override
    public void run() {
        try {
            addFiles(fileNames);
        } catch (IOException | NoSuchAlgorithmException e) {
            System.out.println("The following paths are ignored by .vcsignore files:");
        }

    }

    private void addFiles(String[] fileNames) throws IOException, NoSuchAlgorithmException {

        for (String fileName : fileNames) {

            if (ignore.isIgnored(new File(fileName).toPath())) {
                throw new IOException("The file is in the ignore file");
            }
            Path filePath = Paths.get(fileName);
            stagingArea.add(filePath);
            System.out.println("Added " + fileName + " to staging area");
        }
    }

}
