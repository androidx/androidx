/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.remotecallback;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.RestrictTo;

/**
 * An objects that can receive remote callbacks.
 * <p>
 * Remote callbacks provide an easy way to bundle arguments and pass them
 * directly into a method rather than managing PendingIntents manually.
 * <p>
 * Example:
 * <pre>public class MyReceiver extends BroadcastReceiverWithCallbacks {
 *   public PendingIntent getPendingIntent(Context context, int value1, int value2) {
 *     return createRemoteCallback().doMyAction(value1, value2)
 *         .toPendingIntent(context);
 *   }
 *
 *   \@RemoteCallable
 *   public MyReceiver doMyAction(int value1, int value2) {
 *     ...
 *     return this;
 *   }
 * }</pre>
 * <p>
 * The following types are supported as parameter types for methods tagged
 * with {@link RemoteCallable}.
 * <ul>
 * <li>byte/Byte/byte[]</li>
 * <li>char/Character/char[]</li>
 * <li>short/Short/short[]</li>
 * <li>int/Integer/int[]</li>
 * <li>long/Long/long[]</li>
 * <li>float/Float/float[]</li>
 * <li>double/Double/double[]</li>
 * <li>boolean/Boolean/boolean[]</li>
 * <li>String/String[]</li>
 * <li>Uri</li>
 * <li>Context *</li>
 * </ul>
 * * Context is a special kind of parameter, in that it cannot be specified
 *   during createRemoteCallback, it instead is passed directly through to
 *   provide a valid context at the time of the callback in case no other one
 *   is available.
 * <p>
 * This interface shouldn't be implemented in apps, instead extend one of
 * the implementations of it provided.
 * <ul>
 * <li>{@link BroadcastReceiverWithCallbacks}</li>
 * <li>{@link AppWidgetProviderWithCallbacks}</li>
 * <li>{@link ContentProviderWithCallbacks}</li>
 * </ul>
 * <p>
 * Just like PendingIntents, Remote Callbacks don't require components be
 * exported. They also ensure that all parameters always have a value in the
 * PendingIntent generated, which ensures that the caller cannot inject new values
 * except when explicitly requested by the receiving app. They also generate the
 * intent Uris to ensure that the callbacks stay separate and don't collide with
 * each other.
 *
 * @param <T> Should be specified as the root class (e.g. class X extends
 *           CallbackReceiver\<X>)
 *
 * @see RemoteCallable
 * @see ExternalInput
 */
public interface CallbackReceiver<T> {

    /**
     * Creates a {@link RemoteCallback} that will call the method with method
     * specified with the arguments specified when triggered. Only methods
     * tagged with {@link RemoteCallable} can be used here.
     *
     * This method returns a stub implementation of the class calling it to
     * record the arguments/method being used. This should only be used in a
     * chain of 2 calls, starting with createRemoteCallback(), then followed
     * up with a call to any method tagged with {@link RemoteCallable}.
     *
     * <pre>
     *     createRemoteCallback().callMyMethod("My arguments", 43, 2.4)
     *              .toPendingIntent(context);
     * </pre>
     *
     * <pre>
     *     \@RemoteCallable
     *     public RemoteCallback callMyMethod(String argStr, int argInt, double argDouble) {
     *         ...
     *         return RemoteCallback.LOCAL;
     *     }
     * </pre>
     */
    T createRemoteCallback(Context context);

    /**
     * Generates a {@link RemoteCallback} when a RemoteCallback is being triggered, should only
     * be used in the context on {@link #createRemoteCallback}.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    RemoteCallback toRemoteCallback(Class<T> cls, Context context, String authority, Bundle args,
            String method);
}
