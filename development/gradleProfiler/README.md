# Profiling AndroidX Gradle configuration phase

1. Check out [gradle-profiler](https://github.com/gradle/gradle-profiler)
2. Build it with `./gradlew installDist`
3. Run the following:
```bash
LD_LIBRARY_PATH=$LD_LIBRARY_PATH:~/yourkit/bin/linux-x86-64/ \
    YOURKIT_HOME=~/yourkit/ \
    JAVA_TOOLS_JAR=/path/to/androidx/prebuilts/jdk/jdk8/linux-x86/lib/tools.jar \
    JAVA_HOME=/path/to/androidx/prebuilts/jdk/jdk17/linux-x86/ \
    ./build/install/gradle-profiler/bin/gradle-profiler \
    --profile yourkit \
    --project-dir /path/to/androidx/frameworks/support/ \
    --gradle-user-home my-gradle-user-home \
    --scenario-file /path/to/androidx/frameworks/support/development/gradleProfiler/configuration.scenarios \
    rerunDryRun
```

This will produce a `.snapshot` file that you can open in YourKit profiler for
analysis.