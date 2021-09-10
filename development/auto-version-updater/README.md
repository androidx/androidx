# Auto Version Updater

This script will update versions in LibraryVersions.kt based on Jetpad.

It automatically runs `updateApi` and `repo upload . --cbr --label Presubmit-Ready+1`.

### Using the script

```bash
./update_versions_for_release.py 1234
```

Where 1234 is the Jetpad release id.

To use it without creating a commit and uploading a comment, run:

```bash
./update_versions_for_release.py 1234 --no-commit
```

### Testing the script

Script test suite
```bash
./test_update_versions_for_release.py
```