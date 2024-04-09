# Adding custom lint checks

[TOC]

## Getting started

Lint is a static analysis tool that checks Android project source files. Lint
checks come with Android Studio by default, but custom lint checks can be added
to specific library modules to help avoid potential bugs and encourage best code
practices.

This guide is targeted to developers who would like to quickly get started with
adding lint checks in the AndroidX development workflow. For a complete guide to
writing and running lint checks, see the official
[Android lint documentation](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/docs/README.md.html).

### Create a module

If this is the first lint rule for a library, you will need to create a module
by doing the following:

Include the project in the top-level `settings.gradle` file so that it shows up
in Android Studio's list of modules:

```
includeProject(":mylibrary:mylibrary-lint", "mylibrary/mylibrary-lint")
```

Manually create a new module in `frameworks/support` (preferably in the
directory you are making lint rules for). In the new module, add a `src` folder
and a `build.gradle` file containing the needed dependencies.

`mylibrary/mylibrary-lint/build.gradle`:

```
import androidx.build.LibraryType

plugins {
    id("AndroidXPlugin")
    id("kotlin")
}

dependencies {
    compileOnly(libs.androidLintMinApi)
    compileOnly(libs.kotlinStdlib)

    testImplementation(libs.kotlinStdlib)
    testImplementation(libs.androidLint)
    testImplementation(libs.androidLintTests)
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

androidx {
    name = "MyLibrary lint checks"
    type = LibraryType.LINT
    mavenGroup = LibraryGroups.MYLIBRARY
    inceptionYear = "2022"
    description = "Lint checks for MyLibrary"
}
```

### Issue registry

Your new module will need to have a registry that contains a list of all of the
checks to be performed on the library. There is an
[`IssueRegistry`](https://cs.android.com/android/platform/superproject/+/master:tools/base/lint/libs/lint-api/src/main/java/com/android/tools/lint/client/api/IssueRegistry.java;l=47)
class provided by the tools team. Extend this class into your own
`IssueRegistry` class, and provide it with the issues in the module.

`MyLibraryIssueRegistry.kt`

```kotlin
class MyLibraryIssueRegistry : IssueRegistry() {
    override val minApi = CURRENT_API
    override val api = 13
    override val issues get() = listOf(MyLibraryDetector.ISSUE)
    override val vendor = Vendor(
        feedbackUrl = "https://issuetracker.google.com/issues/new?component=######",
        identifier = "androidx.mylibrary",
        vendorName = "Android Open Source Project",
    )
}
```

The maximum version this lint check will will work with is defined by `api =
13`. Typically, this should track `CURRENT_API`.

`minApi = CURRENT_API` sets the lowest version of the Lint tool that this will
work with.

`CURRENT_API` is defined by the Lint tool API version against which your project
is compiled, as defined in the module's `build.gradle` file. Jetpack lint check
modules should compile using the Lint tool API version referenced in
[libs.versions.toml](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:gradle/libs.versions.toml;l=8).

We guarantee that our lint checks work with the versions referenced by `minApi`
and `api` by running our tests with both versions. For newer versions of Android
Studio (and consequently, the Lint tool) the API variable will need to be
updated.

The `IssueRegistry` requires a list of all of the issues to check. You must
override the `IssueRegistry.getIssues()` method. Here, we override that method
with a Kotlin `get()` property delegate:

[Example `IssueRegistry` Implementation](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:fragment/fragment-lint/src/main/java/androidx/fragment/lint/FragmentIssueRegistry.kt)

There are 4 primary types of lint checks:

1.  Code - Applied to source code, ex. `.java` and `.kt` files
1.  XML - Applied to XML resource files
1.  Android Manifest - Applied to `AndroidManifest.xml`
1.  Gradle - Applied to Gradle configuration files, ex. `build.gradle`

It is also possible to apply lint checks to compiled bytecode (`.class` files)
or binary resource files like images, but these are less common.

## PSI & UAST mapping

To view the PSI structure of any file in Android Studio, use the
[PSI Viewer](https://www.jetbrains.com/help/idea/psi-viewer.html) located in
`Tools > View PSI Structure`.

Note: The PSI Viewer requires enabling internal mode. Follow the directions
[here](https://plugins.jetbrains.com/docs/intellij/enabling-internal.html) to
add `idea.is.internal=true` to `idea.properties.`

<table>
  <tr>
   <td><strong>PSI</strong>
   </td>
   <td><strong>UAST</strong>
   </td>
  </tr>
  <tr>
   <td>PsiAnnotation
   </td>
   <td>UAnnotation
   </td>
  </tr>
  <tr>
   <td>PsiAnonymousClass
   </td>
   <td>UAnonymousClass
   </td>
  </tr>
  <tr>
   <td>PsiArrayAccessExpression
   </td>
   <td>UArrayAccessExpression
   </td>
  </tr>
  <tr>
   <td>PsiBinaryExpression
   </td>
   <td>UArrayAccesExpression
   </td>
  </tr>
  <tr>
   <td>PsiCallExpression
   </td>
   <td>UCallExpression
   </td>
  </tr>
  <tr>
   <td>PsiCatchSection
   </td>
   <td>UCatchClause
   </td>
  </tr>
  <tr>
   <td>PsiClass
   </td>
   <td>UClass
   </td>
  </tr>
  <tr>
   <td>PsiClassObjectAccessExpression
   </td>
   <td>UClassLiteralExpression
   </td>
  </tr>
  <tr>
   <td>PsiConditionalExpression
   </td>
   <td>UIfExpression
   </td>
  </tr>
  <tr>
   <td>PsiDeclarationStatement
   </td>
   <td>UDeclarationExpression
   </td>
  </tr>
  <tr>
   <td>PsiDoWhileStatement
   </td>
   <td>UDoWhileExpression
   </td>
  </tr>
  <tr>
   <td>PsiElement
   </td>
   <td>UElement
   </td>
  </tr>
  <tr>
   <td>PsiExpression
   </td>
   <td>UExpression
   </td>
  </tr>
  <tr>
   <td>PsiForeachStatement
   </td>
   <td>UForEachExpression
   </td>
  </tr>
  <tr>
   <td>PsiIdentifier
   </td>
   <td>USimpleNameReferenceExpression
   </td>
  </tr>
  <tr>
   <td>PsiLiteral
   </td>
   <td>ULiteralExpression
   </td>
  </tr>
  <tr>
   <td>PsiLocalVariable
   </td>
   <td>ULocalVariable
   </td>
  </tr>
  <tr>
   <td>PsiMethod
   </td>
   <td>UMethod
   </td>
  </tr>
  <tr>
   <td>PsiMethodCallExpression
   </td>
   <td>UCallExpression
   </td>
  </tr>
  <tr>
   <td>PsiParameter
   </td>
   <td>UParameter
   </td>
  </tr>
</table>

## Code detector

These are lint checks that will apply to source code files -- primarily Java and
Kotlin, but can also be used for other similar file types. All code detectors
that analyze Java or Kotlin files should implement the
[SourceCodeScanner](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/SourceCodeScanner.kt).

### API surface

#### Calls to specific methods

##### `getApplicableMethodNames`

This defines the list of methods where lint will call the visitMethodCall
callback.

```kotlin
override fun getApplicableMethodNames(): List<String>? = listOf(METHOD_NAMES)
```

##### `visitMethodCall`

This defines the callback that the Lint tool will call when it encounters a call
to an applicable method.

```kotlin
override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {}
```

#### Calls to specific class instantiations

##### `getApplicableConstructorTypes`

```kotlin
override fun getApplicableConstructorTypes(): List<String>? = listOf(CLASS_NAMES)
```

##### visitConstructor

```kotlin
override fun visitConstructor(context: JavaContext, node: UCallExpression, method: PsiMethod) {}
```

#### Classes that extend given superclasses

##### `getApplicableSuperClasses`

```kotlin
override fun applicableSuperClasses(): List<String>? = listOf(CLASS_NAMES)
```

##### `visitClass`

```kotlin
override fun visitClass(context: JavaContext, declaration: UClass) {}
```

#### Call graph support

It is possible to perform analysis on the call graph of a project. However, this
is highly resource intensive since it generates a single call graph of the
entire project and should only be used for whole project analysis. To perform
this analysis you must enable call graph support by overriding the
`isCallGraphRequired` method and access the call graph with the
`analyzeCallGraph(context: Context, callGraph: CallGraphResult)` callback
method.

For performing less resource intensive, on-the-fly analysis it is best to
recursively analyze method bodies. However, when doing this there should be a
depth limit on the exploration. If possible, lint should also not explore within
files that are currently not open in studio.

### Method call analysis

#### `resolve()`

Resolves into a `UCallExpression` or `UMethod` to perform analysis requiring the
method body or containing class.

#### `receiverType`

Each `UCallExpression` has a `receiverType` corresponding to the `PsiType` of
the receiver of the method call.

```kotlin
public abstract class LiveData<T> {
    public void observe() {}
}

public abstract class MutableLiveData<T> extends LiveData<T> {}

MutableLiveData<String> liveData = new MutableLiveData<>();
liveData.observe() // receiverType = PsiType<MutableLiveData>
```

#### Kotlin named parameter mapping

`JavaEvaluator`contains a helper method `computeArgumentMapping(call:
UCallExpression, method: PsiMethod)` that creates a mapping between method call
parameters and the corresponding resolved method arguments, accounting for
Kotlin named parameters.

```kotlin
override fun visitMethodCall(context: JavaContext, node: UCallExpression,
    method: PsiMethod) {
    val argMap: Map<UExpression, PsiParameter> = context.evaluator.computArgumentMapping(node, psiMethod)
}
```

### Testing

Because the `LintDetectorTest` API does not have access to library classes and
methods, you must implement stubs for any necessary classes and include these as
additional files in your test cases. For example, if a lint check involves
Fragment's `getViewLifecycleOwner` and `onViewCreated` methods, then we must
create a stub for this:

```
java("""
    package androidx.fragment.app;

    import androidx.lifecycle.LifecycleOwner;

    public class Fragment {
        public LifecycleOwner getViewLifecycleOwner() {}
        public void onViewCreated() {}
    }
""")
```

Since this class also depends on the `LifecycleOwner` class it is necessary to
create another stub for this.

NOTE `package-info.java` cannot be represented by a source stub and must be
provided as bytecode. See [Updating bytecode](#tips-bytecode) for tips on using
bytecode in lint tests.

## XML resource detector

These are lint checks that will apply to resource files including `anim`,
`layout`, `values`, etc. lint checks being applied to resource files should
extend
[`ResourceXmlDetector`](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/ResourceXmlDetector.java).
The `Detector` must define the issue it is going to detect, most commonly as a
static variable of the class.

```kotlin
companion object {
    val ISSUE = Issue.create(
        id = "TitleOfMyIssue",
        briefDescription = "Short description of issue. This will be what the studio inspection menu shows",
        explanation = """Here is where you define the reason that this lint rule exists in detail.""",
        category = Category.CORRECTNESS,
        severity = Severity.LEVEL,
        implementation = Implementation(
            MyIssueDetector::class.java, Scope.RESOURCE_FILE_SCOPE
        ),
        androidSpecific = true
    ).addMoreInfo(
        "https://linkToMoreInfo.com"
    )
}
```

### API surface

The following methods can be overridden:

```kotlin
appliesTo(folderType: ResourceFolderType)
getApplicableElements()
visitElement(context: XmlContext, element: Element)
```

#### `appliesTo`

This determines the
[ResourceFolderType](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:layoutlib-api/src/main/java/com/android/resources/ResourceFolderType.java)
that the check will run against.

```kotlin
override fun appliesTo(folderType: ResourceFolderType): Boolean {
    return folderType == ResourceFolderType.TYPE
}
```

#### `getApplicableElements`

This defines the list of elements where the Lint tool will call your
`visitElement` callback method when encountered.

```kotlin
override fun getApplicableElements(): Collection<String>? = Collections.singleton(ELEMENT)
```

#### `visitElement`

This defines the behavior when an applicable element is found. Here you normally
place the actions you want to take if a violation of the lint check is found.

```kotlin
override fun visitElement(context: XmlContext, element: Element) {
    val fix = LintFix.create()
        .replace()
        .text(ELEMENT)
        .with(REPLACEMENT_TEXT)
        .build()

    context.report(
        issue = ISSUE,
        location = context.getNameLocation(element),
        message = "My issue message",
        quickFixData = fix
    )
}
```

In this instance, the call to `report()` takes the definition of the issue, the
location of the element that has the issue, the message to display on the
element, as well as a quick fix. In this case we replace our element text with
some other text.

[Example Detector Implementation](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:fragment/fragment-lint/src/main/java/androidx/fragment/lint/FragmentTagDetector.kt)

### Testing

You need tests for two things. First, you must test that the Lint tool API
version is properly set. That is done with a simple `ApiLintVersionTest` class.
It asserts the API version code set earlier in the `IssueRegistry()` class. This
test intentionally fails in the IDE because different Lint tool API versions are
used in Studio and the command line.

Example `ApiLintVersionTest`:

```kotlin
class ApiLintVersionsTest {

    @Test
    fun versionsCheck() {
        LintClient.clientName = LintClient.CLIENT_UNIT_TESTS
        val registry = MyLibraryIssueRegistry()
        assertThat(registry.api).isEqualTo(CURRENT_API)
        assertThat(registry.minApi).isEqualTo(10)
    }
}
```

Next, you must test the `Detector` class. The Tools team provides a
[`LintDetectorTest`](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-tests/src/main/java/com/android/tools/lint/checks/infrastructure/LintDetectorTest.java)
class that should be extended. Override `getDetector()` to return an instance of
the `Detector` class:

```kotlin
override fun getDetector(): Detector = MyLibraryDetector()
```

Override `getIssues()` to return the list of Detector Issues:

```kotlin
getIssues(): MutableList<Issue> = mutableListOf(MyLibraryDetector.ISSUE)
```

[`LintDetectorTest`](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-tests/src/main/java/com/android/tools/lint/checks/infrastructure/LintDetectorTest.java)
provides a `lint()` method that returns a
[`TestLintTask`](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-tests/src/main/java/com/android/tools/lint/checks/infrastructure/TestLintTask.java).
`TestLintTask` is a builder class for setting up lint tests. Call the `files()`
method and provide an `.xml` test file, along with a file stub. After completing
the set up, call `run()` which returns a
[`TestLintResult`](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-tests/src/main/java/com/android/tools/lint/checks/infrastructure/TestLintResult.kt).
`TestLintResult` provides methods for checking the outcome of the provided
`TestLintTask`. `ExpectClean()` means the output is expected to be clean because
the lint check was followed. `Expect()` takes a string literal of the expected
output of the `TestLintTask` and compares the actual result to the input string.
If a quick fix was implemented, you can check that the fix is correct by calling
`checkFix()` and providing the expected output file stub.

[TestExample](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:fragment/fragment-lint/src/test/java/androidx/fragment/lint/FragmentTagDetectorTest.kt)

## Android manifest detector

Lint checks targeting `AndroidManifest.xml` files should implement the
[XmlScanner](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/XmlScanner.kt)
and define target scope in issues as `Scope.MANIFEST`

## Gradle detector

Lint checks targeting Gradle configuration files should implement the
[GradleScanner](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/GradleScanner.kt)
and define target scope in issues as `Scope.GRADLE_SCOPE`

### API surface

#### `checkDslPropertyAssignment`

Analyzes each DSL property assignment, providing the property and value strings.

```kotlin
fun checkDslPropertyAssignment(
    context: GradleContext,
    property: String,
    value: String,
    parent: String,
    parentParent: String?,
    propertyCookie: Any,
    valueCookie: Any,
    statementCookie: Any
) {}
```

The property, value, and parent string parameters provided by this callback are
the literal values in the gradle file. Any string values in the Gradle file will
be quote enclosed in the value parameter. Any constant values cannot be resolved
to their values.

The cookie parameters should be used for reporting lint check errors. To report
an issue on the value, use `context.getLocation(statementCookie)`.

## Enabling lint checks for a library

Once the lint module is implemented we need to enable it for the desired
library. This can be done by adding a `lintPublish` rule to the `build.gradle`
of the library the lint check should apply to.

```
lintPublish(project(':mylibrary:mylibrary-lint'))
```

This adds a `lint.jar` file into the `.aar` bundle of the desired library.

Then we should add a `com.android.tools.lint.client.api.IssueRegistry` file in
`mylibrary > mylibrary-lint > main > resources > META-INF > services`. The file
should contain a single line that has the `IssueRegistry` class name with the
full path. This class can contain more than one line if the module contains
multiple registries.

```
androidx.mylibrary.lint.MyLibraryIssueRegistry
```

Note that `lintPublish` only publishes the lint module, it doesn't include it
when running lint on the module that `lintPublish` is attached to. In order to
also run these lint checks as part of the module that is publishing them, you
can add `lintChecks` in the same way.

```
lintChecks(project(':mylibrary:mylibrary-lint'))
```

## Advanced topics

### Analyzing multiple different file types

Sometimes it is necessary to implement multiple different scanners in a lint
detector. For example, the
[Unused Resource](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-checks/src/main/java/com/android/tools/lint/checks/UnusedResourceDetector.java)
lint check implements an XML and SourceCodeScanner in order to determine if
resources defined in XML files are ever references in the Java/Kotlin source
code.

#### File type iteration order

The Lint tool processes files in a predefined order:

1.  Manifests
1.  Android XML Resources (alphabetical by folder type)
1.  Java & Kotlin
1.  Bytecode
1.  Gradle

### Multi-pass analysis

It is often necessary to process the sources more than once. This can be done by
using `context.driver.requestRepeat(detector, scope)`.

### Debugging custom lint checks

Using Android Studio, there are a few ways to debug custom lint checks:

#### Debug against all lint check tests

1.  Set breakpoint(s) in the desired lint detector sources
1.  Click the `Gradle` icon on the right menu bar
1.  Run the `lintDebug` Gradle task and then hit the `Stop` icon in the top menu
    bar. This creates a Run configuration.
1.  Click the `Debug` icon in the top menu bar for the newly-selected Run
    configuration
1.  Breakpoint will get hit

#### Debug against a single lint check test

1.  Set breakpoint(s) in the desired lint detector sources
1.  Open a lint check test, such as
    [`AnnotationRetentionDetectorTest`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:annotation/annotation-experimental-lint/src/test/kotlin/androidx/annotation/experimental/lint/AnnotationRetentionDetectorTest.kt)
1.  Right-click on a test method and select `Debug`
1.  Breakpoint will get hit

## Helpful tips {#tips}

### Useful classes/packages

[`SdkConstants`](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:common/src/main/java/com/android/SdkConstants.java) -
contains most of the canonical names for Android core library classes, as well
as XML tag names.

### Updating bytecode and checksum in tests {#tips-bytecode}

When updating a file that is used in a lint test, the following error may appear
when running tests:

```
The checksum does not match for java/androidx/sample/deprecated/DeprecatedKotlinClass.kt;
expected 0x1af1856 but was 0x6692f601.
Has the source file been changed without updating the binaries?
Don't just update the checksum -- delete the binary file arguments and re-run the test first!
java.lang.AssertionError: The checksum does not match for java/androidx/sample/deprecated/DeprecatedKotlinClass.kt;
expected 0x1af1856 but was 0x6692f601.
Has the source file been changed without updating the binaries?
Don't just update the checksum -- delete the binary file arguments and re-run the test first!
    at org.junit.Assert.fail(Assert.java:89)
  ...
```

Here are the steps to fix this:

1.  Remove the arguments in `compiled()`:

    ```
    // Before
    compiled(
      "libs/ktlib.jar",
      ktSample("androidx.sample.deprecated.DeprecatedKotlinClass"),
      0x6692f601,
      """
      META-INF/main.kotlin_module:
      H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM2gxKDFAABNj30wGAAAAA==
      """,
      """
      androidx/sample/deprecated/DeprecatedKotlinClass.class:
      H4sIAAAAAAAAAJVSy27TQBQ9YydxcQNNH5SUZyivlkWSpuxAiFIEighBCiit
        // rest of bytecode
      """
    )

    // After
    compiled(
      "libs/ktlib.jar",
      ktSample("androidx.sample.deprecated.DeprecatedKotlinClass"),
    )
    ```

2.  Set `$LINT_TEST_KOTLINC` to the location of `kotlinc` if you haven't
    already, and add it to the test run configuration's environment variables.

    Note: The location of `kotlinc` can vary; use your system's file finder to
    determine the exact location. For gLinux, search under
    `~/.local/share/JetBrains`. For Mac, search under `<your androidx checkout
    root>/frameworks/support/studio`

    If it's not set (or set incorrectly), this error message appears when
    running tests:

    ```
    Couldn't find kotlinc to update test file java/androidx/sample/deprecated/DeprecatedKotlinClass.kt with.
    Point to it with $LINT_TEST_KOTLINC
    ```

3.  Run the test, which will output the new bytecode and checksum:

    ```
    Update the test source declaration for java/androidx/sample/deprecated/DeprecatedKotlinClass.kt with this list of encodings:

    Kotlin:
                  compiled(
                      "libs/ktlib.jar",
                      kotlin(
                          """
                          package java.androidx.sample.deprecated

                          @Deprecated(
                              // (rest of inlined sample file)
                          """
                      ).indented(),
                      0x5ba03e2d,
                  """
                  META-INF/main.kotlin_module:
                  H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM2gxKDFAABNj30wGAAAAA==
                    // rest of bytecode
                  """,
                  """
                  java/androidx/sample/deprecated/DeprecatedKotlinClass.class:
                  """
                  )
    ```

Note: the generated replacement code will inline the specified sample file (in
our case, `ktSample("androidx.sample.deprecated.DeprecatedKotlinClass")`).
Replace the inlined code with the sample declaration.

### Lint checks with WARNING severity (my lint check won't run!) {#tips-warnings}

In AndroidX lint checks with a severity of `WARNING` are ignored by default to
prevent noise from bundled lint checks. If your lint check has this severity,
and you want it to run inside AndroidX, you'll need to override the severity: in
Compose for example this happens in
[AndroidXComposeLintIssues](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/private/src/main/kotlin/androidx/build/AndroidXComposeLintIssues.kt).

## Helpful links

[Writing Custom Lint Rules](https://googlesamples.github.io/android-custom-lint-rules/)

[Studio Lint Rules](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-checks/src/main/java/com/android/tools/lint/checks/)

[Lint Detectors and Scanners Source Code](https://cs.android.com/android-studio/platform/tools/base/+/mirror-goog-studio-master-dev:lint/libs/lint-api/src/main/java/com/android/tools/lint/detector/api/)

[Creating Custom Link Checks (external)](https://twitter.com/alexjlockwood/status/1176675045281693696)

[Android Custom Lint Rules by Tor](https://github.com/googlesamples/android-custom-lint-rules)

[Public lint-dev Google Group](https://groups.google.com/forum/#!forum/lint-dev)

[In-depth Lint Video Presentation by Tor](https://www.youtube.com/watch?v=p8yX5-lPS6o)
(partially out-dated)
([Slides](https://resources.jetbrains.com/storage/products/kotlinconf2017/slides/KotlinConf+Lint+Slides.pdf))

[ADS 19 Presentation by Alan & Rahul](https://www.youtube.com/watch?v=jCmJWOkjbM0)

[META-INF vs Manifest](https://groups.google.com/forum/#!msg/lint-dev/z3NYazgEIFQ/hbXDMYp5AwAJ)
