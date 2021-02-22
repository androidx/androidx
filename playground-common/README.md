# Playground Setup for AndroidX Projects

AndroidX is a fairly large project with 300+ modules which makes it a
very resource heavy project for local development.

Playground setup allows sub projects to have an additional settings.gradle
file that can be run independent of the main project.
It also allows using external resources for artifacts such that just checking
out the AndroidX git repository is enough without the prebuilt repositories
that are needed by the main AndroidX project.

These project setups are only meant to be used for local development and
all CI tasks run using the root AndroidX project.

## How it works?
A playground project needs a `settings.gradle` file that applies
`playground-common/playground-settings.gradle` which provides functionality
to pull select projects from AndroidX.

To share as much common configuration as possible, it is also recommended
to symlink some common files like `gradle` and `.idea` configuration.

To do that, execute "setup-playground.sh" comamnd in your playground directory.
```
cd room;
../playground-common/setup-playground.sh
```
This script will create symbolic links for `gradle` and `.idea` files that are committed
to the git repository. It also force adds the `.idea` files to the git repository because
by default any nested .idea folder is ignored from git.

The `playground-settings.gradle` file sets a pre-defined build file (`playground-build.gradle`)
for the root project and also provides `includeProject` and `selectProjectsFromAndroidX`
methods.

The custom `settings.gradle` file should first call `setupPlayground(this, "..")` to
run the main configuration. Here, the first argument is the `script` object itself and
the second argument is the relative path to the main AndroidX project.

After running `setupPlayground`, it can either include projects via `includeProject`
method or filter projects from the main AndroidX settings gradle file using the
`selectProjectsFromAndroidX` method.

### Properties
When a `gradle.properties` file shows up under a sub project, main AndroidX build ends up
reading it. For this reason, we can only keep a minimal `gradle.properties` file in these
sub modules that also support playground setup.

We cannot avoid creating `gradle.properties` as certain properties (e.g. `useAndroidX`) are
read at configuration time and we cannot set it dynamically.

Properties that will be set dynamically are kept in `playground.properties` file while
shared properties are kept in `androidx-shared.properties` file.
The dynamic properties are read in the `playground-include-settings.gradle` file and set
on each project.

There is a `VerifyPlaygroundGradlePropertiesTask` task that validates the contents of
`androidx-shared.properties` file as part of the main AndroidX build.
### Optional Dependencies
Even though sub-projects usually declare exact coordinates for their dependencies,
for tests, it is a common practice to declare `project` dependencies. To avoid needing
to include all of those projects to make the build file work, `AndroidXPlaygroundRootPlugin`
adds a `projectOrArtifact` method to each sub project. This function can be used instead of
`project` to declare optional project dependencies. This function will return the
`project` if it exists or default to its latest artifact if it doesn't.

Note that range artifacts are not allowed in the main AndroidX build so when the sub
project is opened as part of the main AndroidX build, `projectOrArtifact` always resolves
to the `project`. In playground projects, it always resolves to the latest `SNAPSHOT`
artifact that is included in the playground build.
