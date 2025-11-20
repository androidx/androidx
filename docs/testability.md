# Testability

[TOC]

## How to write testable libraries

When developers use a Jetpack library, it should be easy to write reliable and
automated tests for their own code's functionality. In most cases, tests written
against a library will need to interact with that library in some way -- setting
up preconditions, reaching synchronization points, making calls -- and the
library should provide necessary functionality to enable such tests, either
through public APIs or optional `-testing` artifacts.

**Testability**, in this document, means how easily and effectively the users of
a library can create tests for apps that use that library.

NOTE Tests that check the behavior of a library have a different mission than
tests made by app developers using that library; however, library developers may
find themselves in a similar situation when writing tests for sample code.

Often, the specifics of testability will vary from library to library and there
is no one way of providing it. Some libraries have enough public API surface,
others provide additional testing artifacts (e.g.
[Lifecycle Runtime Testing artifact](https://maven.google.com/web/index.html?q=lifecycle#androidx.lifecycle:lifecycle-runtime-testing)).

The best way to check if your library is testable is to try to write a sample
app with tests. Unlike regular library tests, these apps will be limited to the
public API surface of the library.

Keep in mind that a good test for a sample app should have:

*   [No side effects](#side-effects)
*   [No dependencies on time / looper (except for UI)](#external-dependencies)
*   [No private API calls](#private-apis)
*   [No assumptions on undefined library behavior](#undefined-behavior)

If you are able to write such tests for your library, you are good to go. If you
struggled or found yourself writing duplicate testing code, there is room for
improvement.

To get started with sample code, see
[Sample code in Kotlin modules](/docs/api_guidelines/index.md#sample-code-in-kotlin-modules)
for information on writing samples that can be referenced from API reference
documentation or
[Project directory structure](/docs/api_guidelines/index.md#module-structure)
for module naming guidelines if you'd like to create a basic test app.

### Avoiding side-effects {#side-effects}

#### Ensure proper scoping for your library a.k.a. Avoid Singletons

Singletons are usually bad for tests as they live across different tests,
opening the gates for possible side-effects between tests. When possible, try to
avoid using singletons. If it is not possible, consider providing a test
artifact that will reset the state of the singleton between tests.

```java {.bad}
public class JobQueue {
  public static JobQueue getInstance();
}
```

```java {.good}
public class JobQueue {
  public JobQueue();
}
```

```kotlin {.good}
object JobQueueTestUtil {
     fun createForTest(): JobQueue
     fun resetForTesting(jobQueue: JobQueue)
}
```

#### Side effects due to external resources

Sometimes, your library might be controlling resources on the device in which
case even if it is not a singleton, it might have side-effects. For instance,
Room, being a database library, inherently modifies a file on the disk. To allow
proper isolated testing, Room provides a builder option to create the database
[in memory](https://developer.android.com/reference/androidx/room/Room#inMemoryDatabaseBuilder\(android.content.Context,%20java.lang.Class%3CT%3E\))
A possible alternative solution could be to provide a test rule that will
properly close databases after each test.

```java {.good}
public class Camera {
  // Sends configuration to the camera hardware, which persists the
  // config across app restarts and applies to all camera clients.
  public void setConfiguration(Config c);

  // Retrieves the current configuration, which allows clients to
  // restore the camera hardware to its prior state after testing.
  public Config getConfiguration();
}
```

If your library needs an inherently singleton resource (e.g. `WorkManager` is a
wrapper around `JobScheduler` and there is only 1 instance of it provided by the
system), consider providing a testing artifact. To provide isolation for tests,
the WorkManager library ships a
[separate testing artifact](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/integration-testing)

### "External" dependencies {#external-dependencies}

#### Allow configuration of external resource dependencies

A common example of this use case is libraries that do multi-threaded
operations. For Kotlin libraries, this is usually achieved by receiving a
coroutine context or scope. For Java libraries, it is commonly an `Executor`. If
you have a case like this, make sure it can be passed as a parameter to your
library.

NOTE Android API Guidelines require that methods accepting a callback
[must also take an Executor](https://android.googlesource.com/platform/developers/docs/+/refs/heads/master/api-guidelines/index.md#provide-executor)

For example, the Room library allows developers to
[pass different executors](https://developer.android.com/reference/androidx/room/RoomDatabase.Builder#setQueryExecutor\(java.util.concurrent.Executor\))
for background query operations. When writing a test, developers can invoke this
with a custom executor where they can track work completion. See
[SuspendingQueryTest](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:room/integration-tests/kotlintestapp/src/androidTest/java/androidx/room/integration/kotlintestapp/test/SuspendingQueryTest.kt;l=672)
in Room's integration test app for implementation details.

```kotlin
val localDatabase = Room.inMemoryDatabaseBuilder(
    ApplicationProvider.getApplicationContext(), TestDatabase::class.java
)
    .setQueryExecutor(ArchTaskExecutor.getIOThreadExecutor())
    .setTransactionExecutor(wrappedExecutor)
    .build()

// ...

wrappedExecutor.awaitTermination(1, TimeUnit.SECONDS)
```

*   [sample test](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-master-dev:room/integration-tests/kotlintestapp/src/androidTest/java/androidx/room/integration/kotlintestapp/test/SuspendingQueryTest.kt;l=672)

If the external resource you require does not make sense as a public API, such
as a main thread executor, then you can provide a testing artifact which will
allow setting it. For example, the Lifecycle package depends on the main thread
executor to function but for an application, customizing it does not make sense
(as there is only 1 "pre-defined" main thread for an app). For testing purposes,
the Lifecycle library provides a testing artifact which includes the
[CountingTaskExecutorRule](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:arch/core/core-testing/src/main/java/androidx/arch/core/executor/testing/CountingTaskExecutorRule.java;l=36)
JUnit test rule to change them.

```kotlin
@Rule
@JvmField
val countingTaskExecutorRule = CountingTaskExecutorRule()

// ...

@After
fun teardown() {
    // At the end of all tests, query executor should
    // be idle (e.g. transaction thread released).
    countingTaskExecutorRule.drainTasks(500, TimeUnit.MILLISECONDS)
    assertThat(countingTaskExecutorRule.isIdle).isTrue()
}
```

#### Fakes for external dependencies

Sometimes, the developer might want to track side effects of your library for
end-to-end testing. For instance, if your library provides some functionality
that might decide to toggle Bluetooth -- outside developer's direct control --
it might be a good idea to have an interface for that functionality and also
provide a fake that can record such calls. If you don't think that interface
makes sense as a library configuration, you can use the
[@RestrictTo](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:annotation/annotation/src/main/java/androidx/annotation/RestrictTo.java)
annotation with scope
[LIBRARY_GROUP](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:annotation/annotation/src/main/java/androidx/annotation/RestrictTo.java;l=69)
to restrict usage of that interface to your library group and provide a testing
artifact with the fake so that developer can observe side effects only in tests
while you can avoid creating unnecessary APIs.

```kotlin
public class EndpointConnector {
  public void discoverEndpoints(Executor e, Consumer<List<Endpoint>> callback);

  @RestrictTo(Scope.LIBRARY_GROUP)
  public void setBleInterface(BleInterface bleInterface);
}

public class EndpointConnectorTestHelper {
  public void setBleInterface(EndpointConnector e, BleInterface b);
}
```

NOTE There is a fine balance between making a library configurable and making
configuration a nightmare for the developer. You should try to always have
defaults for these configurable objects to ensure your library is easy to use
while still testable when necessary.

### Avoiding the need for private API calls in tests {#private-apis}

#### Provide additional functionality for tests

There are certain situations where it could be useful to provide more APIs that
only make sense in the scope of testing. For example, a `Lifecycle` class has
methods that are bound to the main thread but developers may want to have other
tests that rely on lifecycle but do not run on the main thread (or even on an
emulator). For such cases, you may create APIs that are testing-only to allow
developers to use them only in tests while giving them the confidence that it
will behave as close as possible to a real implementation. For the case above,
`LifecycleRegistry` provides an API to
[create](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:lifecycle/lifecycle-runtime/src/main/java/androidx/lifecycle/LifecycleRegistry.java;l=334)
an instance of it that will not enforce thread restrictions.

NOTE Even though the implementation referenced above is acceptable, it is always
better to create such functionality in additional testing artifacts when
possible.

When writing Android platform APIs, testing-only APIs should be clearly
distinguished from non-test API surface and restricted as necessary to prevent
misuse. In some cases, the `@TestApi` annotation may be appropriate to restrict
usage to CTS tests; however, many platform testing APIs are also useful for app
developers.

```java {.good}
class AdSelectionManager {
  /**
   * Returns testing-specific APIs for this manager.
   *
   * @throws SecurityException when called from a non-debuggable application
   */
  public TestAdSelectionManager getTestAdSelectionManager();
}
```

### Avoiding assumptions in app code for library behavior {#undefined-behavior}

#### Provide fakes for common classes in a `-testing` artifact

In some cases, the developer might need an instance of a class provided by your
library but does not want to (or cannot) initiate it in tests. Moreover,
behavior of such classes might not be fully defined for edge cases, making it
difficult for developers to mock them.

For such cases, it is a good practice to provide a fake implementation out of
the box that can be controlled in tests. For example, the Lifecycle library
provides
[TestLifecycleOwner](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:lifecycle/lifecycle-runtime-testing/src/main/java/androidx/lifecycle/testing/TestLifecycleOwner.kt)
as a fake implementation for the `LifecycleOwner` class that can be manipulated
in tests to create different use cases.

```java
private TestLifecycleOwner mOwner = new TestLifecycleOwner(
    Lifecycle.State.INITIALIZED, new TestCoroutineDispatcher());

@Test
public void testObserverToggle() {
    Observer<String> observer = (Observer<String>) mock(Observer.class);
    mLiveData.observe(mOwner, observer);

    verify(mActiveObserversChanged, never()).onCall(anyBoolean());

    // ...
}
```

## Document how to test with your library

Even when your library is fully testable, it is often not clear for a developer
to know which APIs are safe to call in tests and when. Providing guidance on
[d.android.com](https://d.android.com) or in a
[Medium post](https://medium.com/androiddevelopers) will make it much easier for
the developer to start testing with your library.

Examples of testing guidance:

-   [Integration tests with WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager/how-to/integration-testing)
-   [Test and debug your Room database](https://developer.android.com/training/data-storage/room/testing-db)
-   [Unit-testing LiveData and other common observability problems](https://medium.com/androiddevelopers/unit-testing-livedata-and-other-common-observability-problems-bb477262eb04)

## Testability anti-patterns

When writing integration tests against your code that depends on your library,
look out for the following anti-patterns.

### Calling `Instrumentation.waitForIdleSync()` as a synchronization barrier

The `waitForIdle()` and `waitForIdleSync()` methods claim to "(Synchronously)
wait for the application to be idle." and may seem like reasonable options when
there is no obvious way to observe whether an action has completed; however,
these methods know nothing about the context of the test and return when the
main thread's message queue is empty.

```java {.bad}
view.requestKeyboardFocus();
Instrumentation.waitForIdleSync();
sendKeyEvents(view, "1234");
// There is no guarantee that `view` has keyboard focus yet.
device.pressEnter();
```

In apps with an active UI animation, the message queue is *never empty*. If the
app is waiting for a callback across IPC, the message queue may be empty despite
the test not reaching the desired state.

In some cases, `waitForIdleSync()` introduces enough of a delay that unrelated
asynchronous actions happen to have completed by the time the method returns;
however, this delay is purely coincidental and eventually leads to flakiness.

Instead, find a reliable synchronization barrier that guarantees the expected
state has been reached or the requested action has been completed. This might
mean adding listeners, callbacks, `ListenableFuture`s, or `LiveData`.

See [Asynchronous work](/docs/api_guidelines/index.md#async)
in the API Guidelines for more information on exposing the state of asynchronous
work to clients.

### Calling `Thread.sleep()` or `UiController.loopMainThreadForAtLeast()` as a synchronization barrier {#calling-threadsleep-as-a-synchronization-barrier}

`Thread.sleep()` (and `UiController.loopMainThreadForAtLeast()`) is a common
source of flakiness and instability in tests. If a developer needs to call
`Thread.sleep()` -- even indirectly via a `PollingCheck` -- to get their test
into a suitable state for checking assertions, your library needs to provide
more reliable synchronization barriers.

```java {.bad}
List<MediaItem> playlist = MediaTestUtils.createPlaylist(playlistSize);
mPlayer.setPlaylist(playlist);

// Wait some time for setting the playlist.
Thread.sleep(TIMEOUT_MS);

assertTrue(mPlayer.getPositionInPlaylist(), 0);
```

See the previous header for more information of providing synchronization
barriers.
