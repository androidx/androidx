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
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

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

    //private final SessionToken2Provider mProvider;

    // From the return value of android.os.Process.getUidForName(String) when error
    private static final int UID_UNKNOWN = -1;

    private static final String KEY_UID = "android.media.token.uid";
    private static final String KEY_TYPE = "android.media.token.type";
    private static final String KEY_PACKAGE_NAME = "android.media.token.package_name";
    private static final String KEY_SERVICE_NAME = "android.media.token.service_name";
    private static final String KEY_ID = "android.media.token.id";
    private static final String KEY_SESSION_TOKEN = "android.media.token.session_token";

    private final int mUid;
    private final @TokenType int mType;
    private final String mPackageName;
    private final String mServiceName;
    private final String mId;
    private final MediaSessionCompat.Token mSessionCompatToken;
    private final ComponentName mComponentName;

    /**
     * Constructor for the token. You can only create token for session service or library service
     * to use by {@link MediaController2} or {@link MediaBrowser2}.
     *
     * @param context The context.
     * @param serviceComponent The component name of the media browser service.
     */
    public SessionToken2(@NonNull Context context, @NonNull ComponentName serviceComponent) {
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
    public SessionToken2(@NonNull Context context, @NonNull ComponentName serviceComponent,
            int uid) {
        if (serviceComponent == null) {
            throw new IllegalArgumentException("serviceComponent shouldn't be null");
        }
        mComponentName = serviceComponent;
        mPackageName = serviceComponent.getPackageName();
        mServiceName = serviceComponent.getClassName();
        // Calculate uid if it's not specified.
        final PackageManager manager = context.getPackageManager();
        if (uid < 0) {
            try {
                uid = manager.getApplicationInfo(mPackageName, 0).uid;
            } catch (PackageManager.NameNotFoundException e) {
                throw new IllegalArgumentException("Cannot find package " + mPackageName);
            }
        }
        mUid = uid;

        // Infer id and type from package name and service name
        String id = getSessionIdFromService(manager, MediaLibraryService2.SERVICE_INTERFACE,
                serviceComponent);
        if (id != null) {
            mId = id;
            mType = TYPE_LIBRARY_SERVICE;
        } else {
            // retry with session service
            mId = getSessionIdFromService(manager, MediaSessionService2.SERVICE_INTERFACE,
                    serviceComponent);
            mType = TYPE_SESSION_SERVICE;
        }
        if (mId == null) {
            throw new IllegalArgumentException("service " + mServiceName + " doesn't implement"
                    + " session service nor library service. Use service's full name.");
        }
        mSessionCompatToken = null;
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    SessionToken2(int uid, int type, String packageName, String serviceName,
            String id, MediaSessionCompat.Token sessionCompatToken) {
        mUid = uid;
        mType = type;
        mPackageName = packageName;
        mServiceName = serviceName;
        mComponentName = (mType == TYPE_SESSION) ? null
                : new ComponentName(packageName, serviceName);
        mId = id;
        mSessionCompatToken = sessionCompatToken;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        return mType
                + prime * (mUid
                + prime * (mPackageName.hashCode()
                + prime * (mId.hashCode()
                + prime * (mServiceName != null ? mServiceName.hashCode() : 0))));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2)) {
            return false;
        }
        SessionToken2 other = (SessionToken2) obj;
        return mUid == other.mUid
                && TextUtils.equals(mPackageName, other.mPackageName)
                && TextUtils.equals(mServiceName, other.mServiceName)
                && TextUtils.equals(mId, other.mId)
                && mType == other.mType;
    }

    @Override
    public String toString() {
        return "SessionToken {pkg=" + mPackageName + " id=" + mId + " type=" + mType
                + " service=" + mServiceName + " sessionCompatToken=" + mSessionCompatToken + "}";
    }

    /**
     * @return uid of the session
     */
    public int getUid() {
        return mUid;
    }

    /**
     * @return package name
     */
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    /**
     * @return service name. Can be {@code null} for TYPE_SESSION.
     */
    public @Nullable String getServiceName() {
        return mServiceName;
    }

    /**
     * @hide
     * @return component name of this session token. Can be null for TYPE_SESSION.
     */
    @RestrictTo(LIBRARY_GROUP)
    public ComponentName getComponentName() {
        return mComponentName;
    }

    /**
     * @return id
     */
    public String getId() {
        return mId;
    }

    /**
     * @return type of the token
     * @see #TYPE_SESSION
     * @see #TYPE_SESSION_SERVICE
     * @see #TYPE_LIBRARY_SERVICE
     */
    public @TokenType int getType() {
        return mType;
    }

    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @param bundle
     * @return
     */
    public static SessionToken2 fromBundle(@NonNull Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        final int uid = bundle.getInt(KEY_UID);
        final @TokenType int type = bundle.getInt(KEY_TYPE, -1);
        final String packageName = bundle.getString(KEY_PACKAGE_NAME);
        final String serviceName = bundle.getString(KEY_SERVICE_NAME);
        final String id = bundle.getString(KEY_ID);
        final MediaSessionCompat.Token token = bundle.getParcelable(KEY_SESSION_TOKEN);

        // Sanity check.
        switch (type) {
            case TYPE_SESSION:
                if (token == null) {
                    throw new IllegalArgumentException("Unexpected token for session,"
                            + " SessionCompat.Token=" + token);
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
        if (TextUtils.isEmpty(packageName) || id == null) {
            throw new IllegalArgumentException("Package name nor ID cannot be null.");
        }
        return new SessionToken2(uid, type, packageName, serviceName, id, token);
    }

    /**
     * Create a {@link Bundle} from this token to share it across processes.
     * @return Bundle
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_UID, mUid);
        bundle.putString(KEY_PACKAGE_NAME, mPackageName);
        bundle.putString(KEY_SERVICE_NAME, mServiceName);
        bundle.putString(KEY_ID, mId);
        bundle.putInt(KEY_TYPE, mType);
        bundle.putParcelable(KEY_SESSION_TOKEN, mSessionCompatToken);
        return bundle;
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

    MediaSessionCompat.Token getSessionCompatToken() {
        return mSessionCompatToken;
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
