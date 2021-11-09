# Remote Callbacks Implementation Details

Remote Callbacks do not create a new technology or aim to have any complex
logic in them. They only aim to make the experience of PendingIntents cleaner
and easier for developers.

## Handling calls

Remote callbacks tries hard to avoid reflection in its implementation, and
does not trigger reflection during a repeated callback creations or executions.
The only reflection is used to initialize the handlers for a given receiver
class, so once per class.

For any class $S that is a CallbackReceiver and has callbacks, the compiler will
generate a class called $SInitializer which is a stub extending $S. The
CallbackHandlerRegistry will instantiate/run this initializer the first time it
encounters $S, and that code will in turn register a bunch of CallbackHandlers
that know how to translate from a Bundle so they can be shoved through
PendingIntents (or other IPC mechanisms).

```java
/**
 * The interface used to trigger a callback when the pending intent is fired.
 * Note: This should only be referenced by generated code, there is no reason to reference
 * this otherwise.
 * @param <T> The receiver type for this callback handler.
 */
public interface CallbackHandler<T extends CallbackReceiver> {
    /**
     * Executes a callback given a Bundle of aurgements.
     * Note: This should only be called by generated code, there is no reason to reference this
     * otherwise.
     */
    void executeCallback(T receiver, Bundle arguments);
}
```

The CallbackHandlerRegistry keeps a map of a handler for all methods/classes that are
`@RemoteCallable` that have been initalized. Whenever a call to
`createRemoteCallback` happens, It looks up a stub implementation of the class
that has generated code that will translate the arguments passed in to
a bundle that can be used in the PendingIntent.

Similarly when a callback is triggered, the concrete implementation of
`CallbackReceiver` (e.g. BroadcastReceiverWithCallbacks) gets a PendingIntent
that it knows to be a callback, it triggers the CallbackHandlerRegistry, which
looks up the corresponding handler and triggers `executeCallback` which will
call the actual implementation of the method.

This handling of callbacks means we don't have to blindly keep all methods
related to callbacks. Only the classname (which is needed for the Manifest
anyway). Internal methods can be obfuscated and optimized however proguard or
other tools see fit without causing issues with the callbacks.

## Inputs

In some cases an input could be provided by the calling app, such as a
notification with inline reply, or when a slider is dragged in a slice. Remote
Callbacks handle these through the ExternalInput. An ExternalInput
is an annotation that can take a parameter on RemoteCallable method.
This enforces an explicit opt-in to receive data from another app.

The reason inputs can easily form a security hole is that the input must be
blank in the incoming pending intent extras, and therefore can easily be
set by the calling app. So they should only be set in cases where the app
wants external data and not for cases where the app wants to rename bundle keys
or something similar.

Now back to a reasonable use case, these will act as constants that just hold
the place of the input argument when creating the callback.

```java
@RemoteCallable
public RemoteCallback setSliderPositon(@ExternalInput(SLIDER_VALUE) int value) {
    ...
    return RemoteCallback.LOCAL;
}

createRemoteCallback(context).setSliderPosition(0 /* ignored */)
    .toPendingIntent();
```

## Providers

Since providers don't actually have a form of a PendingIntent, RemoteCallbacks
includes a broadcast stub that acts as a relay for this case, which receives
the intent, then triggers a call on the corresponding authority.

Since slices will already have access to the provider directly, this relay step
can be optimized out in newer clients, but will still operate with the relay in
the case of older clients that expect a PendingIntent.

## Services

Coming soon, they are on the TODO list.
