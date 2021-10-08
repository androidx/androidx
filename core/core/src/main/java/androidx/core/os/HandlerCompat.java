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

package androidx.core.os;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Helper for accessing features in {@link Handler}.
 */
public final class HandlerCompat {
    private static final String TAG = "HandlerCompat";

    /**
     * Create a new Handler whose posted messages and runnables are not subject to
     * synchronization barriers such as display vsync.
     *
     * <p>Messages sent to an async handler are guaranteed to be ordered with respect to one
     * another, but not necessarily with respect to messages from other Handlers.</p>
     *
     * @see Handler#createAsync(Looper, Handler.Callback) to create an async Handler with custom
     * message handling.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 28 and above, this method matches platform behavior.
     * <li>SDK 17 through 27, this method attempts to call the platform API via reflection, but
     * may fail and return a synchronous handler instance.
     * <li>Below SDK 17, this method will always return a synchronous handler instance.
     * </ul>
     *
     * @param looper the Looper that the new Handler should be bound to
     * @return a new async Handler instance
     * @see Handler#createAsync(Looper)
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    @NonNull
    public static Handler createAsync(@NonNull Looper looper) {
        Exception wrappedException;

        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.createAsync(looper);
        } else if (Build.VERSION.SDK_INT >= 17) {
            try {
                // This constructor was added as private in JB MR1:
                // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/jb-mr1-release/core/java/android/os/Handler.java
                return Handler.class.getDeclaredConstructor(Looper.class, Handler.Callback.class,
                        boolean.class)
                        .newInstance(looper, null, true);
            } catch (IllegalAccessException e) {
                wrappedException = e;
            } catch (InstantiationException e) {
                wrappedException = e;
            } catch (NoSuchMethodException e) {
                wrappedException = e;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                }
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                throw new RuntimeException(cause);
            }
            // This is a non-fatal failure, but it affects behavior and may be relevant when
            // investigating issue reports.
            Log.w(TAG, "Unable to invoke Handler(Looper, Callback, boolean) constructor",
                    wrappedException);
        }
        return new Handler(looper);
    }

    /**
     * Create a new Handler whose posted messages and runnables are not subject to
     * synchronization barriers such as display vsync.
     *
     * <p>Messages sent to an async handler are guaranteed to be ordered with respect to one
     * another, but not necessarily with respect to messages from other Handlers.</p>
     *
     * @see #createAsync(Looper) to create an async Handler without custom message handling.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 28 and above, this method matches platform behavior.
     * <li>SDK 17 through 27, this method attempts to call the platform API via reflection, but
     * may fail and return a synchronous handler instance.
     * <li>Below SDK 17, this method will always return a synchronous handler instance.
     * </ul>
     *
     * @param looper the Looper that the new Handler should be bound to
     * @return a new async Handler instance
     * @see Handler#createAsync(Looper, Handler.Callback)
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    @NonNull
    public static Handler createAsync(@NonNull Looper looper, @NonNull Handler.Callback callback) {
        Exception wrappedException;

        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.createAsync(looper, callback);
        } else if (Build.VERSION.SDK_INT >= 17) {
            try {
                // This constructor was added as private API in JB MR1:
                // https://android.googlesource.com/platform/frameworks/base/+/refs/heads/jb-mr1-release/core/java/android/os/Handler.java
                return Handler.class.getDeclaredConstructor(Looper.class, Handler.Callback.class,
                        boolean.class)
                        .newInstance(looper, callback, true);
            } catch (IllegalAccessException e) {
                wrappedException = e;
            } catch (InstantiationException e) {
                wrappedException = e;
            } catch (NoSuchMethodException e) {
                wrappedException = e;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                }
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                throw new RuntimeException(cause);
            }
            // This is a non-fatal failure, but it affects behavior and may be relevant when
            // investigating issue reports.
            Log.w(TAG, "Unable to invoke Handler(Looper, Callback, boolean) constructor",
                    wrappedException);
        }
        return new Handler(looper, callback);
    }

    /**
     * Causes the Runnable r to be added to the message queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the thread to which this handler
     * is attached.
     * <b>The time-base is {@link android.os.SystemClock#uptimeMillis}.</b>
     * Time spent in deep sleep will add an additional delay to execution.
     *
     * @param r The Runnable that will be executed.
     * @param token An instance which can be used to cancel {@code r} via
     *         {@link Handler#removeCallbacksAndMessages}.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     *
     * @return Returns true if the Runnable was successfully placed in to the
     *         message queue.  Returns false on failure, usually because the
     *         looper processing the message queue is exiting.  Note that a
     *         result of true does not mean the Runnable will be processed --
     *         if the looper is quit before the delivery time of the message
     *         occurs then the message will be dropped.
     *
     * @see Handler#postDelayed(Runnable, Object, long)
     */
    public static boolean postDelayed(@NonNull Handler handler, @NonNull Runnable r,
            @Nullable Object token, long delayMillis) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.postDelayed(handler, r, token, delayMillis);
        }

        Message message = Message.obtain(handler, r);
        message.obj = token;
        return handler.sendMessageDelayed(message, delayMillis);
    }

    /**
     * Checks if there are any pending posts of messages with callback {@code r} in
     * the message queue.
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 29 and above, this method matches platform behavior.
     * <li>SDK 16 through 28, this method attempts to call the platform API via reflection, but
     * will throw an unchecked exception if the method has been altered from the AOSP
     * implementation and cannot be called. This is unlikely, but there is no safe fallback case
     * for this method and we must throw an exception as a result.
     * </ul>
     *
     * @param handler handler on which to call the method
     * @param r callback to look for in the message queue
     * @return {@code true} if the callback is in the message queue
     * @see Handler#hasCallbacks(Runnable)
     */
    @RequiresApi(16)
    public static boolean hasCallbacks(@NonNull Handler handler, @NonNull Runnable r) {
        Exception wrappedException = null;

        if (Build.VERSION.SDK_INT >= 29) {
            return Api29Impl.hasCallbacks(handler, r);
        } else if (Build.VERSION.SDK_INT >= 16) {
            // The method signature didn't change when it was made public in SDK 29, but use
            // reflection so that we don't cause a verification error or NotFound exception if an
            // OEM changed something.
            try {
                Method hasCallbacksMethod = Handler.class.getMethod("hasCallbacks", Runnable.class);
                //noinspection ConstantConditions
                return (boolean) hasCallbacksMethod.invoke(handler, r);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw ((RuntimeException) cause);
                }
                if (cause instanceof Error) {
                    throw ((Error) cause);
                }
                throw new RuntimeException(cause);
            } catch (IllegalAccessException e) {
                wrappedException = e;
            } catch (NoSuchMethodException e) {
                wrappedException = e;
            } catch (NullPointerException e) {
                wrappedException = e;
            }
        }

        throw new UnsupportedOperationException("Failed to call Handler.hasCallbacks(), but there"
                + " is no safe failure mode for this method. Raising exception.", wrappedException);
    }

    private HandlerCompat() {
        // Non-instantiable.
    }

    @RequiresApi(29)
    private static class Api29Impl {
        private Api29Impl() {
            // Non-instantiable.
        }

        public static boolean hasCallbacks(Handler handler, Runnable r) {
            return handler.hasCallbacks(r);
        }
    }

    @RequiresApi(28)
    private static class Api28Impl {
        private Api28Impl() {
            // Non-instantiable.
        }

        public static Handler createAsync(Looper looper) {
            return Handler.createAsync(looper);
        }

        public static Handler createAsync(Looper looper, Handler.Callback callback) {
            return Handler.createAsync(looper, callback);
        }

        public static boolean postDelayed(Handler handler, Runnable r, Object token,
                long delayMillis) {
            return handler.postDelayed(r, token, delayMillis);
        }
    }
}
