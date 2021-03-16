# X Processing

This module (room-compiler-processing) provides an abstraction over Java Annotation Processing
(JavaAP) and Kotlin Symbol Processing (KSP) for Room's annotation processor.

If you are an annotation processor author and want to add KSP support to your project (while still
supporting Java AP), this library might be useful to get ideas or even directly use it in your
project.

**DISCLAIMER**
This is NOT a public Jetpack library and it will NOT have any of the API guarantees that Jetpack
Libraries provide (e.g. no semantic versioning). That being said, if it is useful for your use
case, feel free to use it. If you include it in your project, please `jarjar` it to avoid classpath
conflicts with Room.
This library is still expected to be a production quality abstraction because Room relies on it.

## Project Goals
This is **not** a general purpose abstraction over JavaAP(JSR 269) and KSP. Instead, it is designed
to support what Room needs (and only what Room needs). Despite this goal, it ended up covering most
use cases hence the library can be used as a general purpose abstraction and it also includes a
testing artifact (room-compiler-processing-testing).

If this abstraction turns out to be useful to enough people, we are open to unbundling it
from Room as an independent library.

### Want to make changes?
As long as it does not break Room, we are happy to expand the library to cover your use case. If you
would like a change:

* File a bug. [example](https://issuetracker.google.com/issues/182195680)
* Once the bug is acknowledged by the Room team, send a PR.
[example](https://github.com/androidx/androidx/pull/137)
  * Make sure to add good tests coveraging the change in the PR. Not only it is mandatory for
  AndroidX submissions, but will also help us ensure we don't break it.


## How To Use It

The entry API is the [XProcessingEnv](src/main/java/androidx/room/compiler/processing/XProcessingEnv.kt)
which has the necessary API's to access types etc.

To find annotated elements, you need to subclass the
[XProcessingStep](src/main/java/androidx/room/compiler/processing/XProcessingStep.kt).
It is very similar to the
[BasicAnnotationProcessor](https://github.com/google/auto/blob/master/common/src/main/java/com/google/auto/common/BasicAnnotationProcessor.java)
API in [Google Auto](https://github.com/google/auto) and you can find Room's implementation
[here](../compiler/src/main/kotlin/androidx/room/DatabaseProcessingStep.kt).

To initialize your `XProcessingStep` implementation, you still need to create your own
 AnnotationProcessor (JavaAP) or SymbolProcessor (KSP) implementations and tie it to the
 `XProcessingStep`.
* Room's [KSP processor](../compiler/src/main/kotlin/androidx/room/RoomKspProcessor.kt)
* Room's [JavaAP processor](../compiler/src/main/kotlin/androidx/room/RoomProcessor.kt)

For everything else, the API docs in the library should be sufficient and if not, feel free to file
bugs for more explanations.

## Main Classes
### XTypeElement
This is analogous to JavaAP's `TypeElement` or KSP's `KSClassDeclaration`.
It can be used to get methods, fields etc declared in a class declaration.
### XType
This is analogous to JavaAP's `TypeMirror` or KSP's `KSType`.  For convenience, each
`XType` has a `typeName` property that convert it to JavaPoet's `TypeName`. For types that do not
match 1-1 between Kotlin and Java (e.g. Kotlin `Int` maps to `java.lang.Integer` or primitive `int
`), Room will do a best effort conversion based on the information avaiable from the use site.
### XFieldElement
Represents a field in Java. Might be driven from a kotlin property.
You can obtain them via `XTypeElement`.
### XMethodElement
Represents a Java method or Kotlin function. They can be obtained from `XTypeElement`.
### XConstructorElement
Similar to `XMethodElement` but only represents constructor functions.
### XMethodType
Represents an `XMethodElement` as member of an `XType`. Notice that when a method is resolved as
a member of an `XType`, all available type arguments will be resolved based on the type
declaration.

## Known Caveats (a.k.a. hacks)
XProcessing assumes it is being used to generate Java code and tries to optimize for that. Chances
are, if you are converting a Java AP to support KSP, your annotation processor expects what
XProcessing does.
Caveats listed here are subject to change if/when Room supports generating Kotlin code but it is
very likely that we'll make it an argument to XProcessing.

### Suspend Methods
For suspend methods, it will synthesize an additional `Continuation` parameter.

### Properties
XProcessing will synthesize `getter`/`setter` methods for Kotlin properties.

### Overrides
When checking if a method overrides another, XProcessing [checks its JVM declaration as well
](https://android-review.googlesource.com/c/platform/frameworks/support/+/1564504/11/room/compiler-processing/src/main/java/androidx/room/compiler/processing/ksp/ResolverExt.kt#68),
which may not be what you see in Kotlin sources but this is exactly what an Annotation Processor
would see when used via KAPT.

### Internal Modifier
When an internal modifier is used in Kotlin code, its binary representation has a mangled name.
XProcessing automatically checks for it and the `name` property of a method/property will include
that conversion.
