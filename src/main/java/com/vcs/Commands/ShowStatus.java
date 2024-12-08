package com.vcs.Commands;

import com.vcs.Utils.StagingArea;

import picocli.CommandLine.Command;

@Command(name = "status", description = "Show the working tree status", mixinStandardHelpOptions = true)
public class ShowStatus implements Runnable {

    private StagingArea stagingArea = new StagingArea();

    @Override
    public void run() {
        // TODO Auto-generated method stub

        showStatus();

    }

    private void showStatus() {
        var stagedFiles = stagingArea.getStagedFiles();

        System.out.println("Staged files:");
        if (stagedFiles.isEmpty()) {
            System.out.println("  (no files staged)");
        } else {
            stagedFiles.forEach((path, hash) -> System.out.println("  " + path + " (hash: " + hash + ")"));
        }
    }

}
