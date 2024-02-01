/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.session;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.media.MediaBrowserServiceCompat;
import androidx.media.MediaSessionManager;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Represents an ongoing {@link MediaSession} or a {@link MediaSessionService}. If it's representing
 * a session service, it may not be ongoing.
 *
 * <p>This may be passed to apps by the session owner to allow them to create a {@link
 * MediaController} to communicate with the session.
 *
 * <p>It can be also obtained by {@link MediaSessionManager}.
 *
 * @deprecated androidx.media2 is deprecated. Please migrate to <a
 *     href="https://developer.android.com/guide/topics/media/media3">androidx.media3</a>.
 */
// New version of MediaSession.Token for following reasons
//   - Stop implementing Parcelable for updatable support
//   - Represent session and library service (formerly browser service) in one class.
//     Previously MediaSession.Token was for session and ComponentName was for service.
//     This helps controller apps to keep target of dispatching media key events in uniform way.
//     For details about the reason, see following. (Android O+)
//         android.media.session.MediaSessionManager.Callback#onAddressedPlayerChanged
@Deprecated
@VersionedParcelize
public final class SessionToken implements VersionedParcelable {
    private static final String TAG = "SessionToken";

    private static final long WAIT_TIME_MS_FOR_SESSION_READY = 300;
    private static final int MSG_SEND_TOKEN2_FOR_LEGACY_SESSION = 1000;

    /**
     */
    @RestrictTo(LIBRARY)
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(value = {TYPE_SESSION, TYPE_SESSION_SERVICE, TYPE_LIBRARY_SERVICE})
    public @interface TokenType {
    }

    /**
     * Type for {@link MediaSession}.
     */
    public static final int TYPE_SESSION = 0;

    /**
     * Type for {@link MediaSessionService}.
     */
    public static final int TYPE_SESSION_SERVICE = 1;

    /**
     * Type for {@link MediaLibraryService}.
     */
    public static final int TYPE_LIBRARY_SERVICE = 2;

    /**
     * Type for {@link MediaSessionCompat}.
     */
    static final int TYPE_SESSION_LEGACY = 100;

    /**
     * Type for {@link MediaBrowserServiceCompat}.
     */
    static final int TYPE_BROWSER_SERVICE_LEGACY = 101;

    @ParcelField(1)
    SessionTokenImpl mImpl;

    // WARNING: Adding a new ParcelField may break old library users (b/152830728)

    /**
     * Constructor for the token. You can create token of {@link MediaSessionService},
     * {@link MediaLibraryService} or {@link MediaBrowserServiceCompat} for
     * {@link MediaController} or {@link MediaBrowser}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the service.
     */
    public SessionToken(@NonNull Context context, @NonNull ComponentName serviceComponent) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        if (serviceComponent == null) {
            throw new NullPointerException("serviceComponent shouldn't be null");
        }
        final PackageManager manager = context.getPackageManager();
        final int uid = getUid(manager, serviceComponent.getPackageName());

        final int type;
        if (isInterfaceDeclared(manager, MediaLibraryService.SERVICE_INTERFACE,
                serviceComponent)) {
            type = TYPE_LIBRARY_SERVICE;
        } else if (isInterfaceDeclared(manager, MediaSessionService.SERVICE_INTERFACE,
                    serviceComponent)) {
            type = TYPE_SESSION_SERVICE;
        } else if (isInterfaceDeclared(manager,
                        MediaBrowserServiceCompat.SERVICE_INTERFACE, serviceComponent)) {
            type = TYPE_BROWSER_SERVICE_LEGACY;
        } else {
            throw new IllegalArgumentException(serviceComponent + " doesn't implement none of"
                    + " MediaSessionService, MediaLibraryService, MediaBrowserService nor"
                    + " MediaBrowserServiceCompat. Use service's full name");
        }
        if (type != TYPE_BROWSER_SERVICE_LEGACY) {
            mImpl = new SessionTokenImplBase(serviceComponent, uid, type);
        } else {
            mImpl = new SessionTokenImplLegacy(serviceComponent, uid);
        }
    }

    SessionToken(SessionTokenImpl impl) {
        mImpl = impl;
    }

    /**
     * Used for {@link VersionedParcelable}
     */
    SessionToken() {
        // do nothing
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken)) {
            return false;
        }
        SessionToken other = (SessionToken) obj;
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
     * @return package name of the session
     */
    @NonNull
    public String getPackageName() {
        return mImpl.getPackageName();
    }

    /**
     * @return service name of the session. Can be {@code null} for {@link #TYPE_SESSION}.
     */
    @Nullable
    public String getServiceName() {
        return mImpl.getServiceName();
    }

    /**
     * @return component name of the session. Can be {@code null} for {@link #TYPE_SESSION}.
     */
    @RestrictTo(LIBRARY)
    public ComponentName getComponentName() {
        return mImpl.getComponentName();
    }

    /**
     * @return type of the token
     * @see #TYPE_SESSION
     * @see #TYPE_SESSION_SERVICE
     * @see #TYPE_LIBRARY_SERVICE
     */
    @TokenType
    public int getType() {
        return mImpl.getType();
    }

    /**
     * @return extras of the token
     * @see MediaSession.Builder#setExtras(Bundle)
     */
    @NonNull
    public Bundle getExtras() {
        Bundle extras = mImpl.getExtras();
        if (extras == null || MediaUtils.doesBundleHaveCustomParcelable(extras)) {
            return Bundle.EMPTY;
        }
        return new Bundle(extras);
    }

    /**
     */
    @RestrictTo(LIBRARY)
    public boolean isLegacySession() {
        return mImpl.isLegacySession();
    }

    /**
     */
    @RestrictTo(LIBRARY)
    public Object getBinder() {
        return mImpl.getBinder();
    }

    /**
     * Creates SessionToken object from MediaSessionCompat.Token.
     * When the SessionToken is ready, OnSessionTokenCreateListener will be called.
     */
    @RestrictTo(LIBRARY)
    public static void createSessionToken(@NonNull final Context context,
            @NonNull final MediaSessionCompat.Token compatToken,
            @NonNull final OnSessionTokenCreatedListener listener) {
        if (context == null) {
            throw new NullPointerException("context shouldn't be null");
        }
        if (compatToken == null) {
            throw new NullPointerException("compatToken shouldn't be null");
        }
        if (listener == null) {
            throw new NullPointerException("listener shouldn't be null");
        }

        // If the compat token already has the SessionToken in itself, just notify with that token.
        VersionedParcelable token2 = compatToken.getSession2Token();
        if (token2 instanceof SessionToken) {
            listener.onSessionTokenCreated(compatToken, (SessionToken) token2);
            return;
        }

        // Try retrieving media2 token by connecting to the session.
        final MediaControllerCompat controller = createMediaControllerCompat(context, compatToken);

        final String packageName = controller.getPackageName();
        final int uid = getUid(context.getPackageManager(), packageName);
        final HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        final Handler handler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                synchronized (listener) {
                    if (msg.what != MSG_SEND_TOKEN2_FOR_LEGACY_SESSION) {
                        return;
                    }
                    controller.unregisterCallback((MediaControllerCompat.Callback) msg.obj);

                    // MediaControllerCompat.Callback#onSessionReady() is not called, which means
                    // that the connected session is a framework MediaSession instance.
                    SessionToken resultToken = new SessionToken(new SessionTokenImplLegacy(
                            compatToken, packageName, uid, controller.getSessionInfo()));

                    // To prevent repeating this process with the same compat token, put the result
                    // media2 token inside of the compat token.
                    compatToken.setSession2Token(resultToken);

                    listener.onSessionTokenCreated(compatToken, resultToken);
                    quitHandlerThread(thread);
                }
            }
        };

        MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
            @Override
            public void onSessionReady() {
                // The connected session is a MediaSessionCompat instance.
                synchronized (listener) {
                    handler.removeMessages(MSG_SEND_TOKEN2_FOR_LEGACY_SESSION);
                    controller.unregisterCallback(this);

                    // TODO: Add logic for getting media2 token in API 21- by using binder.

                    SessionToken resultToken;
                    if (compatToken.getSession2Token() instanceof SessionToken) {
                        // TODO(b/132928776): Add tests for this code path.
                        // The connected MediaSessionCompat is created by media2.MediaSession
                        resultToken = (SessionToken) compatToken.getSession2Token();
                    } else {
                        // The connected MediaSessionCompat is standalone.
                        resultToken = new SessionToken(new SessionTokenImplLegacy(
                                compatToken, packageName, uid, controller.getSessionInfo()));
                        // To prevent repeating this process with the same compat token,
                        // put the result media2 token inside of the compat token.
                        compatToken.setSession2Token(resultToken);
                    }

                    listener.onSessionTokenCreated(compatToken, resultToken);
                    quitHandlerThread(thread);
                }
            }
        };
        synchronized (listener) {
            controller.registerCallback(callback, handler);
            Message msg = handler.obtainMessage(MSG_SEND_TOKEN2_FOR_LEGACY_SESSION, callback);
            handler.sendMessageDelayed(msg, WAIT_TIME_MS_FOR_SESSION_READY);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void quitHandlerThread(HandlerThread thread) {
        thread.quitSafely();
    }

    @SuppressWarnings("deprecation")
    private static boolean isInterfaceDeclared(PackageManager manager, String serviceInterface,
            ComponentName serviceComponent) {
        Intent serviceIntent = new Intent(serviceInterface);
        // Use queryIntentServices to find services with MediaLibraryService.SERVICE_INTERFACE.
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
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    private static int getUid(PackageManager manager, String packageName) {
        try {
            return manager.getApplicationInfo(packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Cannot find package " + packageName);
        }
    }

    private static MediaControllerCompat createMediaControllerCompat(Context context,
            MediaSessionCompat.Token sessionToken) {
        return new MediaControllerCompat(context, sessionToken);
    }

    /**
     * Interface definition of a listener to be invoked when a {@link SessionToken token2} object
     * is created from a {@link MediaSessionCompat.Token compat token}.
     *
     * @see #createSessionToken
     */
    @RestrictTo(LIBRARY)
    public interface OnSessionTokenCreatedListener {
        /**
         * Called when SessionToken object is created.
         *
         * @param compatToken the compat token used for creating {@code token2}
         * @param sessionToken the created SessionToken object
         */
        void onSessionTokenCreated(MediaSessionCompat.Token compatToken, SessionToken sessionToken);
    }

    interface SessionTokenImpl extends VersionedParcelable {
        boolean isLegacySession();
        int getUid();
        @NonNull String getPackageName();
        @Nullable String getServiceName();
        @Nullable ComponentName getComponentName();
        @TokenType int getType();
        @Nullable Bundle getExtras();
        Object getBinder();
    }
}
