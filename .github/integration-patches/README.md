# Integration Patches

This directory contains patch files needed to run workflows which test using the latest version of a
build dependency.

If a dependency makes a breaking change requiring a fix in AndroidX, but the fix can't be applied
until the dependency version is actually updated, adding the patch here will allow the integration
workflow to run successfully.

After the dependency version is updated for AndroidX, the patch file should be deleted.

Patches can be generated with `git diff > patch-file-name.patch`.

## Fixing patch files

If a patch file cannot be applied, follow these steps to fix it:

1. Run `git status` and ensure you have no local changes.
2. Run `git apply -3 <patch-file-path>`
3. Run `git status` and resolve the diffs for every file under "Unmerged paths". There may be
   compilation errors in the resolution since the patch is meant to work with a new version of a
   dependency. Run `git add` for these files once the diffs are resolved.
4. Run `git diff --staged > <patch-file-path>`
5. Run `git reset HEAD`, `git add <patch-file-path>`, and `git checkout .` (this will drop all
   pending changes except the patch file).
6. Check that the patch file is fixed with `./gradlew :validateIntegrationPatches`
7. Commit the patch file changes.

## Patch file names

| Workflow                    | Patch File Name              |
|-----------------------------|------------------------------|
| Gradle Nightly Test         | gradle-nightly.patch         |
| Gradle Release Nightly Test | gradle-release-nightly.patch |
| KGP Nightly Test            | kgp-nightly.patch            |
