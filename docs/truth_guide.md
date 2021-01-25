# Adding custom Truth subjects

[TOC]

## Custom truth subjects

Every subject class should extend
[Subject](https://truth.dev/api/latest/com/google/common/truth/Subject.html) and
follow the naming schema `[ClassUnderTest]Subject`. The Subject must also have a
constructor that accepts
[FailureMetadata](https://truth.dev/api/latest/com/google/common/truth/FailureMetadata.html)
and a reference to the object under test, which are both passed to the
superclass.

```kotlin
class NavControllerSubject private constructor(
    metadata: FailureMetadata,
    private val actual: NavController
) : Subject(metadata, actual) { }
```

### Subject factory

The Subject class should also contain two static fields; a
[Subject Factory](https://truth.dev/api/latest/com/google/common/truth/Subject.Factory.html)
and an`assertThat()` shortcut method.

A subject Factory provides most of the functionality of the Subject by allowing
users to perform all operations provided in the Subject class by passing this
factory to `about()` methods. E.g:

`assertAbout(navControllers()).that(navController).isGraph(x)` where `isGraph()`
is a method defined in the Subject class.

The assertThat() shortcut method simply provides a shorthand method for making
assertions about the Subject without needing a reference to the subject factory.
i.e., rather than using
`assertAbout(navControllers()).that(navController).isGraph(x)` users can simply
use`assertThat(navController).isGraph(x)`.

```kotlin
companion object {
    fun navControllers(): Factory<NavControllerSubject, NavController> =
        Factory<NavControllerSubject, NavController> { metadata, actual ->
            NavControllerSubject(metadata, actual)
        }

    @JvmStatic
    fun assertThat(actual: NavController): NavControllerSubject {
        return assertAbout(navControllers()).that(actual)
    }
}
```

### Assertion methods

When creating assertion methods for your custom Subject the names of these
methods should follow the
[Truth naming convention](https://truth.dev/faq#assertion-naming).

To create assertion methods it is necessary to either delegate to an existing
assertion method by using `Subject.check()` or to provide your own failure
strategy by using`failWithActual()` or `failWithoutActual()`.

When using `failWithActual()` the error message will display the`toString()`
value of the Subject object. Additional information can be added to these error
messages by using `fact(key, value)` or `simpleFact(value)` where `fact()` will
be output as a colon separated key, value pair.

```kotlin
fun isGraph(@IdRes navGraph: Int) {
    check("graph()").that(actual.graph.id).isEqualTo(navGraph)
}

// Sample Failure Message
value of          : navController.graph()
expected          : 29340
but was           : 10394
navController was : {actual.toString() value}
```

```kotlin
fun isGraph(@IdRes navGraph: Int) {
    val actualGraph = actual.graph.id
    if (actualGraph != navGraph) {
        failWithoutActual(
            fact("expected id", navGraph.toString()),
            fact("but was", actualGraph.toString()),
            fact("current graph is", actual.graph)
        )
    }
}

// Sample Failure Message
expected id      : 29340
but was          : 10394
current graph is : {actual.graph.toString() value}
```

## Testing

When testing expected successful assertions it is enough to simply call the
assertion with verified successful actual and expected values.

```kotlin
private lateinit var navController: NavController
@Before
fun setUp() {
    navController = NavController(
        ApplicationProvider.getApplicationContext()
    ).apply {
        navigationProvider += TestNavigator()
        // R.navigation.testGraph == R.id.test_graph
        setGraph(R.navigation.test_graph)
    }
}

@Test
fun testGraph() {
    assertThat(navController).isGraph(R.id.test_graph)
}
```

To test that expected failure cases you should use the
[assertThrows](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:testutils/testutils-truth/src/main/java/androidx/testutils/assertions.kt)
function from the AndroidX testutils module. The assertions.kt file contains two
assertThrows functions. The second method, which specifically returns a
TruthFailureSubject, should be used since it allows for validating additional
information about the FailureSubject, particularly that it contains specific
fact messages.

```kotlin
@Test
fun testGraphFailure() {
    with(assertThrows {
        assertThat(navController).isGraph(R.id.second_test_graph)
    }) {
        factValue("expected id").isEqualTo(R.id.second_test_graph.toString())
        factValue("but was").isEqualTo(navController.graph.id.toString())
        factValue("current graph is").isEqualTo(navController.graph.toString())
    }
}
```

## Helpful resources

[Truth extension points](https://truth.dev/extension.html)
