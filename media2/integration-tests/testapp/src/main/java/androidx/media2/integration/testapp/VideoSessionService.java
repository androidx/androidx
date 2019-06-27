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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;

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
    private UriMediaItem mCurrentItem;
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
            mCurrentItem = new UriMediaItem.Builder(Uri.parse(mediaId))
                    .setMetadata(metadata)
                    .build();
            MetadataExtractTask task = new MetadataExtractTask(mCurrentItem,
                    VideoSessionService.this);
            task.execute();
            // TODO: Temporary fix for multiple calls of setMediaItem not working properly.
            //  (b/135728285)
            mMediaPlayer.reset();
            mMediaPlayer.setAudioAttributes(mAudioAttributes);
            return mCurrentItem;
        }
    }

    private class MetadataExtractTask extends AsyncTask<Void, Void, MediaMetadata> {
        private MediaItem mItem;
        private Context mContext;

        MetadataExtractTask(MediaItem mediaItem, Context context) {
            mItem = mediaItem;
            mContext = context;
        }

        @Override
        protected MediaMetadata doInBackground(Void... params) {
            return extractMetadata(mItem);
        }

        @Override
        protected void onPostExecute(MediaMetadata metadata) {
            if (metadata != null) {
                mItem.setMetadata(metadata);
            }
        }

        MediaMetadata extractMetadata(MediaItem mediaItem) {
            MediaMetadataRetriever retriever = null;
            try {
                if (mediaItem == null) {
                    return null;
                } else if (mediaItem instanceof UriMediaItem) {
                    Uri uri = ((UriMediaItem) mediaItem).getUri();
                    retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mContext, uri);
                }
            } catch (IllegalArgumentException e) {
                retriever = null;
            }

            // Do not extract metadata of a media item which is not the current item.
            if (mediaItem != mCurrentItem) {
                if (retriever != null) {
                    retriever.release();
                }
                return null;
            }
            String title = extractString(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE);
            String musicArtistText = extractString(retriever,
                    MediaMetadataRetriever.METADATA_KEY_ARTIST);
            Bitmap musicAlbumBitmap = extractAlbumArt(retriever);

            if (retriever != null) {
                retriever.release();
            }

            // Set duration and title values as MediaMetadata for MediaControlView
            MediaMetadata.Builder builder = new MediaMetadata.Builder(mCurrentItem.getMetadata());

            builder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
            builder.putString(MediaMetadata.METADATA_KEY_ARTIST, musicArtistText);
            builder.putString(
                    MediaMetadata.METADATA_KEY_MEDIA_ID, mediaItem.getMediaId());
            builder.putLong(MediaMetadata.METADATA_KEY_PLAYABLE, 1);
            builder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, musicAlbumBitmap);
            return builder.build();
        }

        private String extractString(MediaMetadataRetriever retriever, int intKey) {
            if (retriever != null) {
                return retriever.extractMetadata(intKey);
            }
            return null;
        }

        private Bitmap extractAlbumArt(MediaMetadataRetriever retriever) {
            if (retriever != null) {
                byte[] album = retriever.getEmbeddedPicture();
                if (album != null) {
                    return BitmapFactory.decodeByteArray(album, 0, album.length);
                }
            }
            return null;
        }
    }
}
