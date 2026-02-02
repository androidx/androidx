# Clang Compilation

This package includes classes to compile C code using the Clang compiler distributed
in the Kotlin Native prebuilts.

Public API of this functionality is exported to build.gradle files via
`AndroidXMultiplatformExtension` to limit usages to KMP project.

There are 2 primary functionalities:

## Compiling C code with multiple targets:
`AndroidXMultiplatformExtension.createNativeCompilation` can be used to create a
`MultiTargetNativeCompilation` instance. `MultiTargetNativeCompilation` is the abstraction used to
define a C compilation that has sources, includes, dependencies and multiple Konan targets.

Unlike the CMake build, this compilation is fully compatible with Gradle build cache.

Once the compilation is created, it can be linked to the artifacts in 2 different ways:

### CInterop:
`AndroidXMultiplatformExtension.createCinterop` can be used to configure the build to compile the
given `MultiTargetNativeCompilation` and embed it into the klib via
[cinterop](https://kotlinlang.org/docs/native-c-interop.html).
The C code will be compiled per Konan target and the output will be embedded into the generated
klib.

* Note: Due to the limitation of CInterop requiring a DEF file with static library paths, CInterop
  compilation relies on relative paths between the source code and build output, hence the cache may
  not be fully move-able (see: KT-62800, KT-62795).

### Java Resources / Android JNI:
`AndroidXMultiplatformExtension.addNativeLibrariesToJniLibs` / `addNativeLibrariesToResources` can
be used to bundle the native code as a shared library inside java resources for JVM and `jnilibs`
for Android. This allows using the compiled library via regular JNI bridges.

## Clang vs CMake
This solution is initially created due to the Gradle build cache incompatibility of CMake. Konan
native compilation provides a decent alternative because Kotlin Native ships all necessary
multiplatform dependencies as 1 zip file (e.g. sysroots) along with Clang compiler. For native code,
we are only interested in platforms supported by Kotlin Native, hence this alignment is future
proof in case the set of platforms changes in the future. This is also another reason why the usages
of these APIs are limited to KMP projects.

Once the CMake cacheability problem is fixed, it should be possible to get rid of the Clang
compilation tasks if necessary cross-compilation dependecies can be obtained by other means.

You can read more about the design here: http://go/androidx-clang (internal only).
