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

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Looper;
import android.os.Message;
import android.view.View;

import androidx.annotation.DoNotInline;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Helper for accessing features in {@link Message}.
 */
public final class MessageCompat {
    /**
     * False when linking of the hidden setAsynchronous method on pre-22 has previously failed.
     * Not volatile because we don't care if multiple threads make an attempt.
     */
    private static boolean sTrySetAsynchronous = true;
    /**
     * False when linking of the hidden isAsynchronous method on pre-22 has previously failed.
     * Not volatile because we don't care if multiple threads make an attempt.
     */
    private static boolean sTryIsAsynchronous = true;

    /**
     * Sets whether the message is asynchronous, meaning that it is not
     * subject to {@link Looper} synchronization barriers.
     * <p>
     * Certain operations, such as view invalidation, may introduce synchronization
     * barriers into the {@link Looper}'s message queue to prevent subsequent messages
     * from being delivered until some condition is met.  In the case of view invalidation,
     * messages which are posted after a call to {@link View#invalidate}
     * are suspended by means of a synchronization barrier until the next frame is
     * ready to be drawn.  The synchronization barrier ensures that the invalidation
     * request is completely handled before resuming.
     * <p>
     * Asynchronous messages are exempt from synchronization barriers.  They typically
     * represent interrupts, input events, and other signals that must be handled independently
     * even while other work has been suspended.
     * <p>
     * Note that asynchronous messages may be delivered out of order with respect to
     * synchronous messages although they are always delivered in order among themselves.
     * If the relative order of these messages matters then they probably should not be
     * asynchronous in the first place.  Use with caution.
     * <p>
     * This API has no effect prior to API 16.
     *
     * @param async True if the message is asynchronous.
     *
     * @see #isAsynchronous(Message)
     * @see Message#setAsynchronous(boolean)
     */
    @SuppressLint("NewApi")
    public static void setAsynchronous(@NonNull Message message, boolean async) {
        if (Build.VERSION.SDK_INT >= 22) {
            Api22Impl.setAsynchronous(message, async);
            return;
        }
        if (sTrySetAsynchronous && Build.VERSION.SDK_INT >= 16) {
            // Since this was an @hide method made public, we can link directly against it with a
            // try/catch for its absence instead of doing the same dance through reflection.
            try {
                Api22Impl.setAsynchronous(message, async);
            } catch (NoSuchMethodError e) {
                sTrySetAsynchronous = false;
            }
        }
    }

    /**
     * Returns true if the message is asynchronous, meaning that it is not
     * subject to {@link Looper} synchronization barriers.
     *
     * @return True if the message is asynchronous. Always false prior to API 16.
     *
     * @see #setAsynchronous(Message, boolean)
     * @see Message#isAsynchronous()
     */
    @SuppressLint("NewApi")
    public static boolean isAsynchronous(@NonNull Message message) {
        if (Build.VERSION.SDK_INT >= 22) {
            return Api22Impl.isAsynchronous(message);
        }
        if (sTryIsAsynchronous && Build.VERSION.SDK_INT >= 16) {
            // Since this was an @hide method made public, we can link directly against it with a
            // try/catch for its absence instead of doing the same dance through reflection.
            try {
                return Api22Impl.isAsynchronous(message);
            } catch (NoSuchMethodError e) {
                sTryIsAsynchronous = false;
            }
        }
        return false;
    }

    private MessageCompat() {
    }

    @RequiresApi(22)
    static class Api22Impl {
        private Api22Impl() {
            // This class is not instantiable.
        }

        @DoNotInline
        static boolean isAsynchronous(Message message) {
            return message.isAsynchronous();
        }

        @DoNotInline
        static void setAsynchronous(Message message, boolean async) {
            message.setAsynchronous(async);
        }
    }
}
