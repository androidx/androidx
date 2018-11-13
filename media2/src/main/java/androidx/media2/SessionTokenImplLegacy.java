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
import static androidx.media2.SessionToken.TYPE_BROWSER_SERVICE_LEGACY;
import static androidx.media2.SessionToken.TYPE_LIBRARY_SERVICE;
import static androidx.media2.SessionToken.TYPE_SESSION;
import static androidx.media2.SessionToken.TYPE_SESSION_LEGACY;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.ObjectsCompat;
import androidx.media2.SessionToken.SessionTokenImpl;
import androidx.versionedparcelable.CustomVersionedParcelable;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

@VersionedParcelize(isCustom = true)
final class SessionTokenImplLegacy extends CustomVersionedParcelable implements SessionTokenImpl {
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

    SessionTokenImplLegacy(MediaSessionCompat.Token token, String packageName, int uid) {
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
    }

    SessionTokenImplLegacy(ComponentName serviceComponent, int uid) {
        if (serviceComponent == null) {
            throw new IllegalArgumentException("serviceComponent shouldn't be null.");
        }

        mLegacyToken = null;
        mUid = uid;
        mType = TYPE_BROWSER_SERVICE_LEGACY;
        mPackageName = serviceComponent.getPackageName();
        mComponentName = serviceComponent;
    }

    /**
     * Used for {@link VersionedParcelable}
     * @hide
     */
    @RestrictTo(LIBRARY)
    SessionTokenImplLegacy() {
        // Do nothing.
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mType, mComponentName, mLegacyToken);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionTokenImplLegacy)) {
            return false;
        }
        SessionTokenImplLegacy other = (SessionTokenImplLegacy) obj;
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
        return "SessionToken {legacyToken=" + mLegacyToken + "}";
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
    public @SessionToken.TokenType int getType() {
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
            // Note: token should be null or SessionToken whose impl equals to this object.
            VersionedParcelable token = mLegacyToken.getSession2Token();

            // Temporarily sets the SessionToken to null to prevent infinite loop when parceling.
            // Otherwise, this will be called again when mLegacyToken parcelize SessionToken in it
            // and it never ends.
            mLegacyToken.setSession2Token(null);

            // Although mLegacyToken is Parcelable, we should use toBundle() instead here because
            // extra binder inside of the mLegacyToken are shared only through the toBundle().
            mLegacyTokenBundle = mLegacyToken.toBundle();

            // Resets the SessionToken.
            mLegacyToken.setSession2Token(token);
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
