package com.vcs.Commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.io.Files;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "branch")
public class CreateBranch implements Runnable {

    @Parameters(index = "0", description = "Name of the branch to create")
    private String branchName;

    @Override
    public void run() {
        // TODO Auto-generated method stub

        createBranch(branchName);
    }

    private void createBranch(String name) {

        String root = ".vcs";
        File newBranch = new File(root, "refs/heads/" + name);
        String currentBranch;
        try {
            newBranch.createNewFile();
            currentBranch = getCurrentBranchName();

            Files.copy(new File(root, "refs/heads/" + currentBranch), new File(root, "refs/heads/" + name));

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static String getCurrentBranchName() throws IOException {
        Path gitHeadPath = Paths.get(".vcs", "HEAD");

        try (BufferedReader reader = new BufferedReader(new FileReader(gitHeadPath.toFile()))) {
            String headContent = reader.readLine();

            if (headContent != null && headContent.startsWith("ref: refs/heads/")) {
                return headContent.substring("ref: refs/heads/".length());
            }

            return null;
        }
    }

}
