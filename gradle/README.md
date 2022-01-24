## wrapper/gradle-wrapper.properties

Keeps track of Gradle version used by androidx. When updating the version a new version prebuilt needs to be added to `tools/external/gradle` repository.

## libs.versions.toml

Keeps track of library and plugin dependencies used by androidx. Adding or updating a library there requires running `./development/importMaven/import_maven_artifacts.py -n myartifact:here:1.0.0`

## verification-keyring.keys

Checked-in [local keyring](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:local-keyring) used to avoid reachout to key servers whenever a key is required by Gradle to verify an artifact. In order to add a new key, first add it as a trusted-key to `verification-metadata.xml`, then run the following

```
./gradlew --write-verification-metadata sha256 --export-keys
```

This will update `verification-keyring.keys` and also create `verification-keyring.gpg`. gpg file needs to be deleted as androidx only uses the human readable keychain to track of the keys.

## verification-metadata.xml

[Configuration file for Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html#sub:verification-metadata) used by androidx to make sure dependencies are [signed with trusted signatures](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:signature-verificationn) and that unsigned artifacts have [expected checksums](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:checksum-verification).

When adding a new artifact
- if it is signed, then follow `verification-keyring.keys` instructions above to add it to trusted-keys
- if it is not signed, then run the following to add generated checksums to `verification-metadata.xml`:

```
./gradlew --write-verification-metadata sha256
```
