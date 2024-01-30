## Processors

### Proguard {#proguard}

Proguard configurations allow libraries to specify how post-processing tools
like optimizers and shrinkers should operate on library bytecode. Note that
while Proguard is the name of a specific tool, a Proguard configuration may be
read by R8 or any number of other post-processing tools.

NOTE Jetpack libraries **must not** run Proguard on their release artifacts. Do
not specify `minifyEnabled`, `shrinkResources`, or `proguardFiles` in your build
configuration.

#### Bundling with a library {#proguard-rules}

**Android libraries (AARs)** can bundle consumer-facing Proguard rules using the
`consumerProguardFiles` (*not* `proguardFiles`) field in their `build.gradle`
file's `defaultConfig`:

```
android {
    defaultConfig {
        consumerProguardFiles 'proguard-rules.pro'
    }
}
```

Libraries *do not* need to specify this field on `buildTypes.all`.

**Java-only libraries (JARs)** can bundle consumer-facing Proguard rules by
placing the file under the `META-INF` resources directory. The file **must** be
named using the library's unique Maven coordinate to avoid build-time merging
issues:

```
<project>/src/main/resources/META-INF/proguard/androidx.core_core.pro
```

#### Conditional Proguard Rules {#proguard-conditional}

Libraries are strongly encouraged to minimize the number of classes that are
kept as part of keep rules. More specifically, library authors are expected to
identify the entry points of their library that call into code paths that may
require classes to be exempt from proguard rules. This may be due to internal
reflection usages or JNI code. In the case of JNI code, java/kotlin classes and
methods that are implemented in native must be exempt in order to avoid JNI
linking errors in libraries that are consumed by applications built with
proguard enabled.

A common pattern is to create an annotation class that is used to annotate all
classes and methods that are to be excluded from proguard obfuscation.

For example:

```
/// in MyProguardExceptionAnnotation.kt
internal annotation class MyProguardExemptionAnnotation
```

Then reference this annotation within your proguard config conditionally
whenever the public API is consumed that leverages facilities that need to be
excluded from proguard optimization.

```
# in proguard-rules.pro
# The following keeps classes annotated with MyProguardExemptionAnnotation
# defined above
-if class androidx.mylibrary.MyPublicApi
-keep @androidx.mylibrary.MyProguardExemptionAnnotation public class *

# The following keeps methods annotated with MyProguardExcemptionAnnotation
-if class androidx.mylibrary.MyPublicApi
-keepclasseswithmembers class * {
    @androidx.mylibrary.MyProguardExcemptionAnnotation *;
}
```

Note that for each public API entry point an additional proguard rule would need
to be introduced in the corresponding proguard-rules.pro. This is because as of
writing there is no "or" operator within proguard that can be used to include
the keep rules for multiple conditions. So each rule would need to be
copy/pasted for each public API entrypoint.
