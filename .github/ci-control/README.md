# AndroidX CI Control
This folder includes script(s) and configuration files to control the behavior of the GitHub CI.

[ci-config.json](ci-config.json) file has configurations for each branch group about which projects they
should build. Each configuration group can have `exclude` and `include` filters that will filter
the projects by name from the build matrix in [presubmit.yml](../workflows/presubmit.yml).

This configuration file is read by the [should_run_project](should_run_project.py) to let the build
know whether it should execute or skip a project.

## Disabling a project
If you need to temporarily disable a project in CI (e.g. it needs new prebuilts), you can update the
[ci-config.json](ci-config.json) and add an `exclude` filter for the relevant branch.