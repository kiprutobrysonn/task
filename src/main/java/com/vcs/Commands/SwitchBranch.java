package com.vcs.Commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "switch", description = "Change the branch")
public class SwitchBranch implements Runnable {

    @Parameters(index = "0", description = "Name of the branch to switch")
    private String branchName;

    @Override
    public void run() {
        changeHead(branchName);
    }

    private void changeHead(String name) {
        String root = ".vcs";

        // Change the reference of HEAD

        try {
            if (!new File(root, "refs/heads/" + name).exists()) {

                throw new IOException("File not found");

            }
            File branch = new File(root, "HEAD");

            Files.write(branch.toPath(), ("ref: refs/heads/" + name + "\n").getBytes());

            System.out.println("Switched to branch" + name);
        } catch (Exception e) {
            System.out.println("Invalid branch name:" + name);
        }

    }

}
