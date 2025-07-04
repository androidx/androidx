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
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.IAuthTabCallback;
import android.util.Log;

import androidx.annotation.RestrictTo;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.IntentCompat;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Wrapper class that can be used as a unique identifier for a session. Also contains an accessor
 * for the {@link AuthTabCallback} for the session if there was any.
 */
public final class AuthTabSessionToken {
    private static final String TAG = "AuthTabSessionToken";

    /**
     * Both {@link #mCallbackBinder} and {@link #mSessionId} are used as session ID.
     * At least one of the ID should be not null. If {@link #mSessionId} is null,
     * the session will be invalidated as soon as the client goes away.
     * Otherwise the browser will attempt to keep the session parameters,
     * but it might drop them to reclaim resources
     */
    @Nullable
    private final IAuthTabCallback mCallbackBinder;
    @Nullable
    private final PendingIntent mSessionId;
    @Nullable
    private final AuthTabCallback mCallback;

    /**
     * Constructs a new session token.
     *
     * Both {@code callbackBinder} and {@code sessionId} are used as session ID. At least one of the
     * IDs should be not null. If {@code sessionId} is null, the session will be invalidated as soon
     * as the client goes away. Otherwise the browser will attempt to keep the session parameters,
     * but it might drop them to reclaim resources.
     *
     * @param callbackBinder The {@link IAuthTabCallback} associated with this session.
     * @param sessionId      The {@link PendingIntent} associated with this session.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public AuthTabSessionToken(@Nullable IAuthTabCallback callbackBinder,
            @Nullable PendingIntent sessionId) {
        if (callbackBinder == null && sessionId == null) {
            throw new IllegalStateException(
                    "AuthTabSessionToken must have either a session id or a callback (or both).");
        }

        mCallbackBinder = callbackBinder;
        mSessionId = sessionId;

        mCallback = mCallbackBinder == null ? null : new AuthTabCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, @NonNull Bundle extras) {
                try {
                    mCallbackBinder.onNavigationEvent(navigationEvent, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during IAuthTabCallback transaction");
                }
            }

            @Override
            public void onExtraCallback(@NonNull String callbackName, @NonNull Bundle args) {
                try {
                    mCallbackBinder.onExtraCallback(callbackName, args);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during IAuthTabCallback transaction");
                }
            }

            @NonNull
            @Override
            public Bundle onExtraCallbackWithResult(@NonNull String callbackName,
                    @NonNull Bundle args) {
                try {
                    return mCallbackBinder.onExtraCallbackWithResult(callbackName, args);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during IAuthTabCallback transaction");
                    return Bundle.EMPTY;
                }
            }

            @Override
            public void onWarmupCompleted(@NonNull Bundle extras) {
                try {
                    mCallbackBinder.onWarmupCompleted(extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during IAuthTabCallback transaction");
                }
            }
        };
    }

    /**
     * Obtain an {@link AuthTabSessionToken} from an intent. See {@link AuthTabIntent.Builder}
     * for ways to generate an intent for Auth Tabs.
     *
     * @param intent The intent to generate the token from. This has to include an extra for
     *               {@link CustomTabsIntent#EXTRA_SESSION}.
     * @return The token that was generated.
     */
    public static @Nullable AuthTabSessionToken createSessionTokenFromIntent(
            @NonNull Intent intent) {
        Bundle b = intent.getExtras();
        if (b == null) return null;
        IBinder binder = b.getBinder(CustomTabsIntent.EXTRA_SESSION);
        PendingIntent sessionId = IntentCompat.getParcelableExtra(intent,
                CustomTabsIntent.EXTRA_SESSION_ID, PendingIntent.class);
        if (binder == null && sessionId == null) return null;
        IAuthTabCallback callback = binder == null ? null : IAuthTabCallback.Stub.asInterface(
                binder);
        return new AuthTabSessionToken(callback, sessionId);
    }

    /**
     * @return {@link AuthTabCallback} corresponding to this session if there was any non-null
     * callbacks passed by the client.
     */
    @Nullable
    public AuthTabCallback getCallback() {
        return mCallback;
    }

    /**
     * @return The callback binder corresponding to this session, null if there is none.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Nullable
    public IBinder getCallbackBinder() {
        if (mCallbackBinder == null) return null;
        return mCallbackBinder.asBinder();
    }

    /** @return The session id corresponding to this session, null if there is none. */
    @Nullable
    public PendingIntent getId() {
        return mSessionId;
    }

    /** @return Whether this token is associated with a session id. */
    public boolean hasId() {
        return mSessionId != null;
    }

    @Override
    public int hashCode() {
        if (mSessionId != null) return mSessionId.hashCode();

        return getCallbackBinderAssertNotNull().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthTabSessionToken)) return false;
        AuthTabSessionToken other = (AuthTabSessionToken) o;

        PendingIntent otherSessionId = other.getId();
        // If one object has a session id and the other one doesn't, they're not equal.
        if ((mSessionId == null) != (otherSessionId == null)) return false;

        // If both objects have an id, check that they are equal.
        if (mSessionId != null) return mSessionId.equals(otherSessionId);

        // Otherwise check for binder equality.
        return getCallbackBinderAssertNotNull().equals(other.getCallbackBinderAssertNotNull());
    }

    private IBinder getCallbackBinderAssertNotNull() {
        if (mCallbackBinder == null) {
            throw new IllegalStateException(
                    "AuthTabSessionToken must have valid binder or pending session");
        }
        return mCallbackBinder.asBinder();
    }

    /**
     * @return Whether this token is associated with the given session.
     */
    public boolean isAssociatedWith(@NonNull AuthTabSession session) {
        return session.getBinder().equals(mCallbackBinder);
    }

    static class MockCallback extends IAuthTabCallback.Stub {
        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) throws RemoteException {
        }

        @Override
        public void onExtraCallback(String callbackName, Bundle args) throws RemoteException {
        }

        @Override
        public Bundle onExtraCallbackWithResult(String callbackName, Bundle args)
                throws RemoteException {
            return Bundle.EMPTY;
        }

        @Override
        public void onWarmupCompleted(Bundle extras) throws RemoteException {
        }
    }
}
