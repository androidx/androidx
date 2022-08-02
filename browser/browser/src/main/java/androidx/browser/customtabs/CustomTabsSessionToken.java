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

package androidx.browser.customtabs;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.customtabs.ICustomTabsCallback;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.BundleCompat;

/**
 * Wrapper class that can be used as a unique identifier for a session. Also contains an accessor
 * for the {@link CustomTabsCallback} for the session if there was any.
 */
public class CustomTabsSessionToken {
    private static final String TAG = "CustomTabsSessionToken";

    /**
     * Both {@link #mCallbackBinder} and {@link #mSessionId} are used as session ID.
     * At least one of the ID should be not null. If {@link #mSessionId} is null,
     * the session will be invalidated as soon as the client goes away.
     * Otherwise the browser will attempt to keep the session parameters,
     * but it might drop them to reclaim resources
     */
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable final ICustomTabsCallback mCallbackBinder;
    @Nullable private final PendingIntent mSessionId;

    @Nullable private final CustomTabsCallback mCallback;

    /* package */ static class MockCallback extends ICustomTabsCallback.Stub {
        @Override
        public void onNavigationEvent(int navigationEvent, Bundle extras) {}

        @Override
        public void extraCallback(String callbackName, Bundle args) {}

        @SuppressWarnings("NullAway")  // TODO: b/142938599
        @Override
        public Bundle extraCallbackWithResult(String callbackName, Bundle args) {
            return null;
        }

        @Override
        public void onMessageChannelReady(Bundle extras) {}

        @Override
        public void onPostMessage(String message, Bundle extras) {}

        @Override
        public void onRelationshipValidationResult(@CustomTabsService.Relation int relation,
                Uri requestedOrigin, boolean result, Bundle extras) {}

        @Override
        public IBinder asBinder() {
            return this;
        }
    }

    /**
     * Obtain a {@link CustomTabsSessionToken} from an intent. See {@link CustomTabsIntent.Builder}
     * for ways to generate an intent for custom tabs.
     * @param intent The intent to generate the token from. This has to include an extra for
     *               {@link CustomTabsIntent#EXTRA_SESSION}.
     * @return The token that was generated.
     */
    @SuppressWarnings("deprecation")
    public static @Nullable CustomTabsSessionToken getSessionTokenFromIntent(
            @NonNull Intent intent) {
        Bundle b = intent.getExtras();
        if (b == null) return null;
        IBinder binder = BundleCompat.getBinder(b, CustomTabsIntent.EXTRA_SESSION);
        PendingIntent sessionId = intent.getParcelableExtra(CustomTabsIntent.EXTRA_SESSION_ID);
        if (binder == null && sessionId == null) return null;
        ICustomTabsCallback callback = binder == null ? null :
                ICustomTabsCallback.Stub.asInterface(binder);
        return new CustomTabsSessionToken(callback, sessionId);
    }

    /**
     * Provides browsers a way to generate a mock {@link CustomTabsSessionToken} for testing
     * purposes.
     *
     * @return A mock token with no functionality.
     */
    @NonNull
    public static CustomTabsSessionToken createMockSessionTokenForTesting() {
        return new CustomTabsSessionToken(new MockCallback(), null);
    }

    CustomTabsSessionToken(@Nullable ICustomTabsCallback callbackBinder,
            @Nullable PendingIntent sessionId) {
        if (callbackBinder == null && sessionId == null) {
            throw new IllegalStateException("CustomTabsSessionToken must have either a session id "
                    + "or a callback (or both).");
        }

        mCallbackBinder = callbackBinder;
        mSessionId = sessionId;

        mCallback = mCallbackBinder == null ? null : new CustomTabsCallback() {
            @SuppressWarnings("NullAway")  // TODO: b/142938599
            @Override
            public void onNavigationEvent(int navigationEvent, @Nullable Bundle extras) {
                try {
                    mCallbackBinder.onNavigationEvent(navigationEvent, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @SuppressWarnings("NullAway")  // TODO: b/142938599
            @Override
            public void extraCallback(@NonNull String callbackName, @Nullable Bundle args) {
                try {
                    mCallbackBinder.extraCallback(callbackName, args);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @SuppressWarnings("NullAway")  // TODO: b/142938599
            @NonNull
            @Override
            public Bundle extraCallbackWithResult(@NonNull String callbackName,
                    @Nullable Bundle args) {
                try {
                    return mCallbackBinder.extraCallbackWithResult(callbackName, args);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                    return null;
                }
            }

            @SuppressWarnings("NullAway")  // TODO: b/142938599
            @Override
            public void onMessageChannelReady(@Nullable Bundle extras) {
                try {
                    mCallbackBinder.onMessageChannelReady(extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @SuppressWarnings("NullAway")  // TODO: b/142938599
            @Override
            public void onPostMessage(@NonNull String message, @Nullable Bundle extras) {
                try {
                    mCallbackBinder.onPostMessage(message, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

            @SuppressWarnings("NullAway")  // TODO: b/142938599
            @Override
            public void onRelationshipValidationResult(@CustomTabsService.Relation int relation,
                    @NonNull Uri origin, boolean result, @Nullable Bundle extras) {
                try {
                    mCallbackBinder.onRelationshipValidationResult(
                            relation, origin, result, extras);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException during ICustomTabsCallback transaction");
                }
            }

        };
    }

    @Nullable IBinder getCallbackBinder() {
        if (mCallbackBinder == null) return null;
        return mCallbackBinder.asBinder();
    }

    private IBinder getCallbackBinderAssertNotNull() {
        if (mCallbackBinder == null) {
            throw new IllegalStateException("CustomTabSessionToken must have valid binder or "
                    + "pending session");
        }
        return mCallbackBinder.asBinder();
    }

    @Nullable PendingIntent getId() {
        return mSessionId;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean hasCallback() {
        return mCallbackBinder != null;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
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
        if (!(o instanceof CustomTabsSessionToken)) return false;
        CustomTabsSessionToken other = (CustomTabsSessionToken) o;

        PendingIntent otherSessionId = other.getId();
        // If one object has a session id and the other one doesn't, they're not equal.
        if ((mSessionId == null) != (otherSessionId == null)) return false;

        // If both objects have an id, check that they are equal.
        if (mSessionId != null) return mSessionId.equals(otherSessionId);

        // Otherwise check for binder equality.
        return getCallbackBinderAssertNotNull().equals(other.getCallbackBinderAssertNotNull());
    }

    /**
     * @return {@link CustomTabsCallback} corresponding to this session if there was any non-null
     *         callbacks passed by the client.
     */
    public @Nullable CustomTabsCallback getCallback() {
        return mCallback;
    }

    /**
     * @return Whether this token is associated with the given session.
     */
    public boolean isAssociatedWith(@NonNull CustomTabsSession session) {
        return session.getBinder().equals(mCallbackBinder);
    }
}
