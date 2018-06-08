# AOSP Support Library Contribution Guide
## Accepted Types of Contributions
* Bug fixes (needs a corresponding bug report in b.android.com)
* Each bug fix is expected to come with tests
* Fixing spelling errors
* Updating documentation
* Adding new tests to the area that is not currently covered by tests

We **are not** currently accepting new modules, features, or behavior changes.

## Checking Out the Code
**NOTE: You will need to use Linux or Mac OS. Building under Windows is not currently supported.**

Follow the [“Downloading the Source”](https://source.android.com/source/downloading.html) guide to install and set up `repo` tool, but instead of running the listed `repo` commands to initialize the repository, run the folowing:

    repo init -u https://android.googlesource.com/platform/manifest -b ub-supportlib-master

Now your repository is set to pull only what you need for building and running support library. Download the code (and grab a coffee while we pull down 7GB):

    repo sync -j8 -c

You will use this command to sync your checkout in the future - it’s similar to `git fetch`


## Using Android Studio
Open `path/to/checkout/frameworks/support/` in Android Studio. Now you're ready edit, run, and test!

If you get “Unregistered VCS root detected” click “Add root” to enable git integration for Android Studio.

If you see any warnings (red underlines) run `Build > Clean Project`.

## Builds
### Full Build (Optional)
You can do most of your work from Android Studio, however you can also build the full support library from command line:

    cd path/to/checkout/frameworks/support/
    ./gradlew createArchive

### Building Support Library as part of your App build
If you intend to repeatedly make changes to Support Library and to wish to see
the results in your app, and you don't want to have to repeatedly build them as
separate Gradle projects, you can
[configure your app build to build Support Library too](adding-support-library-as-included-build.md)

## Running Tests

### Single Test Class or Method
1. Open the desired test file in Android Studio.
2. Right-click on a test class or @Test method name and select `Run FooBarTest`

### Full Test Package
1. In the project side panel open the desired module.
2. Find the directory with the tests
3. Right-click on the directory and select `Run android.support.foobar`

## Running Sample Apps
Support library has a set of Android applications that exercise support library code. These applications can be useful when you want to debug a real running application, or reproduce a problem interactively, before writing test code.

These applications are named support-\*-demos (e.g. support-4v-demos or support-leanback-demos. You can run them by clicking `Run > Run ...` and choosing the desired application.

## Making a change
    cd path/to/checkout/frameworks/support/
    repo start my_branch_name .
    (make needed modifications)
    git commit -a
    repo upload --current-branch .

If you see the following prompt, choose `always`:

    Run hook scripts from https://android.googlesource.com/platform/manifest (yes/always/NO)?

## Getting reviewed
* After you run repo upload, open [r.android.com](http://r.android.com)
* Sign in into your account (or create one if you do not have one yet)
* Add an appropriate reviewer (use git log to find who did most modifications on the file you are fixing)

