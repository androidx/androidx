/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.browser.auth;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.os.IBinder;
import android.support.customtabs.IAuthTabCallback;

import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.ExperimentalPendingSession;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * A class to be used for AuthTab related communication. Clients that want to launch Auth Tabs can
 * use this class exclusively to handle all related communication.
 *
 * Use {@link CustomTabsClient#newAuthTabSession} to create an instance.
 */
public final class AuthTabSession {
    private final IAuthTabCallback mCallback;
    private final ComponentName mComponentName;

    /**
     * The session ID represented by {@link PendingIntent}. Other apps cannot forge
     * {@link PendingIntent}. The {@link PendingIntent#equals(Object)} method considers two
     * {@link PendingIntent} objects equal if their action, data, type, class and category are the
     * same (even across a process being killed).
     *
     * {@see Intent#filterEquals()}
     */
    @Nullable
    private final PendingIntent mId;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public AuthTabSession(@NonNull IAuthTabCallback callback, @NonNull ComponentName componentName,
            @Nullable PendingIntent sessionId) {
        mCallback = callback;
        mComponentName = componentName;
        mId = sessionId;
    }

    /* package */ IBinder getBinder() {
        return mCallback.asBinder();
    }

    /* package */ ComponentName getComponentName() {
        return mComponentName;
    }

    @Nullable
        /* package */ PendingIntent getId() {
        return mId;
    }

    /**
     * A class to be used instead of {@link AuthTabSession} when an Auth Tab is launched before a
     * Service connection is established.
     *
     * Use {@link CustomTabsClient#createPendingAuthTabSession} to create an instance.
     * Use {@link CustomTabsClient#attachAuthTabSession(PendingSession)} to get an
     * {@link AuthTabSession}.
     */
    @ExperimentalPendingSession
    public static class PendingSession {
        @Nullable
        private final PendingIntent mId;
        @Nullable
        private final Executor mExecutor;
        @Nullable
        private final AuthTabCallback mCallback;

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public PendingSession(@Nullable PendingIntent sessionId, @Nullable Executor executor,
                @Nullable AuthTabCallback callback) {
            mId = sessionId;
            mExecutor = executor;
            mCallback = callback;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Nullable
        public PendingIntent getId() {
            return mId;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Nullable
        public Executor getExecutor() {
            return mExecutor;
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @Nullable
        public AuthTabCallback getCallback() {
            return mCallback;
        }
    }
}
