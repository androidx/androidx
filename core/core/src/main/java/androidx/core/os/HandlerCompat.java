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

import java.lang.reflect.InvocationTargetException;

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
     * @see #createAsync(Looper, Callback) to create an async Handler with custom message handling.
     *
     * @param looper the Looper that the new Handler should be bound to
     * @return a new async Handler instance
     * @see Handler#createAsync(Looper)
     */
    @NonNull
    public static Handler createAsync(@NonNull Looper looper) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Handler.createAsync(looper);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                return Handler.class.getDeclaredConstructor(Looper.class, Handler.Callback.class,
                        boolean.class)
                        .newInstance(looper, null, true);
            } catch (IllegalAccessException ignored) {
            } catch (InstantiationException ignored) {
            } catch (NoSuchMethodException ignored) {
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
            Log.v(TAG, "Unable to invoke Handler(Looper, Callback, boolean) constructor");
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
     * @param looper the Looper that the new Handler should be bound to
     * @return a new async Handler instance
     * @see Handler#createAsync(Looper, Callback)
     */
    @NonNull
    public static Handler createAsync(@NonNull Looper looper, @NonNull Handler.Callback callback) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Handler.createAsync(looper, callback);
        }
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                return Handler.class.getDeclaredConstructor(Looper.class, Handler.Callback.class,
                        boolean.class)
                        .newInstance(looper, callback, true);
            } catch (IllegalAccessException ignored) {
            } catch (InstantiationException ignored) {
            } catch (NoSuchMethodException ignored) {
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
            Log.v(TAG, "Unable to invoke Handler(Looper, Callback, boolean) constructor");
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
            return handler.postDelayed(r, token, delayMillis);
        }

        Message message = Message.obtain(handler, r);
        message.obj = token;
        return handler.sendMessageDelayed(message, delayMillis);
    }

    private HandlerCompat() {
    }
}
