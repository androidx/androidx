# Project creator

This script will create a new library project and associated Gradle module using
a `groupId` and `artifactId`.

It will use the `groupId` and `artifactId` to guess which configuration is most
appropriate for the project you are creating.

## Basic usage

```bash
./create_project.py androidx.foo foo-bar
```

## Project types

The script leverages
`buildSrc/public/src/main/kotlin/androidx/build/SoftwareType.kt` to create the
recommended defaults for your project. However, you can override the options to
best fit your requirements.

## Additional documentation

See go/androidx-api-guidelines#module-creation (internal-only) or the
[equivalent page](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:docs/api_guidelines/modules.md#module-creation)
on public Android Code Search for advanced usage and solutions to common issues.

## Development

If you make any changes to the script, please update this `README` and make
corresponding updates at go/androidx-api-guidelines#module-creation.

### Testing the script

Generic project integration test
```bash
./create_project.py androidx.foo.bar bar-qux
```

Script test suite
```bash
./test_project_creator.py
```
