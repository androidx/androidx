# Getting started

[TOC]

This page describes how to set up your workstation to check out source code,
make simple changes in Android Studio, and upload commits to Gerrit for review.

This page does **not** cover best practices for the content of changes. Please
see [Life of a Jetpack Feature](/docs/loaf.md) for details on
creating and releasing a library or
[API Guidelines](/docs/api_guidelines/index.md) for best
practices regarding library development.

## Workstation setup {#setup}

This section will help you install the `repo` tool, which is used for Git branch
and commit management. If you want to learn more about `repo`, see the
[Repo Command Reference](https://source.android.com/setup/develop/repo).

NOTE The `repo` tool uses Git submodules under the hood, and it is possible to
skip using the tool in favor of using submodules directly. If you prefer to use
submodules, look for notes anywhere that `repo` is mentioned in this document.
Submodule users can skip Workstation setup.

### Linux and MacOS {#setup-linux-mac}

#### Git rename limit {#setup-linux-mac-rename-limit}

To ensure `git` can detect diffs and renames across significant changes (namely,
the `androidx.*` package rename), we recommend that you set the following `git
config` properties:

```shell
git config --global merge.renameLimit 999999
git config --global diff.renameLimit 999999
```

#### Repo {#setup-linux-mac-repo}

First, download `repo` using `curl`.

```shell
test -d ~/bin || mkdir ~/bin
curl https://storage.googleapis.com/git-repo-downloads/repo \
    > ~/bin/repo && chmod 700 ~/bin/repo
```

Then, modify `~/.zshrc` (or `~/.bash_profile` if using `bash`) to ensure you can
find local binaries from the command line. We assume you're using `zsh`, but the
following should work with `bash` as well.

```shell
export PATH=~/bin:$PATH
```

> NOTE: When using quotes (`"~/bin"`), `~` does not expand and the path is
> invalid. (Possibly `bash` only?)

Next, if your machine has multiple versions of Python installed then you will
need to add the following lines to `~/.zshrc` (or `~/.bash_profile` if using
`bash`) to force the `repo` command to run with `python3`:

```shell
# Force repo to run with Python3
function repo() {
  command python3 ~/bin/repo $@
}
```

Finally, you will need to either start a new terminal session or run `source
~/.zshrc` (or `source ~/.bash_profile` if using `bash`) to enable the changes.

> NOTE: If you encounter the following warning about Python 2 being no longer
> supported, you will need to install Python 3 from the
> [official website](https://www.python.org).
>
> ```shell {.bad}
> repo: warning: Python 2 is no longer supported; Please upgrade to Python 3.6+.
> ```

> NOTE: If you encounter an SSL `CERTIFICATE_VERIFY_FAILED` error:
>
> ```shell {.bad}
> Downloading Repo source from https://gerrit.googlesource.com/git-repo
> fatal: Cannot get https://gerrit.googlesource.com/git-repo/clone.bundle
> fatal: error [SSL: CERTIFICATE_VERIFY_FAILED] certificate verify failed: unable to get local issuer certificate (\_ssl.c:997)
> ```
>
> Run the `Install Certificates.command` in the Python folder of Application
> (e.g. `/Applications/Python\ 3.11/Install\ Certificates.command`). You may
> also need to install `certifi` via `python3 -m pip install certifi` to run
> this command. For more information about SSL/TLS certificate validation, you
> can read the "Important Information" displayed during Python installation.

### Windows {#setup-win}

Sorry, Windows is not a supported platform for AndroidX development.

## Set up access control {#access}

### Authenticate to AOSP Gerrit {#access-gerrit}

Before you can upload changes, you will need to associate your Google
credentials with the AOSP Gerrit code review system by signing in to
[android-review.googlesource.com](https://android-review.googlesource.com) at
least once using the account you will use to submit patches.

Next, you will need to
[set up authentication](https://android.googlesource.com/new-password).
This will give you a shell command to update your local Git cookies, which will
allow you to upload changes.

Finally, you will need to accept the
[CLA for new contributors](https://android-review.googlesource.com/settings/new-agreement).

## Check out the source {#source}

Like ChromeOS, Chromium, and the Android OS, we develop in the open as much as
possible. All feature development occurs in the public
[`androidx-main`](https://android.googlesource.com/platform/superproject/+/refs/heads/androidx-main)
`repo` branch of the Android Open Source Project, with majority of the code in
the
[`frameworks/support` git repository](https://android.googlesource.com/platform/frameworks/support/+/androidx-main).

As of 2025-08-18, you will need about 60 GB for a clean checkout or 160 GB for a
fully-built checkout with history.

### Synchronize the branch {#source-checkout}

Use the following commands to check out your branch.

#### Public main development branch {#androidx-main}

All development should occur in this branch unless otherwise specified by the
AndroidX Core team.

The following command will check out the public main development branch:

```shell
mkdir androidx-main && cd androidx-main
repo init -u https://android.googlesource.com/platform/manifest \
    -b androidx-main --partial-clone --clone-filter=blob:limit=10M
repo sync -c -j32
```

NOTE On MacOS, if you receive an SSL error like `SSL: CERTIFICATE_VERIFY_FAILED`
you may need to install Python3 and boot strap the SSL certificates in the
included version of pip. You can execute `Install Certificates.command` under
`/Applications/Python 3.6/` to do so.

NOTE On MacOS, if you receive a Repo or GPG error like `repo: error: "gpg"
failed with exit status -6` with cause `md_enable: algorithm 10 not available`
you may need to install a build of `gpg` that supports SHA512, such as the
latest version available from [Homebrew](https://brew.sh/) using `brew install
gpg`.

### To check out older sources, use the superproject {#source-historical}

The
[git superproject](https://android.googlesource.com/platform/superproject/+/androidx-main)
contains a history of the matching exact commits of each git repository over
time, and it can be
[checked out directly via git](https://stackoverflow.com/questions/3796927/how-to-git-clone-including-submodules)

### Troubleshooting

> NOTE: If the repo manifest changes -- for example when we update the version
> of `platform-tools` by pointing it to a different git project -- you may see
> the following error during`repo sync`:
>
> ```shell
> error.GitError: Cannot fetch --force-sync not enabled; cannot overwrite a local work tree.
> ...
> error: Unable to fully sync the tree.
> error: Downloading network changes failed.
> ```
>
> This indicates that Studio or some other process has made changes in the git
> project that has been replaced or removed. You can force `repo sync` to
> discard these changes and check out the correct git project by adding the
> `--force-sync` argument:
>
> ```shell
> repo sync -j32 --force-sync
> ```

## Explore source code from a browser {#code-search}

`androidx-main` has a publicly-accessible
[code search](https://cs.android.com/androidx/platform/frameworks/support) that
allows you to explore all of the source code in the repository. Links to this
URL may be shared on the public issue tracked and other external sites.

### Custom search engine for `androidx-main` {#custom-search-engine}

We recommend setting up a custom search engine in Chrome as a faster (and
publicly-accessible) alternative to `cs/`.

1.  Open `chrome://settings/searchEngines`
1.  Click the `Add` button
1.  Enter a name for your search engine, ex. "AndroidX Code Search"
1.  Enter a keyword, ex. "csa"
1.  Enter the following URL:
    `https://cs.android.com/search?q=%s&ss=androidx%2Fplatform%2Fframeworks%2Fsupport`
1.  Click the `Add` button

Now you can select the Chrome omnibox, type in `csa` and press tab, then enter a
query to search for, e.g. `AppCompatButton file:appcompat`, and press the
`Enter` key to get to the search result page.

## Develop in Android Studio {#studio}

Library development uses a curated version of Android Studio to ensure
compatibility between various components of the development workflow.

From the `frameworks/support` directory, you can use

```shell
./studiow :core:,:work:
```

where the argument is comma separated list of project prefixes for projects you
want to work on. This will automatically download and run the correct version of
Studio to work on the selected libraries.

If you want to open all projects, you can run

```shell
./studiow all
```

Next, open the `framework/support` project root from your checkout. If Studio
asks you which SDK you would like to use, select `Use project SDK`. Importing
projects may take a while, but once that finishes you can use Studio as you
normally would for application or library development -- right-click on a test
or sample to run or debug it, search through classes, and so on.

> NOTE: You should choose "Use project SDK" when prompted by Studio. If you
> picked "Android Studio SDK" by mistake, don't panic! You can fix this by
> opening `File > Project Structure > Platform Settings > SDKs` and manually
> setting the Android SDK home path to
> `<project-root>/prebuilts/fullsdk-<platform>`.

### Troubleshooting {#studio-troubleshooting}

*   If you've updated to macOS Ventura and receive a "xcrun: error: invalid
    active developer path" message when running Studio, reinstall Xcode using
    `xcode-select --install`. If that does not work, you will need to download
    Xcode.
*   If you get a “Unregistered VCS root detected” message, click “Add root” to
    enable the Git/VCS integration for Android Studio.
*   If you see any errors (red underlines), click Gradle's elephant button in
    the toolbar (or `File > Sync Project with Gradle Files`) and they should
    resolve once the build completes.
*   If you run `./studiow` with a new project set but you're still seeing the
    old project set in `Project`, use `File > Sync Project with Gradle Files` to
    force a re-sync.
*   If you still see errors after gradle sync, run `repo status` to check for
    any files listed in a "deleted" status. If there are deleted files, navigate
    to each directory containing these files and run `git reset --hard` on each
    of the directories of the deleted files.
*   If Android Studio's UI looks scaled up, ex. twice the size it should be, you
    may need to add the following line to your `studio64.vmoptions` file using
    `Help > Edit Custom VM Options`: `-Dsun.java2d.uiScale.enabled=false`
*   If you don't see a specific Gradle task listed in Studio's Gradle pane,
    check the following:
    *   Studio might be running a different project subset than the one
        intended. For example, `./studiow main` only loads the `main` set of
        androidx projects; run `./studiow compose` to load the tasks specific to
        Compose.
    *   Gradle tasks aren't being loaded. Under Studio's settings =>
        Experimental, make sure that "Do not build Gradle task list during
        Gradle sync" is unchecked. Note that unchecking this can reduce Studio's
        performance.

If in the future you encounter unexpected errors in Studio and you want to check
for the possibility it is due to some incorrect settings or other generated
files, you can run `./studiow --clean main <project subset>` or `./studiow
--reinstall <project subset>` to clean generated files or reinstall Studio.

### Enabling Compose `@Preview` annotation previews

Add the following dependencies to your project's `build.gradle`:

```groovy
dependencies {
    implementation(project(":compose:ui:ui-tooling-preview"))
    debugImplementation(project(":compose:ui:ui-tooling"))
}
```

Then,
[use it like you would on an external project](https://developer.android.com/jetpack/compose/tooling).

## Making changes {#changes}

Similar to Android framework development, library development should occur in
CL-specific working branches. Use `repo` to create, upload, and abandon local
branches. Use `git` to manage changes within a local branch.

```shell
cd path/to/checkout/frameworks/support/
repo start my_branch_name .
# make necessary code changes
# use git to commit changes
repo upload --cbr -t .
```

The `--cbr` switch automatically picks the current repo branch for upload. The
`-t` switch sets the Gerrit topic to the branch name, e.g. `my-branch-name`. You
can refer to the
[Android documentation](https://source.android.com/setup/create/coding-tasks#workflow)
for a high level overview of this basic workflow.

If you see the following prompt, choose `always`:

```
Run hook scripts from https://android.googlesource.com/platform/manifest (yes/always/NO)?
```

If the upload succeeds, you'll see an output like:

```
remote:
remote: New Changes:
remote:   https://android-review.googlesource.com/c/platform/frameworks/support/+/720062 Further README updates
remote:
```

To edit your change, use `git commit --amend`, and re-upload.

NOTE If you encounter issues with `repo upload`, consider running upload with
trace enabled, e.g. `GIT_DAPPER_TRACE=1 repo --trace upload . --cbr -y`. These
logs can be helpful for reporting issues to the team that manages our git
servers.

NOTE If `repo upload` or any `git` command hangs and causes your CPU usage to
skyrocket (e.g. your laptop fan sounds like a jet engine), then you may be
hitting a rare issue with Git-on-Borg and HTTP/2. You can force `git` and `repo`
to use HTTP/1.1 with `git config --global http.version HTTP/1.1`.

### Fixing Kotlin code style errors

`repo upload` automatically runs `ktfmt`, which will cause the upload to fail if
your code has style errors, which it reports on the command line like so:

```
[FAILED] ktfmt_hook
    [path]/MessageListAdapter.kt:36:69: Missing newline before ")"
```

To find and fix these errors, you can run ktfmt locally, either in a console
window or in the Terminal tool in Android Studio. Running in the Terminal tool
is preferable because it will surface links to your source files/lines so you
can easily navigate to the code to fix any problems.

First, to run the tool and see all of the errors, run:

`./gradlew module:submodule:ktCheck`

where module/submodule are the names used to refer to the module you want to
check, such as `navigation:navigation-common`. You can also run ktfmt on the
entire project, but that takes longer as it is checking all active modules in
your project.

Many of the errors that ktfmt finds can be automatically fixed by running
ktFormat:

`./gradlew module:submodule:ktFormat`

ktFormat will report any remaining errors, but you can also run `ktCheck` again
at any time to see an updated list of the remaining errors.

## Building {#building}

Gradle `:tasks` command allows you to find all the useful tasks for a given
project. For example, the following command will let you find tasks available
for `:core:core` project:

```bash
./gradlew :core:core:tasks
```

### Modules and Maven artifacts {#modules-and-maven-artifacts}

To build a specific module, use the module's `assemble` Gradle task. For
example, if you are working on `core` module use:

```shell
./gradlew core:core:assemble
```

To make warnings fail your build (same as presubmit), use the `--strict` flag,
which our gradlew expands into a few correctness-related flags including
`-Pandroidx.validateNoUnrecognizedMessages`:

```shell
./gradlew core:core:assemble --strict
```

To generate a local Maven artifact for the specific module and place it in
`out/repository`, use the `publish` Gradle task:

```shell
./gradlew core:core:publish
```

To build every module and generate the local Maven repository artifacts and
place them in `out/repository`, use the `createArchive` Gradle task:

```shell
./gradlew createArchive
```

To run the complete build task that our build servers use, use the corresponding
shell script:

```shell
./busytown/androidx.sh
```

### Attaching a debugger to the build

Gradle tasks, including building a module, may be run or debugged from within
Android Studio. To start, you need to add the task as a run configuration: you
can do this manually by adding the corresponding task by clicking on the run
configuration dropdown, pressing
[`Edit Configurations`](https://www.jetbrains.com/help/idea/run-debug-gradle.html),
and adding the corresponding task.

You can also run the task through the IDE from the terminal, by using the
[`Run highlighted command using IDE`](https://blog.jetbrains.com/idea/2020/07/run-ide-features-from-the-terminal/)
feature - type in the task you want to run in the in-IDE terminal, and
`ctrl+enter` / `cmd+enter` to launch this through the IDE. This will
automatically add the configuration to the run configuration menu - you can then
cancel the task.

Once the task has been added to the run configuration menu, you can start
debugging as with any other task by pressing the `debug` button.

Note that debugging will not be available until Gradle sync has completed.

#### From the command line

Tasks may also be debugged from the command line, which may be useful if
`./studiow` cannot run due to a Gradle task configuration issue.

1.  From the Run dropdown in Studio, select "Edit Configurations".
1.  Click the plus in the top left to create a new "Remote" configuration. Give
    it a name and hit "Ok".
1.  Set breakpoints.
1.  Run your task with added flags: `./gradlew <your_task_here>
    -Dorg.gradle.debug=true --no-daemon`
1.  Hit the "Debug" button to the right of the configuration dropdown to attach
    to the process.

#### Troubleshooting the debugger

If you get a "Connection refused" error, it's likely because a gradle daemon is
still running on the port specified in the config, and you can fix this by
killing the running gradle daemons:

```shell
./gradlew --stop
```

NOTE This is described in more detail in this
[Medium article](https://medium.com/grandcentrix/how-to-debug-gradle-plugins-with-intellij-eef2ef681a7b).

#### Attaching to an annotation processor

Annotation processors run as part of the build, to debug them is similar to
debugging the build.

For a Java project:

```shell
./gradlew <your_project>:compileDebugJava --no-daemon --rerun-tasks -Dorg.gradle.debug=true
```

For a Kotlin project:

```shell
./gradlew <your_project>:compileDebugKotlin --no-daemon --rerun-tasks -Dorg.gradle.debug=true -Dkotlin.compiler.execution.strategy="in-process" -Dkotlin.daemon.jvm.options="-Xdebug,-Xrunjdwp:transport=dt_socket\,address=5005\,server=y\,suspend=n"
```

### Optional: Enabling internal menu in IntelliJ/Studio

To enable tools such as `PSI tree` inside of IntelliJ/Studio to help debug
Android Lint checks and Metalava, you can enable the
[internal menu](https://www.jetbrains.org/intellij/sdk/docs/reference_guide/internal_actions/enabling_internal.html)
which is typically used for plugin and IDE development.

### Reference documentation {#docs}

See [Reference documentation](reference_documentation.md) for information on
building API reference documentation.

### Updating public APIs {#updating-public-apis}

Public API tasks -- including tracking, linting, and verifying compatibility --
are run under the following conditions based on the `androidx` configuration
block, evaluated in order:

*   `runApiTasks=Yes` => yes
*   `runApiTasks=No` => no
*   `toolingProject=true` => no
*   `mavenVersion` or group version not set => no
*   Has an existing `api/` directory => yes
*   `publish=SNAPSHOT_AND_RELEASE` => yes
*   Otherwise, no

If you make changes to tracked public APIs, you will need to acknowledge the
changes by updating the `<component>/api/current.txt` and associated API files.
This is handled automatically by the `updateApi` Gradle task:

```shell
# Run updateApi for all modules.
./gradlew updateApi

# Run updateApi for a single module, ex. appcompat-resources in group appcompat.
./gradlew :appcompat:appcompat-resources:updateApi
```

If you change the public APIs without updating the API file, your module will
still build **but** your CL will fail Treehugger presubmit checks.

NOTE The `updateApi` task does not generate versioned API files (e.g.
`1.0.0-beta01.txt`) during a library's `alpha`, `rc` or stable cycles. The task
will always generate `current.txt` API files.

#### What are all these files in `api/`? {#updating-public-apis-glossary}

Historical API surfaces are tracked for compatibility and docs generation
purposes. For each version -- including `current` to represent the tip-of-tree
version -- we record three different types of API surfaces.

*   `<version>.txt`: Public API surface, tracked for compatibility
*   `restricted_<version>.txt`: `@RestrictTo` API surface, tracked for
    compatibility where necessary (see
    [Restricted APIs](/docs/api_guidelines/index.md#restricted-api))
*   `public_plus_experimental_<version>.txt`: Public API surface plus
    `@RequiresOptIn` experimental API surfaces used for documentation (see
    [Experimental APIs](/docs/api_guidelines/index.md#experimental-api))
    and API review

NOTE: Experimental API tracking for KLib is enabled by default for KMP projects
via parallel `updateAbi` and `checkAbi` tasks. If you have a problem with these
tools,
[please file an issue](https://issuetracker.google.com/issues/new?component=1102332&template=1780493).
As a workaround, you may opt-out by setting
`enableBinaryCompatibilityValidator = false` under
`AndroidxMultiplatformExtension` in your library's `build.gradle` file.

## Testing {#testing}

AndroidX libraries are expected to include unit or integration test coverage for
100% of their public API surface. Additionally, all CLs must include a `Test:`
stanza indicating which tests were used to verify correctness. Any CLs
implementing bug fixes are expected to include new regression tests specific to
the issue being fixed.

### Running tests {#run-tests}

Generally, tests in the AndroidX repository should be run through the Android
Studio UI. You can also run tests from the command line or via remote devices on
FTL, see
[Running unit and integration tests](/docs/testing.md#running)
for details.

#### Single test class or method

1.  Open the desired test file in Android Studio
2.  Right-click on a test class or `@Test` method name and select `Run <name>`

#### Full test package

1.  In the `Project` side panel, open the desired module
2.  Find the package directory with the tests
3.  Right-click on the directory and select `Run <package>`

### Running sample apps {#run-samples}

The AndroidX repository has a set of Android applications that exercise AndroidX
code. These applications can be useful when you want to debug a real running
application, or reproduce a problem interactively, before writing test code.

These applications are named either `<libraryname>-integration-tests-testapp`,
or `support-\*-demos` (e.g. `support-v4-demos` or `support-leanback-demos`). You
can run them by clicking `Run > Run ...` and choosing the desired application.

See the [Testing](/docs/testing.md) page for more resources on
writing, running, and monitoring tests.

### AVD Manager

The Android Studio instance started by `./studiow` uses a custom SDK directory,
which means any virtual devices created by a "standard" non-AndroidX instance of
Android Studio will be *visible* from the `./studiow` instance but will be
unable to locate the SDK artifacts -- they will display a `Download` button.

You can either use the `Download` button to download an extra copy of the SDK
artifacts *or* you can set up a symlink to your "standard" non-AndroidX SDK
directory to expose your existing artifacts to the `./studiow` instance:

```shell
# Using the default MacOS Android SDK directory...
ln -s /Users/$(whoami)/Library/Android/sdk/system-images \
      ../../prebuilts/fullsdk-darwin/system-images
```

## Library snapshots {#snapshots}

See [Library snapshots](library_snapshots.md) for information on using snapshot
builds.

## FAQ {#faq}

See [FAQ](faq.md) for answers to frequently asked questions.
