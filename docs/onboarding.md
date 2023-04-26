# Getting started

[TOC]

This page describes how to set up your workstation to check out source code,
make simple changes in Android Studio, and upload commits to Gerrit for review.

This page does **not** cover best practices for the content of changes. Please
see [Life of a Jetpack Feature](/company/teams/androidx/loaf.md) for details on
developing and releasing a library,
[API Guidelines](/company/teams/androidx/api_guidelines/index.md) for best
practices regarding public APIs and an overview of the constraints placed on
changes.

## Workstation setup {#setup}

This section will help you install the `repo` tool, which is used for Git branch
and commit management. If you want to learn more about `repo`, see the
[Repo Command Reference](https://source.android.com/setup/develop/repo).

### Linux and MacOS {#setup-linux-mac}

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

Next, add the following lines to `~/.zshrc` (or `~/.bash_profile` if using
`bash`) aliasing the `repo` command to run with `python3`:

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
> Run the `Install Certificates.command` in the Python folder of Application.
> For more information about SSL/TLS certificate validation, you can read the
> "Important Information" displayed during Python installation.

### Windows {#setup-win}

Sorry, Windows is not a supported platform for AndroidX development.

## Set up access control {#access}

### Authenticate to AOSP Gerrit {#access-gerrit}

Before you can upload changes, you will need to associate your Google
credentials with the AOSP Gerrit code review system by signing in to
[android-review.googlesource.com](https://android-review.googlesource.com) at
least once using the account you will use to submit patches.

Next, you will need to
[set up authentication](https://android-review.googlesource.com/new-password).
This will give you a shell command to update your local Git cookies, which will
allow you to upload changes.

Finally, you will need to accept the
[CLA for new contributors](https://android-review.googlesource.com/settings/new-agreement).

## Check out the source {#source}

Like ChromeOS, Chromium, and the Android build system, we develop in the open as
much as possible. All feature development occurs in the public
[androidx-main](https://android.googlesource.com/platform/frameworks/support/+/androidx-main)
branch of the Android Open Source Project.

As of 2023/03/30, you will need about 42 GB for a fully-built checkout.

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
repo sync -c -j8
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

### Increase Git rename limit {#source-config}

To ensure `git` can detect diffs and renames across significant changes (namely,
the `androidx.*` package rename), we recommend that you set the following `git
config` properties:

```shell
git config --global merge.renameLimit 999999
git config --global diff.renameLimit 999999
```

### Set up Git file exclusions {#source-exclude}

Mac users should consider adding `.DS_Store` to a global `.gitignore` file to
avoid accidentally checking in local metadata files:

```shell
echo .DS_Store>>~/.gitignore
git config --global core.excludesFile '~/.gitignore'
```

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

From the `frameworks/support` directory, you can use `./studiow m` (short for
`ANDROIDX_PROJECTS=main ./gradlew studio`) to automatically download and run the
correct version of Studio to work on the `main` set of androidx projects
(non-Compose Jetpack libraries).
[studiow](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:studiow)
also supports several other arguments like `all` for other subsets of the
projects (run `./studiow` for help).

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

*   If you've updated to macOS Ventura and receive a "App is damaged and cannot
    be opened" message when running Studio, *do not* move the app to the Trash.
    Cancel out of the dialog and open macOS `System Settings > Gatekeeper`, look
    for `"Android Studio" was blocked`, and click `Open Anyway` to grant an
    exception. Alternatively, you can navigate to the Studio `.app` file under
    `frameworks/support/studio` and run it once using `Control+Click` and `Open`
    to automatically grant an exception.
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

`repo upload` automatically runs `ktlint`, which will cause the upload to fail
if your code has style errors, which it reports on the command line like so:

```
[FAILED] ktlint_hook
    [path]/MessageListAdapter.kt:36:69: Missing newline before ")"
```

To find and fix these errors, you can run ktlint locally, either in a console
window or in the Terminal tool in Android Studio. Running in the Terminal tool
is preferable because it will surface links to your source files/lines so you
can easily navigate to the code to fix any problems.

First, to run the tool and see all of the errors, run:

`./gradlew module:submodule:ktlint`

where module/submodule are the names used to refer to the module you want to
check, such as `navigation:navigation-common`. You can also run ktlint on the
entire project, but that takes longer as it is checking all active modules in
your project.

Many of the errors that ktlint finds can be automatically fixed by running
ktlintFormat:

`./gradlew module:submodule:ktlintFormat`

ktlintFormat will report any remaining errors, but you can also run `ktlint`
again at any time to see an updated list of the remaining errors.

## Building {#building}

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

To build every module and generate the local Maven repository artifact, use the
`createArchive` Gradle task:

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

Our reference docs (Javadocs and KotlinDocs) are published to
https://developer.android.com/reference/androidx/packages and may be built
locally.

NOTE `./gradlew tasks` always has the canonical task information! When in doubt,
run `./gradlew tasks`

#### Generate Docs

To build API reference docs for both Java and Kotlin source code using Dackka,
run the Gradle task:

```
./gradlew docs
```

Location of generated refdocs:

*   docs-public (what is published to DAC):
    `{androidx-main}/out/androidx/docs-public/build/docs`
*   docs-tip-of-tree: `{androidx-main}/out/androidx/docs-tip-of-tree/build/docs`

The generated docs are plain HTML pages with links that do not work locally.
These issues are fixed when the docs are published to DAC, but to preview a
local version of the docs with functioning links and CSS, run:

```
python3 development/offlinifyDocs/offlinify_dackka_docs.py
```

You will need to have the `bs4` Python package installed. The CSS used is not
the same as what will be used when the docs are published.

By default, this command converts the tip-of-tree docs for all libraries. To see
more options, run:

```
python3 development/offlinifyDocs/offlinify_dackka_docs.py --help
```

#### Release docs

To build API reference docs for published artifacts formatted for use on
[d.android.com](http://d.android.com), run the Gradle command:

```
./gradlew zipDocs
```

This will create the artifact `{androidx-main}/out/dist/docs-public-0.zip`. This
command builds docs based on the version specified in
`{androidx-main-checkout}/frameworks/support/docs-public/build.gradle` and uses
the prebuilt checked into
`{androidx-main-checkout}/prebuilts/androidx/internal/androidx/`. We
colloquially refer to this two step process of (1) updating `docs-public` and
(2) checking in a prebuilt artifact into the prebuilts directory as
[The Prebuilts Dance](/company/teams/androidx/releasing_detailed.md#the-prebuilts-dance™).
So, to build javadocs that will be published to
https://developer.android.com/reference/androidx/packages, both of these steps
need to be completed.

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
    [Restricted APIs](/company/teams/androidx/api_guidelines/index.md#restricted-api))
*   `public_plus_experimental_<version>.txt`: Public API surface plus
    `@RequiresOptIn` experimental API surfaces used for documentation (see
    [Experimental APIs](/company/teams/androidx/api_guidelines/index.md#experimental-api))
    and API review

### Release notes & the `Relnote:` tag {#relnote}

Prior to releasing, release notes are pre-populated using a script and placed
into a Google Doc. The Google Doc is manually double checked by library owners
before the release goes live. To auto-populate your release notes, you can use
the semi-optional commit tag `Relnote:` in your commit, which will automatically
include that message the commit in the pre-populated release notes.

The presence of a `Relnote:` tag is required for API changes in `androidx-main`.

#### How to use it?

One-line release note:

``` {.good}
Relnote: Fixed a critical bug
```

``` {.good}
Relnote: "Fixed a critical bug"
```

``` {.good}
Relnote: Added the following string function: `myFoo(\"bar\")`
```

Multi-line release note:

Note: If the following lines do not contain an indent, you may hit b/165570183.

``` {.good}
Relnote: "We're launching this awesome new feature!  It solves a whole list of
    problems that require a lot of explaining! "
```

``` {.good}
Relnote: """Added the following string function: `myFoo("bar")`
    It will fix cases where you have to call `myFoo("baz").myBar("bar")`
    """
```

Opt out of the Relnote tag:

``` {.good}
Relnote: N/A
```

``` {.good}
Relnote: NA
```

NOT VALID:

``` {.bad}
Relnote: This is an INVALID multi-line release note.  Our current scripts won't
include anything beyond the first line.  The script has no way of knowing when
the release note actually stops.
```

``` {.bad}
Relnote: This is an INVALID multi-line release note.  "Quotes" need to be
  escaped in order for them to be parsed properly.
```

### Common build errors

#### Diagnosing build failures

If you've encountered a build failure and you're not sure what is triggering it,
then please run
`./development/diagnose-build-failure/diagnose-build-failure.sh`.

This script can categorize your build failure into one of the following
categories:

*   The Gradle Daemon is saving state in memory and triggering a failure
*   Your source files have been changed and/or incompatible git commits have
    been checked out
*   Some file in the out/ dir is triggering an error
    *   If this happens, diagnose-build-failure.sh should also identify which
        file(s) specifically
*   The build is nondeterministic and/or affected by timestamps
*   The build via gradlew actually passes and this build failure is specific to
    Android Studio

Some more-specific build failures are listed below in this page.

#### Out-of-date platform prebuilts

Like a normal Android library developed in Android Studio, libraries within
`androidx` are built against prebuilts of the platform SDK. These are checked in
to the `prebuilts/fullsdk-darwin/platforms/<android-version>` directory.

If you are developing against pre-release platform APIs in the internal
`androidx-platform-dev` branch, you may need to update these prebuilts to obtain
the latest API changes.

#### Missing external dependency

If Gradle cannot resolve a dependency listed in your `build.gradle`:

*   You will probably want to import the missing artifact via
    [importMaven.sh](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/importMaven/README.md)

    *   We store artifacts in the prebuilts repositories under
        `prebuilts/androidx` to facilitate reproducible builds even if remote
        artifacts are changed.

*   You may need to [establish trust for](#dependency-verification) the new
    artifact

##### Importing dependencies in `libs.versions.toml`

Libraries typically reference dependencies using constants defined in
[`libs.versions.toml`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:gradle/libs.versions.toml).
Update this file to include a constant for the version of the library that you
want to depend on. You will reference this constant in your library's
`build.gradle` dependencies.

**After** you update the `libs.versions.toml` file with new dependencies, you
can download them by running:

```shell
cd frameworks/support &&\
development/importMaven/importMaven.sh import-toml
```

This command will resolve everything declared in the `libs.versions.toml` file
and download missing artifacts into `prebuilts/androidx/external` or
`prebuilts/androidx/internal`.

Make sure to upload these changes before or concurrently (ex. in the same Gerrit
topic) with the dependent library code.

##### Downloading a dependency without changing `libs.versions.toml`

You can also download a dependency without changing `libs.versions.toml` file by
directly invoking:

```shell
cd frameworks/support &&\
./development/importMaven/importMaven.sh someGroupId:someArtifactId:someVersion
```

##### Missing konan dependencies

Kotlin Multiplatform projects need prebuilts to compile native code, which are
located under `prebuilts/androidx/konan`. **After** you update the kotlin
version of AndroidX, you should also download necessary prebuilts via:

```shell
cd frameworks/support &&\
development/importMaven/importMaven.sh import-konan-binaries --konan-compiler-version <new-kotlin-version>
```

Please remember to commit changes in the `prebuilts/androidx/konan` repository.

#### Dependency verification

If you import a new dependency that is either unsigned or is signed with a new,
unrecognized key, then you will need to add new dependency verification metadata
to indicate to Gradle that this new dependency is trusted. See the instructions
[here](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/gradle/README.md)

#### Updating an existing dependency

If an older version of a dependency prebuilt was already checked in, please
manually remove it within the same CL that adds the new prebuilt. You will also
need to update `Dependencies.kt` to reflect the version change.

#### My gradle build fails with "Cannot invoke method getURLs() on null object"

You're using Java 9's javac, possibly because you ran envsetup.sh from the
platform build or specified Java 9 as the global default Java compiler. For the
former, you can simply open a new shell and avoid running envsetup.sh. For the
latter, we recommend you set Java 8 as the default compiler using sudo
update-java-alternatives; however, if you must use Java 9 as the default then
you may alternatively set JAVA_HOME to the location of the Java 8 SDK.

#### My gradle build fails with "error: cannot find symbol" after making framework-dependent changes.

You probably need to update the prebuilt SDK used by the gradle build. If you
are referencing new framework APIs, you will need to wait for the framework
changes to land in an SDK build (or build it yourself) and then land in both
prebuilts/fullsdk and prebuilts/sdk. See
[Updating SDK prebuilts](/company/teams/androidx/playbook.md#prebuilts-fullsdk)
for more information.

#### How do I handle refactoring a framework API referenced from a library?

Because AndroidX must compile against both the current framework and the latest
SDK prebuilt, and because compiling the SDK prebuilt depends on AndroidX, you
will need to refactor in stages:

1.  Remove references to the target APIs from AndroidX
2.  Perform the refactoring in the framework
3.  Update the framework prebuilt SDK to incorporate changes in (2)
4.  Add references to the refactored APIs in AndroidX
5.  Update AndroidX prebuilts to incorporate changes in (4)

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
[Running unit and integration tests](/company/teams/androidx/testing.md#running)
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

See the [Testing](/company/teams/androidx/testing.md) page for more resources on
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

### Quick how-to

Add the following snippet to your build.gradle file, replacing `buildId` with a
snapshot build ID.

```groovy {highlight=context:[buildId]}
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://androidx.dev/snapshots/builds/[buildId]/artifacts/repository' }
    }
}
```

You must define dependencies on artifacts using the `SNAPSHOT` version suffix,
for example:

```groovy {highlight=context:SNAPSHOT}
dependencies {
    implementation "androidx.core:core:1.2.0-SNAPSHOT"
}
```

### Where to find snapshots

If you want to use unreleased `SNAPSHOT` versions of `androidx` artifacts, you
can find them on either our public-facing build server:

`https://ci.android.com/builds/submitted/<build_id>/androidx_snapshot/latest`

or on our slightly-more-convenient [androidx.dev](https://androidx.dev) site:

`https://androidx.dev/snapshots/builds/<build-id>/artifacts` for a specific
build ID

`https://androidx.dev/snapshots/latest/artifacts` for tip-of-tree snapshots

### Obtaining a build ID

To browse build IDs, you can visit either
[androidx-main](https://ci.android.com/builds/branches/aosp-androidx-main/grid?)
on ci.android.com or [Snapshots](https://androidx.dev/snapshots/builds) on the
androidx.dev site.

Note that if you are using androidx.dev, you may substitute `latest` for a build
ID to use the last known good build.

To manually find the last known good `build-id`, you have several options.

#### Snapshots on androidx.dev

[Snapshots](https://androidx.dev/snapshots/builds) on androidx.dev only lists
usable builds.

#### Programmatically via `jq`

Install `jq`:

```shell
sudo apt-get install jq
```

```shell
ID=`curl -s "https://ci.android.com/builds/branches/aosp-androidx-main/status.json" | jq ".targets[] | select(.ID==\"aosp-androidx-main.androidx_snapshot\") | .last_known_good_build"` \
  && echo https://ci.android.com/builds/submitted/"${ID:1:-1}"/androidx_snapshot/latest/raw/repository/
```

#### Android build server

Go to
[androidx-main](https://ci.android.com/builds/branches/aosp-androidx-main/grid?)
on ci.android.com.

For `androidx-snapshot` target, wait for the green "last known good build"
button to load and then click it to follow it to the build artifact URL.

### Using in a Gradle build

To make these artifacts visible to Gradle, you need to add it as a repository:

```groovy
allprojects {
    repositories {
        google()
        maven {
          // For all Jetpack libraries (including Compose)
          url 'https://androidx.dev/snapshots/builds/<build-id>/artifacts/repository'
        }
    }
}
```

Note that the above requires you to know the `build-id` of the snapshots you
want.

#### Specifying dependencies

All artifacts in the snapshot repository are versioned as `x.y.z-SNAPSHOT`. So
to use a snapshot artifact, the version in your `build.gradle` will need to be
updated to `androidx.<groupId>:<artifactId>:X.Y.Z-SNAPSHOT`

For example, to use the `core:core:1.2.0-SNAPSHOT` snapshot, you would add the
following to your `build.gradle`:

```
dependencies {
    ...
    implementation("androidx.core:core:1.2.0-SNAPSHOT")
    ...
}
```

## FAQ {#faq}

### How do I test my change in a separate Android Studio project? {#faq-test-change-studio}

If you're working on a new feature or bug fix in AndroidX, you may want to test
your changes against another project to verify that the change makes sense in a
real-world context or that a bug's specific repro case has been fixed.

If you need to be absolutely sure that your test will exactly emulate the
developer's experience, you can repeatedly build the AndroidX archive and
rebuild your application. In this case, you will need to create a local build of
AndroidX's local Maven repository artifact and install it in your Android SDK
path.

First, use the `createArchive` Gradle task to generate the local Maven
repository artifact:

```shell
# Creates <path-to-checkout>/out/androidx/build/support_repo/
./gradlew createArchive
```

Using for your alternate (non-AndroidX) version of Android Studio open the
project's 'build.gradle' and add the following within 'repositories' to make
Android Gradle Plugin look for binaries in newly built repository:

```groovy
allprojects {
    repositories {
        ...
        maven {
            url "<path-to-sdk>/out/androidx/build/support_repo/"
        }
    }
}
```

NOTE Gradle resolves dependencies in the order that the repositories are defined
(if 2 repositories can resolve the same dependency, the first listed will do so
and the second will not). Therefore, if the library you are testing has the same
group, artifact, and version as one already published, you will want to list
your custom maven repo first.

Finally, in the dependencies section of your standalone project's `build.gradle`
file, add or update the `implementation` entries to reflect the AndroidX modules
that you would like to test. Example:

```
dependencies {
    ...
    implementation "androidx.appcompat:appcompat::1.0.0-alpha02"
}
```

If you are testing your changes in the Android Platform code, you can replace
the module you are testing
`YOUR_ANDROID_PATH/prebuilts/sdk/current/androidx/m2repository` with your own
module. We recommend only replacing the module you are modifying instead of the
full m2repository to avoid version issues of other modules. You can either take
the unzipped directory from
`<path-to-checkout>/out/dist/top-of-tree-m2repository-##.zip`, or from
`<path-to-checkout>/out/androidx/build/support_repo/` after building `androidx`.
Here is an example of replacing the RecyclerView module:

```shell
$TARGET=YOUR_ANDROID_PATH/prebuilts/sdk/current/androidx/m2repository/androidx/recyclerview/recyclerview/1.1.0-alpha07;
rm -rf $TARGET;
cp -a <path-to-sdk>/extras/m2repository/androidx/recyclerview/recyclerview/1.1.0-alpha07 $TARGET
```

Make sure the library versions are the same before and after replacement. Then
you can build the Android platform code with the new `androidx` code.

### How do I measure library size? {#library-size}

Method count and bytecode size are tracked in CI
[alongside benchmarks](/company/teams/androidx/benchmarking.md#monitoring) to
detect regressions.

For local measurements, use the `:reportLibraryMetrics` task. For example:

```shell
./gradlew benchmark:benchmark-macro:reportLibraryMetrics
cat ../../out/dist/librarymetrics/androidx.benchmark_benchmark-macro.json
```

Will output something like: `{"method_count":1256,"bytecode_size":178822}`

Note: this only counts the weight of your library's jar/aar, including
resources. It does not count library dependencies. It does not account for a
minification step (e.g. with R8), as that is dynamic, and done at app build time
(and depend on which entrypoints the app uses).

### How do I add content to a library's Overview reference doc page?

Put content in a markdown file that ends with `-documentation.md` in the
directory that corresponds to the Overview page that you'd like to document.

For example, the `androidx.compose.runtime`
[Overview page](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary)
includes content from
[compose-runtime-documentation.md](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/runtime/runtime/src/commonMain/kotlin/androidx/compose/runtime/compose-runtime-documentation.md).

### How do I enable MultiDex for my library?

Go to your project/app level build.gradle file, and add

```
android {
    defaultConfig {
        multiDexEnabled = true
    }
}
```

as well as `androidTestImplementation(libs.multidex)` to the dependenices block.

If you want it enabled for the application and not test apk, add
`implementation(libs.multidex)` to the dependencies block instead. Any prior
failures may not re-occur now that the software is multi-dexed. Rerun the build.
