Fixed APKs used in ChecksumsTest.java.
Has to be submitted instead of built to keep hashes constant.

Generation of CtsPkgInstallTiny* apks was performed using the `apksigner` command-line tool,
which lives at `tools/apksig/src/apksigner/java/com/android/apksigner/` in the
android source tree.  Please refer to the usage instructions there for how to
sign APKs using different keystores, providers, etc.

Source app:
cts/hostsidetests/appsecurity/test-apps/tinyapp

Use this command to re-generate the apk and v4 signature file:
apksigner sign --v2-signing-enabled false --v3-signing-enabled false --v4-signing-enabled false --key cts/hostsidetests/appsecurity/certs/pkgsigverify/dsa-3072.pk8 --cert cts/hostsidetests/appsecurity/certs/pkgsigverify/dsa-3072.x509.pem -out cts/tests/tests/content/data/CtsPkgInstallTinyAppV1.apk cts/hostsidetests/appsecurity/res/pkgsigverify/original.apk
apksigner sign --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled --key cts/hostsidetests/appsecurity/certs/pkgsigverify/dsa-3072.pk8 --cert cts/hostsidetests/appsecurity/certs/pkgsigverify/dsa-3072.x509.pem -out cts/tests/tests/content/data/CtsPkgInstallTinyAppV2V3V4.apk cts/hostsidetests/appsecurity/res/pkgsigverify/original.apk
apksigner sign --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled --key cts/hostsidetests/appsecurity/certs/pkgsigverify/ec-p384.pk8 --cert cts/hostsidetests/appsecurity/certs/pkgsigverify/ec-p384.x509.pem -out cts/tests/tests/content/data/CtsPkgInstallTinyAppV2V3V4-Sha512withEC.apk cts/hostsidetests/appsecurity/res/pkgsigverify/original.apk
apksigner sign --v2-signing-enabled true --v3-signing-enabled true --v4-signing-enabled --verity-enabled --key cts/hostsidetests/appsecurity/certs/pkgsigverify/dsa-3072.pk8 --cert cts/hostsidetests/appsecurity/certs/pkgsigverify/dsa-3072.x509.pem -out cts/tests/tests/content/data/CtsPkgInstallTinyAppV2V3V4-Verity.apk cts/hostsidetests/appsecurity/res/pkgsigverify/original.apk

Generation of HelloWorld5 was done from a HelloWorld project in AndroidStudio. Any application with
splits will do.

!Please note that all hardcoded hashes in ChecksumsTest.java will have to be changed!
Use md5sum, sha1sum, sha256sum, sha512sum command line tools to regenerate full apk hashes.
