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

package androidx.media2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;

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
import java.util.concurrent.Executor;

/**
 * Represents an ongoing {@link MediaSession} or a {@link MediaSessionService}.
 * If it's representing a session service, it may not be ongoing.
 * <p>
 * This may be passed to apps by the session owner to allow them to create a
 * {@link MediaController} to communicate with the session.
 * <p>
 * It can be also obtained by {@link MediaSessionManager}.
 */
// New version of MediaSession.Token for following reasons
//   - Stop implementing Parcelable for updatable support
//   - Represent session and library service (formerly browser service) in one class.
//     Previously MediaSession.Token was for session and ComponentName was for service.
//     This helps controller apps to keep target of dispatching media key events in uniform way.
//     For details about the reason, see following. (Android O+)
//         android.media.session.MediaSessionManager.Callback#onAddressedPlayerChanged
@VersionedParcelize
@SuppressLint("ObsoleteSdkInt") // TODO: Remove once the minSdkVersion is lowered enough.
public final class SessionToken implements VersionedParcelable {
    private static final String TAG = "SessionToken";

    private static final long WAIT_TIME_MS_FOR_SESSION_READY = 300;
    private static final int MSG_SEND_TOKEN2_FOR_LEGACY_SESSION = 1000;

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
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

    /**
     * Constructor for the token. You can create token of {@link MediaSessionService},
     * {@link MediaLibraryService} nor {@link MediaBrowserServiceCompat} for
     * {@link MediaController} or {@link MediaBrowser}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the media browser service.
     */
    public SessionToken(@NonNull Context context, @NonNull ComponentName serviceComponent) {
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
                    + " MediaBrowserServiceCompat. Use service's full name.");
        }
        if (type != TYPE_BROWSER_SERVICE_LEGACY) {
            mImpl = new SessionTokenImplBase(serviceComponent, uid, type);
        } else {
            mImpl = new SessionTokenImplLegacy(serviceComponent, uid);
        }
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SessionToken(SessionTokenImpl impl) {
        mImpl = impl;
    }

    /**
     * Used for {@link VersionedParcelable}
     * @hide
     */
    @RestrictTo(LIBRARY)
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
        return mImpl.isLegacySession();
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public Object getBinder() {
        return mImpl.getBinder();
    }

    /**
     * Creates SessionToken object from MediaSessionCompat.Token.
     * When the SessionToken is ready, OnSessionTokenCreateListner will be called.
     *
     * TODO: Consider to use this in the constructor of MediaController.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    public static void createSessionToken(@NonNull final Context context,
            @NonNull final MediaSessionCompat.Token tokenCompat, @NonNull final Executor executor,
            @NonNull final OnSessionTokenCreatedListener listener) {
        if (context == null) {
            throw new IllegalArgumentException("context shouldn't be null");
        }
        if (tokenCompat == null) {
            throw new IllegalArgumentException("token shouldn't be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor shouldn't be null");
        }
        if (listener == null) {
            throw new IllegalArgumentException("listener shouldn't be null");
        }

        try {
            VersionedParcelable token2 = tokenCompat.getSession2Token();
            if (token2 instanceof SessionToken) {
                notifySessionTokenCreated(executor, listener, tokenCompat, (SessionToken) token2);
                return;
            }
            final MediaControllerCompat controller =
                    new MediaControllerCompat(context, tokenCompat);
            final int uid = getUid(context.getPackageManager(), controller.getPackageName());
            final SessionToken token2ForLegacySession = new SessionToken(
                    new SessionTokenImplLegacy(tokenCompat, controller.getPackageName(), uid));

            final HandlerThread thread = new HandlerThread(TAG);
            thread.start();
            final Handler handler = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    synchronized (listener) {
                        if (msg.what == MSG_SEND_TOKEN2_FOR_LEGACY_SESSION) {
                            // token for framework session.
                            controller.unregisterCallback((MediaControllerCompat.Callback) msg.obj);
                            tokenCompat.setSession2Token(token2ForLegacySession);
                            notifySessionTokenCreated(executor, listener, tokenCompat,
                                    token2ForLegacySession);
                            if (Build.VERSION.SDK_INT >= 18) {
                                thread.quitSafely();
                            } else {
                                thread.quit();
                            }
                        }
                    }
                }
            };
            MediaControllerCompat.Callback callback = new MediaControllerCompat.Callback() {
                @Override
                public void onSessionReady() {
                    synchronized (listener) {
                        handler.removeMessages(MSG_SEND_TOKEN2_FOR_LEGACY_SESSION);
                        controller.unregisterCallback(this);
                        if (!(tokenCompat.getSession2Token() instanceof SessionToken)) {
                            tokenCompat.setSession2Token(token2ForLegacySession);
                        }
                        notifySessionTokenCreated(executor, listener, tokenCompat,
                                (SessionToken) tokenCompat.getSession2Token());
                        if (Build.VERSION.SDK_INT >= 18) {
                            thread.quitSafely();
                        } else {
                            thread.quit();
                        }
                    }
                }
            };
            synchronized (listener) {
                controller.registerCallback(callback, handler);
                Message msg = handler.obtainMessage(MSG_SEND_TOKEN2_FOR_LEGACY_SESSION, callback);
                handler.sendMessageDelayed(msg, WAIT_TIME_MS_FOR_SESSION_READY);
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to create session token2.", e);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void notifySessionTokenCreated(final Executor executor,
            final OnSessionTokenCreatedListener listener, final MediaSessionCompat.Token token,
            final SessionToken token2) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.onSessionTokenCreated(token, token2);
            }
        });
    }

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

    private static int getUid(PackageManager manager, String packageName) {
        try {
            return manager.getApplicationInfo(packageName, 0).uid;
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalArgumentException("Cannot find package " + packageName);
        }
    }

    /**
     * @hide
     * Interface definition of a listener to be invoked when a {@link SessionToken token2} object
     * is created from a {@link MediaSessionCompat.Token compat token}.
     *
     * @see #createSessionToken
     */
    @RestrictTo(LIBRARY_GROUP)
    public interface OnSessionTokenCreatedListener {
        /**
         * Called when SessionToken object is created.
         *
         * @param token the compat token used for creating {@code token2}
         * @param token2 the created SessionToken object
         */
        void onSessionTokenCreated(MediaSessionCompat.Token token, SessionToken token2);
    }

    interface SessionTokenImpl extends VersionedParcelable {
        boolean isLegacySession();
        int getUid();
        @NonNull String getPackageName();
        @Nullable String getServiceName();
        @Nullable ComponentName getComponentName();
        @TokenType int getType();
        Object getBinder();
    }
}
