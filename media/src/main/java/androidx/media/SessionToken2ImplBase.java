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
import static androidx.media.SessionToken2.KEY_PACKAGE_NAME;
import static androidx.media.SessionToken2.KEY_SERVICE_NAME;
import static androidx.media.SessionToken2.KEY_SESSION_BINDER;
import static androidx.media.SessionToken2.KEY_SESSION_ID;
import static androidx.media.SessionToken2.KEY_TYPE;
import static androidx.media.SessionToken2.KEY_UID;
import static androidx.media.SessionToken2.TYPE_LIBRARY_SERVICE;
import static androidx.media.SessionToken2.TYPE_SESSION;
import static androidx.media.SessionToken2.TYPE_SESSION_SERVICE;
import static androidx.media.SessionToken2.UID_UNKNOWN;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.app.BundleCompat;
import androidx.media.SessionToken2.TokenType;

import java.util.List;

final class SessionToken2ImplBase implements SessionToken2.SupportLibraryImpl {

    private final int mUid;
    private final @TokenType int mType;
    private final String mPackageName;
    private final String mServiceName;
    private final String mSessionId;
    private final IMediaSession2 mISession2;
    private final ComponentName mComponentName;

    /**
     * Constructor for the token. You can only create token for session service or library service
     * to use by {@link MediaController2} or {@link MediaBrowser2}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the media browser service.
     */
    SessionToken2ImplBase(@NonNull Context context,
            @NonNull ComponentName serviceComponent) {
        this(context, serviceComponent, UID_UNKNOWN);
    }

    /**
     * Constructor for the token. You can only create token for session service or library service
     * to use by {@link MediaController2} or {@link MediaBrowser2}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the media browser service.
     * @param uid uid of the app.
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SessionToken2ImplBase(@NonNull Context context, @NonNull ComponentName serviceComponent,
            int uid) {
        if (serviceComponent == null) {
            throw new IllegalArgumentException("serviceComponent shouldn't be null");
        }
        mComponentName = serviceComponent;
        mPackageName = serviceComponent.getPackageName();
        mServiceName = serviceComponent.getClassName();
        // Calculate uid if it's not specified.
        final PackageManager manager = context.getPackageManager();
        if (uid == UID_UNKNOWN) {
            try {
                uid = manager.getApplicationInfo(mPackageName, 0).uid;
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Cannot find package " + mPackageName);
            }
        }
        mUid = uid;

        // Infer id and type from package name and service name
        String sessionId = getSessionIdFromService(manager, MediaLibraryService2.SERVICE_INTERFACE,
                serviceComponent);
        if (sessionId != null) {
            mSessionId = sessionId;
            mType = TYPE_LIBRARY_SERVICE;
        } else {
            // retry with session service
            mSessionId = getSessionIdFromService(manager, MediaSessionService2.SERVICE_INTERFACE,
                    serviceComponent);
            mType = TYPE_SESSION_SERVICE;
        }
        if (mSessionId == null) {
            throw new IllegalArgumentException("service " + mServiceName + " doesn't implement"
                    + " session service nor library service. Use service's full name.");
        }
        mISession2 = null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SessionToken2ImplBase(int uid, int type, String packageName, String serviceName,
            String sessionId, IMediaSession2 iSession2) {
        mUid = uid;
        mType = type;
        mPackageName = packageName;
        mServiceName = serviceName;
        mComponentName = (mType == TYPE_SESSION) ? null
                : new ComponentName(packageName, serviceName);
        mSessionId = sessionId;
        mISession2 = iSession2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return mType
                + prime * (mUid
                + prime * (mPackageName.hashCode()
                + prime * (mSessionId.hashCode()
                + prime * (mServiceName != null ? mServiceName.hashCode() : 0))));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2ImplBase)) {
            return false;
        }
        SessionToken2ImplBase other = (SessionToken2ImplBase) obj;
        return mUid == other.mUid
                && TextUtils.equals(mPackageName, other.mPackageName)
                && TextUtils.equals(mServiceName, other.mServiceName)
                && TextUtils.equals(mSessionId, other.mSessionId)
                && mType == other.mType
                && sessionBinderEquals(mISession2, other.mISession2);
    }

    private boolean sessionBinderEquals(IMediaSession2 a, IMediaSession2 b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.asBinder().equals(b.asBinder());
    }

    @Override
    public String toString() {
        return "SessionToken {pkg=" + mPackageName + " id=" + mSessionId + " type=" + mType
                + " service=" + mServiceName + " IMediaSession2=" + mISession2 + "}";
    }

    @Override
    public int getUid() {
        return mUid;
    }

    @Override
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    @Override
    public @Nullable String getServiceName() {
        return mServiceName;
    }

    /**
     * @hide
     * @return component name of this session token. Can be null for TYPE_SESSION.
     */
    @RestrictTo(LIBRARY_GROUP)
    @Override
    public ComponentName getComponentName() {
        return mComponentName;
    }

    @Override
    public String getSessionId() {
        return mSessionId;
    }

    @Override
    public @TokenType int getType() {
        return mType;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_UID, mUid);
        bundle.putString(KEY_PACKAGE_NAME, mPackageName);
        bundle.putString(KEY_SERVICE_NAME, mServiceName);
        bundle.putString(KEY_SESSION_ID, mSessionId);
        bundle.putInt(KEY_TYPE, mType);
        if (mISession2 != null) {
            BundleCompat.putBinder(bundle, KEY_SESSION_BINDER, mISession2.asBinder());
        }
        return bundle;
    }

    @Override
    public Object getBinder() {
        return mISession2 == null ? null : mISession2.asBinder();
    }
    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @param bundle
     * @return SessionToken2 object
     */
    public static SessionToken2ImplBase fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final int uid = bundle.getInt(KEY_UID);
        final @TokenType int type = bundle.getInt(KEY_TYPE, -1);
        final String packageName = bundle.getString(KEY_PACKAGE_NAME);
        final String serviceName = bundle.getString(KEY_SERVICE_NAME);
        final String sessionId = bundle.getString(KEY_SESSION_ID);
        final IMediaSession2 iSession2 = (IMediaSession2) BundleCompat.getBinder(
                bundle, KEY_SESSION_BINDER);

        // Sanity check.
        switch (type) {
            case TYPE_SESSION:
                if (iSession2 == null) {
                    throw new IllegalArgumentException("Unexpected token for session,"
                            + " binder=" + iSession2);
                }
                break;
            case TYPE_SESSION_SERVICE:
            case TYPE_LIBRARY_SERVICE:
                if (TextUtils.isEmpty(serviceName)) {
                    throw new IllegalArgumentException("Session service needs service name");
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid type");
        }
        if (TextUtils.isEmpty(packageName) || sessionId == null) {
            throw new IllegalArgumentException("Package name nor ID cannot be null.");
        }
        return new SessionToken2ImplBase(uid, type, packageName, serviceName, sessionId, iSession2);
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
}
