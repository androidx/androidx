# Do Not Mock

All APIs created in Jetpack **must have a testing story**: how developers should
write tests for their code that relies on a library, this story should not be
"use Mockito to mock class `Foo`". Your goal as an API owner is to **create
better alternatives** to mocking.

## Why can't I suggest mocks as testing strategy?

Frequently, mocks don't follow guarantees outlined in the API they mock. That
leads to:

*   Significant difference in the behavior that diminishes test value.
*   Brittle tests, that make hard to evolve both apps and libraries, because new
    code may start to rely on the guarantees broken in a mock. Let's take a look
    at a simplified example. So, let's say you mocked a bundle and getString in
    it:

    ```java
    Bundle mock = mock(Bundle.class);
    when(mock.getString("key")).thenReturn("result");
    ```

    But you don't mock it to simply call `getString()` in your test. A goal is
    not to test a mock, the goal is always to test your app code, so your app
    code always interacts with a mock in some way:

    ```java
    Bundle bundle = mock(Bundle.class);
    when(mock.getString("key")).thenReturn("result");
    mycomponent.consume(bundle)
    ```

    Originally the test worked fine, but over time `component.consume` is
    evolving, and, for example, it may start to call `containsKey` on the given
    bundle. But our test passes a mock that don't expect such call and, boom,
    test is broken. However, component code is completely valid and has nothing
    to do with the broken test. We observed a lot of issues like that during
    updates of Android SDK and Jetpack libraries to newer versions internally at
    google. Suggesting to mock our own components is shooting ourselves in the
    foot, it will make adoption of newer version of libraries even slower.

*   Messy tests. It always starts with simple mock with one method, but then
    this mock grows with the project, and as a result test code has sub-optimal
    half-baked class implementation of on top of the mock.

## But it is okay to mock interfaces, right?

It depends. There are interfaces that don't imply any behavior guarantees and
they are ok to be mocked. However, **not all** interfaces are like that: for
example, `Map` is an interface but it has a lot of contracts required from
correct implementation. Examples of interfaces that are ok to mock are callback
interfaces in general, for example: `View.OnClickListener`, `Runnable`.

## What about spying?

Spying on these classes is banned as well - Mockito spies permit stubbing of
methods just like mocks do, and interaction verification is brittle and
unnecessary for these classes. Rather than verifying an interaction with a
class, developers should observe the result of an interaction - the effect of a
task submitted to an `Executor`, or the presence of a fragment added to your
layout. If an API in your library misses a way to have such checks, you should
add methods to do that.

## Avoid Mockito in your own tests.

One of the things that would help you to identify if your library is testable
without Mockito is not using Mockito yourself. Yes, historically we heavily
relied on Mockito ourselves and old tests are not rewritten, but new tests
shouldn't follow up that and should take as an example good citizens, for
example, `-ktx` modules. These modules don't rely on Mockito and have concise
expressive tests.

One of the popular and legit patterns for Mockito usage were tests that verify
that a simple callback-like interface receives correct parameters.

```java
class MyApi {
   interface Callback {
     void onFoo(Value value);
  }
  void foo() { … }
  void registerFooCallback(Callback callback) {...}
}
```

In API like the one above, in Java 7 tests for value received in `Callback`
tended to become very wordy without Mockito. But now in your tests you can use
Kotlin and test will be as short as with Mockito:

```kotlin
fun test() {
    var receivedValue = null
    myApi.registerCallback { value -> receivedValue = value }
    myApi.foo()
   // verify receivedValue
}
```

## Don't compromise in API to enable Mockito

Mockito on Android
[had an issue](https://github.com/Mockito/Mockito/issues/1173) with mocking
final classes. Moreover, internally at Google this feature is disabled even for
non-Android code. So you may hear complaints that some of your classes are not
mockable, however **it is not a reason for open up a class for extension**. What
you should instead is verify that is possible to write the same test without
mocking, if not, again you should **provide better alternative in your API**.

## How do I approach testing story for my API?

Best way is to step into developer's shoes and write a sample app that is a
showcase for your API, then go to the next step - test that code also. If you
are able to implement tests for your demo app, then users of your API will also
be able to implement tests for functionalities where your API is also used.
