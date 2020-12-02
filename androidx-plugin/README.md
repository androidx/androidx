# AndroidX Gradle Plugin

The AndroidX Gradle plugin is a repackaged version of the existing Gradle plugin in the `buildSrc` directory.

This project helps decouple AndroidX project builds from having to use `buildSrc` and they can use the maven coordinates
`androidx.build:gradle-plugin:<version>` instead. 

```groovy
// in settings.gradle
includeBuild("../androidx-plugin")

// in build.gradle
dependencies {
    //.. other dependencies
    classpath 'androidx.build:gradle-plugin:0.1.0'
}
```

