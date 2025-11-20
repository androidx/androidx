/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.core.app;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.IntDef;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.CountDownLatch;

/** Helper for accessing features in {@link PendingIntent}. */
public final class PendingIntentCompat {

    @IntDef(
            flag = true,
            value = {
                PendingIntent.FLAG_ONE_SHOT,
                PendingIntent.FLAG_NO_CREATE,
                PendingIntent.FLAG_CANCEL_CURRENT,
                PendingIntent.FLAG_UPDATE_CURRENT,
                Intent.FILL_IN_ACTION,
                Intent.FILL_IN_DATA,
                Intent.FILL_IN_CATEGORIES,
                Intent.FILL_IN_COMPONENT,
                Intent.FILL_IN_PACKAGE,
                Intent.FILL_IN_SOURCE_BOUNDS,
                Intent.FILL_IN_SELECTOR,
                Intent.FILL_IN_CLIP_DATA
            })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public @interface Flags {}

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getActivities(Context, int, Intent[], int, Bundle)}.
     */
    public static @NonNull PendingIntent getActivities(
            @NonNull Context context,
            int requestCode,
            @SuppressLint("ArrayReturn") Intent @NonNull [] intents,
            @Flags int flags,
            @Nullable Bundle options,
            boolean isMutable) {
        return PendingIntent.getActivities(context, requestCode, intents,
                addMutabilityFlags(isMutable, flags), options);
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getActivities(Context, int, Intent[], int, Bundle)}.
     */
    public static @NonNull PendingIntent getActivities(
            @NonNull Context context,
            int requestCode,
            @SuppressLint("ArrayReturn") Intent @NonNull [] intents,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getActivities(
                context, requestCode, intents, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary.
     *
     * @return Returns an existing or new PendingIntent matching the given parameters. May return
     *         {@code null} only if {@link PendingIntent#FLAG_NO_CREATE} has been supplied.
     * @see PendingIntent#getActivity(Context, int, Intent, int)
     */
    public static @Nullable PendingIntent getActivity(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getActivity(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary.
     *
     * @return Returns an existing or new PendingIntent matching the given parameters. May return
     *         {@code null} only if {@link PendingIntent#FLAG_NO_CREATE} has been supplied.
     * @see PendingIntent#getActivity(Context, int, Intent, int, Bundle)
     */
    public static @Nullable PendingIntent getActivity(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            @Nullable Bundle options,
            boolean isMutable) {
        return PendingIntent.getActivity(context, requestCode, intent,
                addMutabilityFlags(isMutable, flags), options);
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary.
     *
     * @return Returns an existing or new PendingIntent matching the given parameters. May return
     *         {@code null} only if {@link PendingIntent#FLAG_NO_CREATE} has been supplied.
     * @see PendingIntent#getBroadcast(Context, int, Intent, int)
     */
    public static @Nullable PendingIntent getBroadcast(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getBroadcast(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary. See {@link
     * PendingIntent#getForegroundService(Context, int, Intent, int)} .
     */
    @RequiresApi(26)
    public static @NonNull PendingIntent getForegroundService(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return Api26Impl.getForegroundService(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * Retrieves a {@link PendingIntent} with mandatory mutability flag set on supported platform
     * versions. The caller provides the flag as combination of all the other values except
     * mutability flag. This method combines mutability flag when necessary.
     *
     * @return Returns an existing or new PendingIntent matching the given parameters. May return
     *         {@code null} only if {@link PendingIntent#FLAG_NO_CREATE} has been supplied.
     * @see PendingIntent#getService(Context, int, Intent, int)
     */
    public static @Nullable PendingIntent getService(
            @NonNull Context context,
            int requestCode,
            @NonNull Intent intent,
            @Flags int flags,
            boolean isMutable) {
        return PendingIntent.getService(
                context, requestCode, intent, addMutabilityFlags(isMutable, flags));
    }

    /**
     * {@link PendingIntent#send()} variants that support {@link PendingIntent.OnFinished} callbacks
     * have a bug on many API levels that the callback may be invoked even if the PendingIntent was
     * never sent (ie, such as if the PendingIntent was canceled, and the send() invocation threw a
     * {@link PendingIntent.CanceledException}). Using this compatibility method fixes that bug and
     * guarantees that {@link PendingIntent.OnFinished} callbacks will only be invoked if send()
     * completed successfully.
     *
     * <p>See {@link PendingIntent#send(int, PendingIntent.OnFinished, Handler)}.
     */
    @SuppressLint("LambdaLast")  // compat shim so arguments should be in the same order
    public static void send(
            @NonNull PendingIntent pendingIntent,
            int code,
            PendingIntent.@Nullable OnFinished onFinished,
            @Nullable Handler handler) throws PendingIntent.CanceledException {
        try (GatedCallback gatedCallback = new GatedCallback(onFinished)) {
            pendingIntent.send(code, gatedCallback.getCallback(), handler);
            gatedCallback.complete();
        }
    }

    /**
     * {@link PendingIntent#send()} variants that support {@link PendingIntent.OnFinished} callbacks
     * have a bug on many API levels that the callback may be invoked even if the PendingIntent was
     * never sent (ie, such as if the PendingIntent was canceled, and the send() invocation threw a
     * {@link PendingIntent.CanceledException}). Using this compatibility method fixes that bug and
     * guarantees that {@link PendingIntent.OnFinished} callbacks will only be invoked if send()
     * completed successfully.
     *
     * <p>See {@link PendingIntent#send(Context, int, Intent, PendingIntent.OnFinished, Handler)}.
     */
    @SuppressLint("LambdaLast")  // compat shim so arguments must be in the same order
    public static void send(
            @NonNull PendingIntent pendingIntent,
            // compat shim so arguments must be in the same order
            @SuppressLint("ContextFirst") @NonNull Context context,
            int code,
            @NonNull Intent intent,
            PendingIntent.@Nullable OnFinished onFinished,
            @Nullable Handler handler) throws PendingIntent.CanceledException {
        send(pendingIntent, context, code, intent, onFinished, handler, null, null);
    }

    /**
     * {@link PendingIntent#send()} variants that support {@link PendingIntent.OnFinished} callbacks
     * have a bug on many API levels that the callback may be invoked even if the PendingIntent was
     * never sent (ie, such as if the PendingIntent was canceled, and the send() invocation threw a
     * {@link PendingIntent.CanceledException}). Using this compatibility method fixes that bug and
     * guarantees that {@link PendingIntent.OnFinished} callbacks will only be invoked if send()
     * completed successfully.
     *
     * <p>See {@link
     * PendingIntent#send(Context, int, Intent, PendingIntent.OnFinished, Handler, String, Bundle)}
     */
    @SuppressLint("LambdaLast")  // compat shim so arguments must be in the same order
    public static void send(
            @NonNull PendingIntent pendingIntent,
            // compat shim so arguments must be in the same order
            @SuppressLint("ContextFirst") @NonNull Context context,
            int code,
            @NonNull Intent intent,
            PendingIntent.@Nullable OnFinished onFinished,
            @Nullable Handler handler,
            @Nullable String requiredPermissions,
            @Nullable Bundle options) throws PendingIntent.CanceledException {
        try (GatedCallback gatedCallback = new GatedCallback(onFinished)) {
            pendingIntent.send(
                context,
                code,
                intent,
                onFinished,
                handler,
                requiredPermissions,
                options);
            gatedCallback.complete();
        }
    }

    static int addMutabilityFlags(boolean isMutable, int flags) {
        if (isMutable) {
            if (VERSION.SDK_INT >= 31) {
                flags |= FLAG_MUTABLE;
            }
        } else {
            flags |= FLAG_IMMUTABLE;
        }

        return flags;
    }

    private PendingIntentCompat() {}

    @RequiresApi(26)
    private static class Api26Impl {
        private Api26Impl() {}

        public static PendingIntent getForegroundService(
                Context context, int requestCode, Intent intent, int flags) {
            return PendingIntent.getForegroundService(context, requestCode, intent, flags);
        }
    }

    // see b/201299281 for more info and context
    private static class GatedCallback implements Closeable {

        private final CountDownLatch mComplete = new CountDownLatch(1);

        private PendingIntent.@Nullable OnFinished mCallback;
        private boolean mSuccess;

        GatedCallback(PendingIntent.@Nullable OnFinished callback) {
            this.mCallback = callback;
            mSuccess = false;
        }

        public PendingIntent.@Nullable OnFinished getCallback() {
            if (mCallback == null) {
                return null;
            } else {
                return this::onSendFinished;
            }
        }

        public void complete() {
            mSuccess = true;
        }

        @Override
        public void close() {
            if (!mSuccess) {
                mCallback = null;
            }
            mComplete.countDown();
        }

        private void onSendFinished(
                PendingIntent pendingIntent,
                Intent intent,
                int resultCode,
                String resultData,
                Bundle resultExtras) {
            boolean interrupted = false;
            try {
                while (true) {
                    try {
                        mComplete.await();
                        break;
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }

            if (mCallback != null) {
                mCallback.onSendFinished(
                        pendingIntent,
                        intent,
                        resultCode,
                        resultData,
                        resultExtras);
                mCallback = null;
            }
        }
    }
}
