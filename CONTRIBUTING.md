## Introduction

This repo is an official mirror of Android Jetpack libraries that enables external contributions to a select number of libraries via GitHub pull requests.

### Why this repository exists

The Android team has been exploring how we could make it easier to develop libraries that don’t rely on infrastructure from the Android Open Source Project (AOSP). This GitHub infrastructure has two benefits. First, it makes it easier to contribute to a small set of Jetpack libraries. Second, this parallel infrastructure makes it possible to build and test Jetpack libraries against non-Android target platforms.

### What can you contribute to?

You can start contributing to any of the following library groups from GitHub:
  - [Activity](https://developer.android.com/guide/components/activities/intro-activities)
  - [AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat)
  - [Biometric](https://developer.android.com/training/sign-in/biometric-auth)
  - [Collection](https://developer.android.com/jetpack/androidx/releases/collection)
  - [Compose Runtime](https://developer.android.com/jetpack/androidx/releases/compose-runtime)
  - [Core](https://developer.android.com/jetpack/androidx/releases/core)
  - [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
  - [Fragment](https://developer.android.com/guide/components/fragments)
  - [Lifecycle](https://developer.android.com/topic/libraries/architecture/lifecycle)
  - [Navigation](https://developer.android.com/guide/navigation)
  - [Paging](https://developer.android.com/topic/libraries/architecture/paging)
  - [Room](https://developer.android.com/topic/libraries/architecture/room)
  - [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

Not sure where to start? Take a look at the official [feature/bug bounty list](http://goo.gle/androidx-bug-bounty).

Our tooling currently supports **macOS and Linux**. This new setup is a **work-in-progress**, so it might have some rough edges. Please bear with us while we streamline this workflow.

## Getting started

We have tried to make contributing to androidx a lot easier with this new setup. Just start by
creating a fork of the [androidx/androidx](https://github.com/androidx/androidx) GitHub repository.

### One-time setup

- Click on the `Actions` tab in the forked `androidx` repository, and enable the use of `Workflows`.

- Download and install JDK 17, if you don’t have it already.

  Note the installation directory. If you already have JDK 17 installed and set as default, you can
  find this with `which javac`.

- Download and install [Android Studio](https://developer.android.com/studio) if you don't have it
  already. Then, locate your Android SDK directory at `Tools > SDK Manager > Android SDK Location`.

  If you already have the SDK installed or don't plan on using Android Studio, just note the
  SDK directory.

- Download and install the NDK. You can either do this using the command-line `sdkmanager` or from
  Android Studio using `Tools > SDK Manager`, checking `Show Package Details`, and expanding `NDK`.

  ```bash
  sdkmanager --install "ndk;23.1.7779620"
  sdkmanager --install "cmake;3.22.1"
  ```

- Next, set up the following environment variables:

  ```bash
  # You could also add this to your .{bash|zsh}rc file.
  export JAVA_HOME="location of JDK 17 directory"
  export ANDROID_SDK_ROOT="location of the Android SDK directory"
  ```

### Checkout and importing a project

The list of folders that can be contributed to, using the GitHub repository are:

```
androidx
  -- activity
  -- appcompat
  -- biometric
  -- compose/compiler
  -- compose/runtime
  -- core
  -- datastore
  -- fragment
  -- lifecycle
  -- navigation
  -- paging
  -- room
  -- work
```

To avoid conflict with the main project, these sub project groups are located under the
`playground-projects` folder.

**Note:** For other projects, you will still need to use the Gerrit workflow used by the Android Open Source Project (AOSP). For more information, please look at the [README](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:README.md).

Fork the [androidx/androidx](https://github.com/androidx/androidx) repository.

We recommend cloning using blob filter to reduce checkout size:
```bash
git clone https://github.com/YOUR_USERNAME/androidx.git
```

---

> NOTE: If you are low on disk space, then you can use the following command to save some space in your checkout. :point_left:

```bash
# Filters any blob > 10M
git clone --filter=blob:limit=10M https://github.com/YOUR_USERNAME/androidx.git
```
---

Let’s assume that you want to make a contribution to Room. The first step is to launch Android Studio and import the Room project.

First launch Android Studio using:

```bash
cd playground-projects/room-playground
# This will automatically launch the `room` project in Android Studio.
./gradlew studio
```

The studio task automatically downloads the correct version of Android Studio that matches the Android Gradle Plugin version.

### Making changes

You can now start making changes to the Room project. Making changes is just like making changes to any other Android project. It’s a good idea to build consensus on the change you intend to make. Make sure there is a related issue on the [AOSP issue tracker](https://issuetracker.google.com/issues/new?component=192731&template=842428) and start a conversation on that issue to ensure project maintainers are aware of it. It is best to start the conversation as early as possible to avoid duplicating work as other contributors might already be working on it.

### Validating changes locally

Before you send out a pull request, it’s always a good idea to run tests locally to make sure that you have not accidentally introduced bugs. Also, given all AndroidX projects follow semantic versioning, it's also important for projects to not introduce breaking changes without changing the library’s major version.

Apart from this, there are checks that ensure developers follow the Android coding standards & guidelines.

To make sure that your code passes all the above checks & tests you can run:

```bash
# Switch to the `room` directory or the project you are working on.
cd room
# Run device side and host side tests
./gradlew test connectedCheck

# Run additional checks
./gradlew buildOnServer

# If you are testing on an emulator, you can disable benchmark tests as
# follows since they require a real device to run
./gradlew \
    test connectedCheck \
    -x :room:room-benchmark:cC \
    -x :room:integration-tests:room-incremental-annotation-processing:test
```

Once your changes look good, you can push them to your fork of the repository. Now you are ready to make a pull request.

**Note:** When you make changes to an API, you need to run:

```
./gradlew updateApi
```

If you are adding new APIs, then you might **additionally need to update** [LibraryVersions.kt](https://github.com/androidx/androidx/blob/androidx-main/buildSrc/src/main/kotlin/androidx/build/LibraryVersions.kt) as well, before running the updateApi task. This is **relevant when the library’s API is frozen** (betas, rc’s and stable versions). For alpha versions, you don’t have to update this file.

This helps the AndroidX project keep track of API changes and avoid inadvertently adding APIs or introduce backwards incompatible changes.

**Note:** In case you make a valid violation of Lint, you can use `@Suppress("Rule")` in Kotlin, or `@SuppressLint("Rule")` in Java to suppress the rule.

**Note: CI build will already check for these but it is best to run them locally to speedup the process.**

### Making a pull request

To create a pull request click on [this](https://github.com/androidx/androidx/pulls) link and then click on New Pull Request.

Then click on the compare across forks and select your forked repository as the HEAD repository. Then click Create.

All pull requests **must follow** the following conventions.

1.  The pull request includes a short description of the change, and a longer detailed description.
2.  Include a Test stanza in the pull request which describes the steps followed by the developer to test the changes.
3.  Include a Fixes stanza that describes the issue being fixed. That way the corresponding issue trackers can automatically be resolved once the change lands in AOSP.

Here is an example:

```
Short description for the change.

Longer explanation for the change.

Test: A test stanza. For e.g. /gradlew test connectedCheck
Fixes: b/<bugId> if applicable
```

### Pull request workflow

AndroidX is primarily developed in [AOSP](https://android.googlesource.com/platform/frameworks/support/+/androidx-main). This flow simply mirrors pull requests from GitHub into Gerrit, For all intents and purposes, AOSP is the **single** **source of truth**, all changes will be merged in Gerrit and mirrored back to GitHub.

Here is what a typical pull request workflow looks like:

1.  Create a GitHub pull request from **your forked repository** to the androidx-main branch on GitHub.
2.  Sign the Contributor’s License Agreement at https://cla.developers.google.com/ to get @googlebot to give you the `cla: yes` label.
3.  Your PR will be reviewed using the GitHub pull request flow. You can address the comments / suggestions in your forked repository and update the pull request as normal.
4.  Once the changes look good, a Googler will Approve your pull request on GitHub.
5.  Your PR will be **tested using GitHub workflows**. You can monitor these GitHub workflows by using the Actions tab in your forked repository.
6.  Once your **pull request has been approved** by a Googler, it will also be **mirrored to AOSP Gerrit**. You can find the link to Gerrit under the status check, `import/copybara` left by `@copybara-service`, by clicking details.
7.  Once **all** the checks in **Gerrit and GitHub** pass, your change will get merged in androidx-main in AOSP and mirrored back to androidx-main in GitHub. Congratulations, your change landed in AOSP!
8.  Currently, your pull request will not get automatically closed when your changes are merged. So you will have to close the pull request manually. We are working on improving the workflow to address this.

### Running into problems?

- If you see GitHub workflows failing, then look at the verbose logs under the `Actions` tab for more information. If you don’t understand why a test might be failing, then reach out to us by creating a new issue [here](https://issuetracker.google.com/issues/new?component=923725&template=1480355).
