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
import static androidx.media2.SessionToken2.TYPE_BROWSER_SERVICE_LEGACY;
import static androidx.media2.SessionToken2.TYPE_LIBRARY_SERVICE;
import static androidx.media2.SessionToken2.TYPE_SESSION;
import static androidx.media2.SessionToken2.TYPE_SESSION_LEGACY;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.media.MediaSessionManager;
import androidx.media2.SessionToken2.SessionToken2Impl;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

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
@VersionedParcelize(isCustom = true)
final class SessionToken2ImplLegacy extends CustomVersionedParcelable implements SessionToken2Impl {
    // Don't mark mLegacyToken @ParcelField, because we need to use toBundle()/fromBundle() instead
    // of the writeToParcel()/Parcelable.Creator for sending extra binder.
    @NonParcelField
    private MediaSessionCompat.Token mLegacyToken;
    // Intermediate Bundle just for CustomVersionedParcelable.
    @ParcelField(1)
    Bundle mLegacyTokenBundle;
    @ParcelField(2)
    int mUid;
    @ParcelField(3)
    int mType;
    @ParcelField(4)
    ComponentName mComponentName;
    @ParcelField(5)
    String mPackageName;
    @ParcelField(6)
    String mId;

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

    /**
     * Used for {@link VersionedParcelable}
     * @hide
     */
    @RestrictTo(LIBRARY)
    SessionToken2ImplLegacy() {
        // Do nothing.
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
        return mUid;
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
                return TYPE_SESSION;
            case TYPE_BROWSER_SERVICE_LEGACY:
                return TYPE_LIBRARY_SERVICE;
        }
        return TYPE_SESSION;
    }

    @Override
    public Object getBinder() {
        return mLegacyToken;
    }

    @Override
    public void onPreParceling(boolean isStream) {
        if (mLegacyToken != null) {
            // Note: token should be null or SessionToken2 whose impl equals to this object.
            VersionedParcelable token = mLegacyToken.getSessionToken2();

            // Temporarily sets the SessionToken2 to null to prevent infinite loop when parceling.
            // Otherwise, this will be called again when mLegacyToken parcelize SessionToken2 in it
            // and it never ends.
            mLegacyToken.setSessionToken2(null);

            // Although mLegacyToken is Parcelable, we should use toBundle() instead here because
            // extra binder inside of the mLegacyToken are shared only through the toBundle().
            mLegacyTokenBundle = mLegacyToken.toBundle();

            // Resets the SessionToken2.
            mLegacyToken.setSessionToken2(token);
        } else {
            mLegacyTokenBundle = null;
        }
    }

    @Override
    public void onPostParceling() {
        // Although mLegacyToken is Parcelable, we should use fromBundle() instead here because
        // extra binder inside of the mLegacyToken are shared only through the fromBundle().
        mLegacyToken = MediaSessionCompat.Token.fromBundle(mLegacyTokenBundle);
        mLegacyTokenBundle = null;
    }
}
