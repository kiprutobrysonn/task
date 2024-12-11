<!-- # ./run.sh (Version Control System) -->

## Overview

This is a custom Version Control System (./run.sh) implemented with core Git-like functionality. The ./run.sh provides essential version control operations to manage your project's source code and track changes.

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

- `./run.sh init`: Initialize a new repository
- `./run.sh add`: Stage files for commit
- `./run.sh commit`: Create a new commit
- `./run.sh status`: Show the current state of the working tree
- `./run.sh log`: View commit history

### Object Management

- `./run.sh hash-object`: Create a new blob object
- `./run.sh cat-file`: Retrieve contents of a blob object
- `./run.sh write-tree`: Create a new tree object from staged files
- `./run.sh ls-tree`: List contents of a tree object
- `./run.sh commit-tree`: Create a new commit object

### Branch Operations

- `./run.sh branch`: List, create, or delete branches
- `./run.sh switch`: Change the current branch

### Comparison

- `./run.sh diff`: Show changes between commits

## Global Options

- `-h, --help`: Show help message
- `-V, --version`: Print version information

## Example Workflow

```bash
# Initialize a new repository
./run.sh init

# Add files to the staging area
./run.sh add myfile.txt

# Commit changes
./run.sh commit -m "Initial commit"

# View commit history
./run.sh log

# Switch branches
./run.sh switch new-feature
```

## Create .vcsignore on the working directory

- Add files or patterns the way you would on git to ignore them

## Troubleshooting

- Ensure Java and Maven are correctly installed
- Verify the path to the JAR file in `run.sh`
- Check permissions of the `run.sh` script

-
-
