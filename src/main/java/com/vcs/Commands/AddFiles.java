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
    IgnoreManager ignore = new IgnoreManager();
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
                continue;
            }
            Path filePath = Paths.get(fileName);
            stagingArea.add(filePath);
            System.out.println("Added " + fileName + " to staging area");
        }
    }

}
