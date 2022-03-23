## wrapper/gradle-wrapper.properties

Keeps track of Gradle version used by androidx. When updating the version a new version prebuilt needs to be added to `tools/external/gradle` repository.

## libs.versions.toml

Keeps track of library and plugin dependencies used by androidx. Adding or updating a library there requires running `./development/importMaven/import_maven_artifacts.py -n myartifact:here:1.0.0`

## verification-keyring.keys

Checked-in [local keyring](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:local-keyring)
that is used to avoid reaching out to key servers whenever a key is required by Gradle to verify an
artifact.

AndroidX only uses human readable `verification-keyring.keys`. Gradle also generates binary
`verification-keyring.gpg`, but it is optional, and thus we do not use it.

To update this file, after adding the relevant dependencies to the build, run:
```
development/update-verification-metadata.sh
```

## verification-metadata.xml

[Configuration file for Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html#sub:verification-metadata) used by androidx to make sure dependencies are [signed with trusted signatures](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:signature-verificationn) and that unsigned artifacts have [expected checksums](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:checksum-verification).

When adding a new artifact
- if it is signed, then run:
```
development/update-verification-metadata.sh
```
to trust the signature of the new artifact.

- if it is not signed, then run the following to add generated checksums to `verification-metadata.xml`:

```
./gradlew -M sha256 buildOnServer --dry-run
```

Then you will want to diff `gradle/verification-metadata.dryrun.xml` and
`gradle/verification-metadata.xml` using your favorite tool (e.g. meld) can copy over the entries
that are relevant to your new artifacts.

You can then delete all the `verification-*-dryrun.*` files.
