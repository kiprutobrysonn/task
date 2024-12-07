package com.vcs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vcs.Commands.AddFiles;
import com.vcs.Commands.CatFile;
import com.vcs.Commands.Commit;
import com.vcs.Commands.CommitTree;
import com.vcs.Commands.CreateBlob;
import com.vcs.Commands.CreateTree;
import com.vcs.Commands.InitialzieRepo;
import com.vcs.Commands.ReadTree;
import com.vcs.Commands.ShowStatus;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "vcs", version = "vcs v 1.0.0", mixinStandardHelpOptions = true, subcommands = { InitialzieRepo.class,
        CreateBlob.class,
        CatFile.class,
        CreateTree.class,
        ReadTree.class,
        CommitTree.class,
        ShowStatus.class,
        AddFiles.class,
        Commit.class

})
public class Main implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        System.exit(new CommandLine(new Main()).execute(args));
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

}
