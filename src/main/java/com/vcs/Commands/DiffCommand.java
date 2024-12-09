package com.vcs.Commands;

import com.vcs.Utils.DiffTool;
import com.vcs.Utils.StagingArea;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "diff", description = "Show changes between commits")
public class DiffCommand implements Runnable {

    private StagingArea stage = new StagingArea();

    private DiffTool diffTool = new DiffTool(stage);

    @Parameters(description = "Commits to compare")
    private String[] commitHashes;

    // When no arguments passed it will do a diff between HEAD and HEAD^

    @Override
    public void run() {

        try {
            if (commitHashes == null) {
                diffTool.diffWorkingDirectory(stage);

            } else {
                diffTool.diffCommits(commitHashes[0], commitHashes[1]);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
