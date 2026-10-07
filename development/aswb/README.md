# Guide for setting up a separate profile for Android Studio

It can be helpful to have a separate profile of Android Studio when
working on AndroidX as we use prebuilts of an SDK instead of public SDK.
Launching Studio with a custom idea.properties files allows to
have a separate profile of Android Studio for work with AndroidX.

To use this launch Studio with setting STUDIO_PROPERTIES env variable to point
to idea.properties in this checkout that sets custom `idea.config.path` and
`idea.system.path`. `sample-androidx-canary.desktop` is an example Linux desktop
shortcut entry.

[More details on this on Jetbrains website](https://intellij-support.jetbrains.com/hc/en-us/articles/207240985-Changing-IDE-default-directories-used-for-config-plugins-and-caches-storage?page=2)
