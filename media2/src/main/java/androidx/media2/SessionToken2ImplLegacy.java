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

import static androidx.media2.SessionToken2.KEY_PACKAGE_NAME;
import static androidx.media2.SessionToken2.KEY_SERVICE_NAME;
import static androidx.media2.SessionToken2.KEY_SESSION_ID;
import static androidx.media2.SessionToken2.KEY_TOKEN_LEGACY;
import static androidx.media2.SessionToken2.KEY_TYPE;
import static androidx.media2.SessionToken2.KEY_UID;
import static androidx.media2.SessionToken2.TYPE_BROWSER_SERVICE_LEGACY;
import static androidx.media2.SessionToken2.TYPE_SESSION_LEGACY;
import static androidx.media2.SessionToken2.UID_UNKNOWN;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager;
import androidx.media2.SessionToken2.SessionToken2Impl;

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
final class SessionToken2ImplLegacy implements SessionToken2Impl {

    private final MediaSessionCompat.Token mLegacyToken;
    private final int mUid;
    private final int mType;
    private final ComponentName mComponentName;
    private final String mPackageName;
    private final String mId;

    SessionToken2ImplLegacy(MediaSessionCompat.Token token, String packageName, int uid) {
        if (token == null) {
            throw new IllegalArgumentException("token shouldn't be null.");
        }
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("packageName shouldn't be null.");
        }

        mLegacyToken = token;
        mUid = uid;
        mPackageName = packageName;
        mComponentName = null;
        mType = TYPE_SESSION_LEGACY;
        mId = "";
    }

    SessionToken2ImplLegacy(ComponentName serviceComponent, int uid, String id) {
        if (serviceComponent == null) {
            throw new IllegalArgumentException("serviceComponent shouldn't be null.");
        }

        mLegacyToken = null;
        mUid = uid;
        mType = TYPE_BROWSER_SERVICE_LEGACY;
        mPackageName = serviceComponent.getPackageName();
        mComponentName = serviceComponent;
        mId = id;
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mType, mComponentName, mLegacyToken);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2ImplLegacy)) {
            return false;
        }
        SessionToken2ImplLegacy other = (SessionToken2ImplLegacy) obj;
        if (mType != other.mType) {
            return false;
        }
        switch (mType) {
            case TYPE_SESSION_LEGACY:
                return ObjectsCompat.equals(mLegacyToken, other.mLegacyToken);
            case TYPE_BROWSER_SERVICE_LEGACY:
                return ObjectsCompat.equals(mComponentName, other.mComponentName);
        }
        return false;
    }

    @Override
    public boolean isLegacySession() {
        return true;
    }

    @Override
    public String toString() {
        return "SessionToken2 {legacyToken=" + mLegacyToken + "}";
    }

    @Override
    public int getUid() {
        return UID_UNKNOWN;
    }

    @Override
    public @NonNull String getPackageName() {
        return mPackageName;
    }

    @Override
    public @Nullable String getServiceName() {
        return mComponentName == null ? null : mComponentName.getClassName();
    }

    @Override
    public ComponentName getComponentName() {
        return mComponentName;
    }

    @Override
    public String getSessionId() {
        return mId;
    }

    @Override
    public @SessionToken2.TokenType int getType() {
        switch (mType) {
            case TYPE_SESSION_LEGACY:
                return SessionToken2.TYPE_SESSION;
            case TYPE_BROWSER_SERVICE_LEGACY:
                return SessionToken2.TYPE_LIBRARY_SERVICE;
        }
        return SessionToken2.TYPE_SESSION;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putBundle(KEY_TOKEN_LEGACY, (mLegacyToken == null) ? null : mLegacyToken.toBundle());
        bundle.putInt(KEY_UID, mUid);
        bundle.putInt(KEY_TYPE, mType);
        bundle.putString(KEY_PACKAGE_NAME, mPackageName);
        bundle.putString(KEY_SERVICE_NAME,
                mComponentName == null ? null : mComponentName.getClassName());
        bundle.putString(KEY_SESSION_ID, mId);
        return bundle;
    }

    @Override
    public Object getBinder() {
        return mLegacyToken;
    }

    /**
     * Create a token from the bundle, exported by {@link #toBundle()}.
     *
     * @return SessionToken2 object
     */
    public static SessionToken2ImplLegacy fromBundle(@NonNull Bundle bundle) {
        int type = bundle.getInt(KEY_TYPE);
        switch (type) {
            case TYPE_SESSION_LEGACY:
                return new SessionToken2ImplLegacy(
                        MediaSessionCompat.Token.fromBundle(bundle.getBundle(KEY_TOKEN_LEGACY)),
                        bundle.getString(KEY_PACKAGE_NAME),
                        bundle.getInt(KEY_UID));
            case TYPE_BROWSER_SERVICE_LEGACY:
                return new SessionToken2ImplLegacy(
                        new ComponentName(bundle.getString(KEY_PACKAGE_NAME),
                                bundle.getString(KEY_SERVICE_NAME)),
                        bundle.getInt(KEY_UID),
                        bundle.getString(KEY_SESSION_ID));
        }
        return null;
    }
}
