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

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;

import androidx.annotation.NonNull;
import androidx.media.MediaLibraryService2.MediaLibrarySession;

import java.util.concurrent.Executor;

@TargetApi(Build.VERSION_CODES.KITKAT)
class MediaLibrarySessionImplBase extends MediaSession2ImplBase {
    MediaLibrarySessionImplBase(Context context,
            MediaSessionCompat sessionCompat, String id,
            MediaPlayerInterface player, MediaPlaylistAgent playlistAgent,
            VolumeProviderCompat volumeProvider, PendingIntent sessionActivity,
            Executor callbackExecutor,
            MediaSession2.SessionCallback callback) {
        super(context, sessionCompat, id, player, playlistAgent, volumeProvider, sessionActivity,
                callbackExecutor, callback);
    }

    @Override
    MediaSession2 createInstance() {
        return new MediaLibrarySession(this);
    }

    static final class Builder extends MediaSession2ImplBase.BuilderBase<
            MediaLibrarySession, MediaLibrarySession.MediaLibrarySessionCallback> {
        Builder(Context context) {
            super(context);
        }

        @Override
        public @NonNull MediaLibrarySession build() {
            if (mCallbackExecutor == null) {
                mCallbackExecutor = new MainHandlerExecutor(mContext);
            }
            if (mCallback == null) {
                mCallback = new MediaLibrarySession.MediaLibrarySessionCallback() {};
            }
            return new MediaLibrarySession(new MediaLibrarySessionImplBase(mContext,
                    new MediaSessionCompat(mContext, mId), mId, mPlayer, mPlaylistAgent,
                    mVolumeProvider, mSessionActivity, mCallbackExecutor, mCallback));
        }
    }
}
