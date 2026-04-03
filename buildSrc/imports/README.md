This directory contains projects that just mirror the corresponding project in the main build in frameworks/support

This may be useful if a project in frameworks/support creates a plugin that another project wants to apply

<h3>Some information on plugins:</h3>

<br />

The plugin definitions (e.g. `gradlePlugin {}` blocks) in the other projects in frameworks/support
(for example, `benchmark/baseline-profile-gradle-plugin/build.gradle`) are what's used to publish
the plugins to Google Maven.

The plugin definitions (e.g. `gradlePlugin {}` blocks) in this directory are used to pull in the
plugins to `buildSrc` so that the plugins can be built before the rest of the project is built
(`buildSrc` build before anything else in the AndroidX project).

In theory, the projects that rely on these plugins (for example, the
[ones that use baseline-profile-gradle-plugin](https://cs.android.com/search?q=%22id(%22androidx.baselineprofile%22)%22%20filepath:build.gradle&sq=&ss=androidx%2Fplatform%2Fframeworks%2Fsupport))
could just pull from Google Maven, but we want to use tip-of-tree versions of the plugins to detect
issues faster and to make it easier for owning teams to iterate on features.
