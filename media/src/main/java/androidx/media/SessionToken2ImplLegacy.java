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

import static androidx.media.SessionToken2.KEY_TOKEN_LEGACY;
import static androidx.media.SessionToken2.KEY_TYPE;
import static androidx.media.SessionToken2.UID_UNKNOWN;

import android.content.ComponentName;
import android.os.Bundle;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
final class SessionToken2ImplLegacy implements SessionToken2.SupportLibraryImpl {

    private final MediaSessionCompat.Token mLegacyToken;

    SessionToken2ImplLegacy(MediaSessionCompat.Token token) {
        mLegacyToken = token;
    }

    @Override
    public int hashCode() {
        return mLegacyToken.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SessionToken2ImplLegacy)) {
            return false;
        }
        SessionToken2ImplLegacy other = (SessionToken2ImplLegacy) obj;
        return mLegacyToken.equals(other.mLegacyToken);
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
        return null;
    }

    @Override
    public @Nullable String getServiceName() {
        return null;
    }

    @Override
    public ComponentName getComponentName() {
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public @SessionToken2.TokenType int getType() {
        return SessionToken2.TYPE_SESSION;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_TYPE, SessionToken2.TYPE_SESSION_LEGACY);
        bundle.putBundle(KEY_TOKEN_LEGACY, mLegacyToken.toBundle());
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
        Bundle legacyTokenBundle = bundle.getBundle(KEY_TOKEN_LEGACY);
        return new SessionToken2ImplLegacy(MediaSessionCompat.Token.fromBundle(legacyTokenBundle));
    }
}
