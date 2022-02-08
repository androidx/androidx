# Module root

JUnit Gtest

# Package androidx.test.ext.junitgtest

A JUnit runner for running Gtest suites on connected devices. Can be used by creating a Java/Kotlin class for the Gtest suite we want to run and annotating it with the `TargetLibrary` annotation. For example, given a gtest suite in a file `mytest.cpp`, we can create the following Kotlin class inside our `androidTest` directory.

```kotlin
@RunWith(GtestRunner::class)
@TargetLibrary("mytest")
class MyTest
```

And the `junit-gtest` library must be linked to your test library like the example `CMakeLists.txt`

```
project("example")

find_package(googletest REQUIRED CONFIG)
find_package(junit-gtest REQUIRED CONFIG)

add_library(
 mylib
 SHARED
 mylib.cpp)

add_library(
 mytest
 SHARED
 mytest.cpp)

target_link_libraries( # Specifies the target library.
 mytest
 PRIVATE
 mylib
 googletest::gtest
 junit-gtest::junit-gtest
 )
```