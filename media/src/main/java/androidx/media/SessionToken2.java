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

package androidx.media;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Represents an ongoing {@link MediaSession2} or a {@link MediaSessionService2}.
 * If it's representing a session service, it may not be ongoing.
 * <p>
 * This may be passed to apps by the session owner to allow them to create a
 * {@link MediaController2} to communicate with the session.
 * <p>
 * It can be also obtained by {@link MediaSessionManager}.
 */
// New version of MediaSession.Token for following reasons
//   - Stop implementing Parcelable for updatable support
//   - Represent session and library service (formerly browser service) in one class.
//     Previously MediaSession.Token was for session and ComponentName was for service.
public final class SessionToken2 {
    private static final String TAG = "SessionToken2";

    private static final long WAIT_TIME_MS_FOR_SESSION_READY = 300;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {TYPE_SESSION, TYPE_SESSION_SERVICE, TYPE_LIBRARY_SERVICE})
    public @interface TokenType {
    }

    /**
     * Type for {@link MediaSession2}.
     */
    public static final int TYPE_SESSION = 0;

    /**
     * Type for {@link MediaSessionService2}.
     */
    public static final int TYPE_SESSION_SERVICE = 1;

    /**
     * Type for {@link MediaLibraryService2}.
     */
    public static final int TYPE_LIBRARY_SERVICE = 2;

    /**
     * Type for {@link MediaSessionCompat}.
     */
    static final int TYPE_SESSION_LEGACY = 100;

    // From the return value of android.os.Process.getUidForName(String) when error
    static final int UID_UNKNOWN = -1;

    static final String KEY_UID = "android.media.token.uid";
    static final String KEY_TYPE = "android.media.token.type";
    static final String KEY_PACKAGE_NAME = "android.media.token.package_name";
    static final String KEY_SERVICE_NAME = "android.media.token.service_name";
    static final String KEY_SESSION_ID = "android.media.token.session_id";
    static final String KEY_SESSION_BINDER = "android.media.token.session_binder";
    static final String KEY_TOKEN_LEGACY = "android.media.token.LEGACY";

    private final SupportLibraryImpl mImpl;

    /**
     * Constructor for the token. You can only create token for session service or library service
     * to use by {@link MediaController2} or {@link MediaBrowser2}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the media browser service.
     */
    public SessionToken2(@NonNull Context context, @NonNull ComponentName serviceComponent) {
        mImpl = new SessionToken2ImplBase(context, serviceComponent);
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SessionToken2(SupportLibraryImpl impl) {
        mImpl = impl;
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2)) {
            return false;
        }
        SessionToken2 other = (SessionToken2) obj;
        return mImpl.equals(other.mImpl);
    }

    @Override
    public String toString() {
        return mImpl.toString();
    }

    /**
     * @return uid of the session
     */
    public int getUid() {
        return mImpl.getUid();
    }

    /**
     * @return package name
     */
    public @NonNull String getPackageName() {
        return mImpl.getPackageName();
    }

    /**
     * @return service name. Can be {@code null} for TYPE_SESSION.
     */
    public @Nullable String getServiceName() {
        return mImpl.getServiceName();
    }

    /**
     * @hide
     * @return component name of this session token. Can be null for TYPE_SESSION.
     */
    @RestrictTo(LIBRARY_GROUP)
    public ComponentName getComponentName() {
        return mImpl.getComponentName();
    }

    /**
     * @return id
     */
    public String getId() {
        return mImpl.getSessionId();
    }

    /**
     * @return type of the token
     * @see #TYPE_SESSION
     * @see #TYPE_SESSION_SERVICE
     * @see #TYPE_LIBRARY_SERVICE
     */
    public @TokenType int getType() {
        return mImpl.getType();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public boolean isLegacySession() {
        return mImpl instanceof SessionToken2ImplLegacy;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public Object getBinder() {
        return mImpl.getBinder();
    }

    /**
     * Create a {@link Bundle} from this token to share it across processes.
     * @return Bundle
     */
    public Bundle toBundle() {
        return mImpl.toBundle();
    }

    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @param bundle
     * @return SessionToken2 object
     */
    public static SessionToken2 fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }

        final int type = bundle.getInt(KEY_TYPE, -1);
        if (type == TYPE_SESSION_LEGACY) {
            return new SessionToken2(SessionToken2ImplLegacy.fromBundle(bundle));
        } else {
            return new SessionToken2(SessionToken2ImplBase.fromBundle(bundle));
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static void createSessionToken2(@NonNull final Context context,
            @NonNull final MediaSessionCompat.Token token, @NonNull final Executor executor,
            @NonNull final OnSessionToken2CreatedListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener shouldn't be null");
        }

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final MediaControllerCompat controller = new MediaControllerCompat(context,
                                token);
                    MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
                        @Override
                        public void onSessionReady() {
                            synchronized (listener) {
                                listener.onSessionToken2Created(token,
                                        controller.getSessionToken2());
                                listener.notify();
                            }
                        }
                    };
                    controller.registerCallback(callback);
                    if (controller.isSessionReady()) {
                        listener.onSessionToken2Created(token, controller.getSessionToken2());
                    }
                    synchronized (listener) {
                        listener.wait(WAIT_TIME_MS_FOR_SESSION_READY);
                        if (!controller.isSessionReady()) {
                            // token for framework session.
                            SessionToken2 token2 = new SessionToken2(
                                    new SessionToken2ImplLegacy(token));
                            token.setSessionToken2(token2);
                            listener.onSessionToken2Created(token, token2);
                        }
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to create session token2.", e);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Failed to create session token2.", e);
                }
            }
        });
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static String getSessionId(ResolveInfo resolveInfo) {
        if (resolveInfo == null || resolveInfo.serviceInfo == null) {
            return null;
        } else if (resolveInfo.serviceInfo.metaData == null) {
            return "";
        } else {
            return resolveInfo.serviceInfo.metaData.getString(
                    MediaSessionService2.SERVICE_META_DATA, "");
        }
    }

    private static String getSessionIdFromService(PackageManager manager, String serviceInterface,
            ComponentName serviceComponent) {
        Intent serviceIntent = new Intent(serviceInterface);
        // Use queryIntentServices to find services with MediaLibraryService2.SERVICE_INTERFACE.
        // We cannot use resolveService with intent specified class name, because resolveService
        // ignores actions if Intent.setClassName() is specified.
        serviceIntent.setPackage(serviceComponent.getPackageName());

        List<ResolveInfo> list = manager.queryIntentServices(
                serviceIntent, PackageManager.GET_META_DATA);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ResolveInfo resolveInfo = list.get(i);
                if (resolveInfo == null || resolveInfo.serviceInfo == null) {
                    continue;
                }
                if (TextUtils.equals(
                        resolveInfo.serviceInfo.name, serviceComponent.getClassName())) {
                    return getSessionId(resolveInfo);
                }
            }
        }
        return null;
    }

    /**
     * @hide
     * Interface definition of a listener to be invoked when a {@link SessionToken2 token2} object
     * is created from a {@link MediaSessionCompat.Token compat token}.
     *
     * @see #createSessionToken2
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface OnSessionToken2CreatedListener {
        /**
         * Called when SessionToken2 object is created.
         *
         * @param token the compat token used for creating {@code token2}
         * @param token2 the created SessionToken2 object
         */
        void onSessionToken2Created(MediaSessionCompat.Token token, SessionToken2 token2);
    }

    interface SupportLibraryImpl {
        int getUid();
        @NonNull String getPackageName();
        @Nullable String getServiceName();
        @Nullable ComponentName getComponentName();
        String getSessionId();
        @TokenType int getType();
        Bundle toBundle();
        Object getBinder();
    }
}
