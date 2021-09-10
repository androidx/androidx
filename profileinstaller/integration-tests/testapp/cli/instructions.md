
== Repackage tool


== Profgen CLI tool
`profgen-cli.jar` is generated in `studio-master-dev` by the _gradle_ task:

Edit the `build.gradle` for profgen-cli to add a jar clause:

```
jar {
    manifest {
        attributes "Main-Class": "com.android.tools.profgen.cli.main"
        attributes "Class-Path": configurations.compile.collect { it.name }.join(' ')
    }

    from { configurations.runtimeClasspath.collect { zipTree(it) } }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
```

Then generate a fat jar with gradle:

```
cd <studio-master-dev-checkout>/studio-master-dev/tools
./gradlew :base:profgen-cli:clean :base:profgen-cli:jar
ls ../out/build/base/profgen-cli/build/libs/profgen-cli*.jar
```

Copy the resulting file to this directory and name it `profgen-cli.jar`