<!-- # VCS (Version Control System) -->

## Overview

This is a custom Version Control System (VCS) implemented with core Git-like functionality. The VCS provides essential version control operations to manage your project's source code and track changes.

## Prerequisites

- Java
- Maven

## Installation

### Building the Project

1. Navigate to the project directory
2. Run the build command:
   ```bash
   mvn clean package
   ```

### Setup

After building the project, navigate to the `run.sh` script and modify the Java JAR path:

```bash
# Open the run.sh file
nano run.sh

# Update the exec line to use the correct path to your generated JAR file
# Replace the existing line:
# exec java -jar </home/bryce/Code/gigs/task/target/task-1.0-SNAPSHOT.jar >"$@"
# With the actual path to your generated JAR file
```

## Commands

### Basic Operations

- `vcs init`: Initialize a new repository
- `vcs add`: Stage files for commit
- `vcs commit`: Create a new commit
- `vcs status`: Show the current state of the working tree
- `vcs log`: View commit history

### Object Management

- `vcs hash-object`: Create a new blob object
- `vcs cat-file`: Retrieve contents of a blob object
- `vcs write-tree`: Create a new tree object from staged files
- `vcs ls-tree`: List contents of a tree object
- `vcs commit-tree`: Create a new commit object

### Branch Operations

- `vcs branch`: List, create, or delete branches
- `vcs switch`: Change the current branch

### Comparison

- `vcs diff`: Show changes between commits

## Global Options

- `-h, --help`: Show help message
- `-V, --version`: Print version information

## Example Workflow

```bash
# Initialize a new repository
vcs init

# Add files to the staging area
vcs add myfile.txt

# Commit changes
vcs commit -m "Initial commit"

# View commit history
vcs log

# Switch branches
vcs switch new-feature
```

## Create a .vcsignore on the working directory

- Add files or patterns the way you would on git to ignore them

## Troubleshooting

- Ensure Java and Maven are correctly installed
- Verify the path to the JAR file in `run.sh`
- Check permissions of the `run.sh` script
