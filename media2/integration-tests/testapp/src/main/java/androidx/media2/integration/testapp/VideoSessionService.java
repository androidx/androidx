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

package androidx.media2.integration.testapp;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.UriMediaItem;
import androidx.media2.player.MediaPlayer;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSessionService;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Test service for VideoPlayerActivity
 */
public class VideoSessionService extends MediaSessionService {
    private final String mSessionId = "VideoSessionService";

    private MediaPlayer mMediaPlayer;
    private MediaSession mMediaSession;
    private AudioAttributesCompat mAudioAttributes;

    @Override
    public void onCreate() {
        super.onCreate();

        Executor executor = ContextCompat.getMainExecutor(this);
        if (mMediaPlayer == null) {
            mAudioAttributes = new AudioAttributesCompat.Builder()
                    .setUsage(AudioAttributesCompat.USAGE_MEDIA)
                    .setContentType(AudioAttributesCompat.CONTENT_TYPE_MOVIE).build();
            mMediaPlayer = new MediaPlayer(this);
            mMediaPlayer.setAudioAttributes(mAudioAttributes);
        }

        List<MediaSession> sessions = getSessions();
        for (int i = 0; i < sessions.size(); i++) {
            if (sessions.get(i).getId().equals(mSessionId)) {
                mMediaSession = sessions.get(i);
            }
        }
        if (mMediaSession == null) {
            mMediaSession = new MediaSession.Builder(this, mMediaPlayer)
                    .setSessionCallback(executor, new SessionCallback())
                    .setId(mSessionId)
                    .build();
            mMediaSession.updatePlayer(mMediaPlayer);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.close();
                mMediaPlayer = null;
            }
        } catch (Exception e) {
        }

        if (mMediaSession != null) {
            mMediaSession.close();
            mMediaSession = null;
        }
    }

    @Nullable
    @Override
    public MediaSession onGetSession(@NonNull MediaSession.ControllerInfo controllerInfo) {
        return mMediaSession;
    }

    class SessionCallback extends MediaSession.SessionCallback {
        @Nullable
        @Override
        public MediaItem onCreateMediaItem(@NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller, @NonNull String mediaId) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                    .build();
            UriMediaItem mediaItem = new UriMediaItem.Builder(Uri.parse(mediaId))
                    .setMetadata(metadata)
                    .build();
            // TODO: Temporary fix for multiple calls of setMediaItem not working properly.
            //  (b/135728285)
            mMediaPlayer.reset();
            mMediaPlayer.setAudioAttributes(mAudioAttributes);
            return mediaItem;
        }
    }
}
