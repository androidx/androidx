# Project/Module Creator

This script will create a new project/module using a groupId and artifactId.

It will use the groupId and artifactId to best guess which configuration
is most appropriate for the project/module you are creating.

### Using the script

```bash
./create_project.py androidx.foo foo-bar
```

### Todos **after** project creation

1. [OWNERS] Check that the OWNERS file is in the correct place
2. [OWNERS] Add your name (and others) to the OWNERS file
3. [build.grade] Check that the correct library version is assigned
4. [build.grade] Fill out the project/module name
5. [package-info.java] Fill out the project/module package-info.java file

### Project/Module Types

The script leverages buildSrc/public/src/main/kotlin/androidx/build/LibraryType.kt
to create the recommended defaults for your project.  However, you can override
the options to best fit your requirements.

### Testing the script

Generic project integration test
```bash
./create_project.py androidx.foo.bar bar-qux
```

Script test suite
```bash
./test_project_creator.py
```

### Debugging `No module named 'toml' errors

If you see an error message `No module named 'toml'` try the following steps.

*   Install necessary tools if they are not already installed
    *   (Linux) `sudo apt-get install virtualenv python3-venv`
    *   (Mac) `pip3 install virtualenv`
*   Create a virtual environment with `virtualenv androidx_project_creator` (you
    can choose another name for your virtualenv if you wish).
*   Install the `toml` library in your virtual env with
    `androidx_project_creator/bin/pip3 install toml`
*   Run the project creator script from your virtual env with
    `androidx_project_creator/bin/python3
    ./development/project-creator/create_project.py androidx.foo foo-bar`
*   Delete your virtual env with `rm -rf ./androidx-project_creator`
    *   virtualenv will automatically .gitignore itself, but you may want to to
        remove it anyway.