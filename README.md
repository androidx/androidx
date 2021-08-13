# Android Jetpack

Jetpack is a suite of libraries, tools, and guidance to help developers write high-quality apps easier. These components help you follow best practices, free you from writing boilerplate code, and simplify complex tasks, so you can focus on the code you care about.

Jetpack comprises the `androidx.*` package libraries, unbundled from the platform APIs. This means that it offers backward compatibility and is updated more frequently than the Android platform, making sure you always have access to the latest and greatest versions of the Jetpack components.

Our official AARs and JARs binaries are distributed through [Google Maven](https://maven.google.com).

You can learn more about using it from [Android Jetpack landing page](https://developer.android.com/jetpack).

# Contribution Guide

For contributions via GitHub, see the [GitHub Contribution Guide](CONTRIBUTING.md).

Note: The contributions workflow via GitHub is currently experimental - only contributions to the following projects are being accepted at this time:
* [Activity](activity)
* [Biometric](biometric)
* [Compose Compiler](compose/compiler)
* [Compose Runtime](compose/runtime)
* [DataStore](datastore)
* [Fragment](fragment)
* [Lifecycle](lifecycle)
* [Navigation](navigation)
* [Paging](paging)
* [Room](room)
* [WorkManager](work)

## Code Review Etiquette
When contributing to Jetpack, follow the [code review etiquette](code-review.md).

## Accepted Types of Contributions
* Bug fixes - needs a corresponding bug report in the [Android Issue Tracker](https://issuetracker.google.com/issues/new?component=192731&template=842428)
* Each bug fix is expected to come with tests
* Fixing spelling errors
* Updating documentation
* Adding new tests to the area that is not currently covered by tests
* New features to existing libraries if the feature request bug has been approved by an AndroidX team member.

We **are not** currently accepting new modules.

## Checking Out the Code
**NOTE: You will need to use Linux or Mac OS. Building under Windows is not currently supported.**

1. Install `repo` (Repo is a tool that makes it easier to work with Git in the context of Android. For more information about Repo, see the [Repo Command Reference](https://source.android.com/setup/develop/repo))

```bash
mkdir ~/bin
PATH=~/bin:$PATH
curl https://storage.googleapis.com/git-repo-downloads/repo > ~/bin/repo
chmod a+x ~/bin/repo
```

2. Configure Git with your real name and email address.

```bash
git config --global user.name "Your Name"
git config --global user.email "you@example.com"
```

3. Create a directory for your checkout (it can be any name)

```bash
mkdir androidx-main
cd androidx-main
```

4. Use `repo` command to initialize the repository.

```bash
repo init -u https://android.googlesource.com/platform/manifest -b androidx-main --partial-clone --clone-filter=blob:limit=10M
```

5. Now your repository is set to pull only what you need for building and running AndroidX libraries. Download the code (and grab a coffee while we pull down the files):

```bash
repo sync -j8 -c
```

You will use this command to sync your checkout in the future - it’s similar to `git fetch`


## Using Android Studio
To open the project with the specific version of Android Studio recommended for developing:

```bash
cd path/to/checkout/frameworks/support/
ANDROIDX_PROJECTS=MAIN ./gradlew studio
```

and accept the license agreement when prompted. Now you're ready to edit, run, and test!

You can also the following sets of projects: `ALL`, `MAIN`, `COMPOSE`, or `FLAN`

If you get “Unregistered VCS root detected” click “Add root” to enable git integration for Android Studio.

If you see any warnings (red underlines) run `Build > Clean Project`.

## Builds
### Full Build (Optional)
You can do most of your work from Android Studio, however you can also build the full AndroidX library from command line:

```bash
cd path/to/checkout/frameworks/support/
./gradlew createArchive
```

### Testing modified AndroidX Libraries to in your App
You can build maven artifacts locally, and test them directly in your app:

```bash
./gradlew createArchive
```

And put the following at the top of your 'repositories' property in your **project** `build.gradle` file:

```gradle
maven { url '/path/to/checkout/out/androidx/build/support_repo/' }
```

**NOTE: In order to see your changes in the project, you might need to clean your build (`Build > Clean Project` in Android Studio or run `./gradlew clean`).**

### Continuous integration
[Our continuous integration system](https://ci.android.com/builds/branches/aosp-androidx-main/grid?) builds all in progress (and potentially unstable) libraries as new changes are merged. You can manually download these AARs and JARs for your experimentation.

## Running Tests

### Single Test Class or Method
1. Open the desired test file in Android Studio.
2. Right-click on a test class or @Test method name and select `Run FooBarTest`

### Full Test Package
1. In the project side panel open the desired module.
2. Find the directory with the tests
3. Right-click on the directory and select `Run androidx.foobar`

## Running Sample Apps
The AndroidX repository has a set of Android applications that exercise AndroidX code. These applications can be useful when you want to debug a real running application, or reproduce a problem interactively, before writing test code.

These applications are named either `<libraryname>-integration-tests-testapp`, or `support-\*-demos` (e.g. `support-v4-demos` or `support-leanback-demos`). You can run them by clicking `Run > Run ...` and choosing the desired application.

## Password and Contributor Agreement before making a change
Before uploading your first contribution, you will need setup a password and agree to the contribution agreement:

Generate a HTTPS password:
https://android-review.googlesource.com/new-password

Agree to the Google Contributor Licenses Agreement:
https://android-review.googlesource.com/settings/new-agreement

## Making a change
```bash
cd path/to/checkout/frameworks/support/
repo start my_branch_name .
# make needed modifications...
git commit -a
repo upload --current-branch .
```

If you see the following prompt, choose `always`:

```
Run hook scripts from https://android.googlesource.com/platform/manifest (yes/always/NO)?
```

If the upload succeeds, you'll see output like:

```
remote:
remote: New Changes:
remote:   https://android-review.googlesource.com/c/platform/frameworks/support/+/720062 Further README updates
remote:
```

To edit your change, use `git commit --amend`, and re-upload.

## Getting reviewed
* After you run repo upload, open [r.android.com](http://r.android.com)
* Sign in into your account (or create one if you do not have one yet)
* Add an appropriate reviewer (use git log to find who did most modifications on the file you are fixing or check the OWNERS file in the project's directory)

## Handling binary dependencies
AndroidX uses git to store all the binary Gradle dependencies. They are stored in `prebuilts/androidx/internal` and `prebuilts/androidx/external` directories in your checkout. All the dependencies in these directories are also available from `google()`, `jcenter()`, or `mavenCentral()`. We store copies of these dependencies to have hermetic builds. You can pull in [a new dependency using our importMaven tool](development/importMaven/README.md).
