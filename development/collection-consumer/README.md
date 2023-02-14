## Testing collection KMP release

This project helps to validate a newly-staged version of the KMP build of androidx.collection.  Here's how!

### Stage the release
- Create a new global release in production or autopush JetPad (TODO: more info about how)
- Schedule a library group release attached to the global release for androidx.collect (TODO: ditto)
- You cannot stage unless the _global_ ADMRS config allowlists the KMP targets.  If necessary, you may need to
  create, have reviewed, and submit a CL like [this one](https://critique.corp.google.com/cl/474557118).
  - It can take around 30 minutes between submission and updating the loaded allowlist in ADMRS, so be patient.
- Then:
  - Start with [prod](go/jetpad) or [autopush](go/jetpad-autopush)
  - `Release Dates` > `Browse Release Date`
  - Click `Release Information` for the global release you created above
  - `Stage to ADMRS`
  - `Compose BOM?`  No (not relevant to our test)
  - Answer "Yes" to "really staging"
- At this point, you will either see an error in the first ~20 seconds if something is wrong with our stuff, or
  ADMRS will go quietly do things for a few minutes, resulting in an email to `mdb.jetpad-admins@google.com`.

### Test the staged release
- Check out this repo (if you haven't): `git clone sso://user/saff`
- The email has [instructions](go/adt-redir) for how to set up a proxy server for the staged maven repo.
- Once that's done, if necessary, edit the androidx.collection version in build.gradle.kts
- To test JVM:
  - `./gradlew installDist`
  - `./build/install/collection-consumer/bin/collection-consumer`
- To test native:
  - `./gradlew nativeBinaries`
  - `build/bin/native/releaseExecutable/collection-consumer.kexe`
- You can look back at the stdout for the adt-redir proxy server to assure yourself that the androidx dependencies
  are being loaded through the proxy.
- Profit??!