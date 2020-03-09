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

import static androidx.media2.common.MediaMetadata.METADATA_KEY_MEDIA_ID;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.AudioAttributesCompat;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.UriMediaItem;
import androidx.media2.player.MediaPlayer;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSessionService;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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
            showToastIfFailed(mMediaPlayer.setAudioAttributes(mAudioAttributes),
                    "Failed to set audio attribute.");
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

    void showToastIfFailed(ListenableFuture<SessionPlayer.PlayerResult> result,
            String errorMessage) {
        result.addListener(() -> {
            boolean showToastMessage = false;
            try {
                SessionPlayer.PlayerResult playerResult = result.get(0, TimeUnit.MILLISECONDS);
                if (playerResult.getResultCode() != SessionPlayer.PlayerResult.RESULT_SUCCESS) {
                    showToastMessage = true;
                }
            } catch (Exception e) {
                showToastMessage = true;
            }
            if (showToastMessage) {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    class SessionCallback extends MediaSession.SessionCallback {
        @Nullable
        @Override
        public MediaItem onCreateMediaItem(@NonNull MediaSession session,
                @NonNull MediaSession.ControllerInfo controller, @NonNull String mediaId) {
            MediaMetadata metadata = new MediaMetadata.Builder()
                    .putString(METADATA_KEY_MEDIA_ID, mediaId)
                    .build();
            UriMediaItem currentItem = new UriMediaItem.Builder(Uri.parse(mediaId))
                    .setMetadata(metadata)
                    .build();
            MetadataExtractTask task = new MetadataExtractTask(currentItem,
                    VideoSessionService.this);
            task.execute();
            // TODO: Temporary fix for multiple calls of setMediaItem not working properly.
            //  (b/135728285)
            mMediaPlayer.reset();
            showToastIfFailed(mMediaPlayer.setAudioAttributes(mAudioAttributes),
                    "Failed to set audio attribute.");
            return currentItem;
        }
    }

    private static class MetadataExtractTask extends AsyncTask<Void, Void, MediaMetadata> {
        private MediaItem mItem;
        private WeakReference<Context> mRefContext;

        MetadataExtractTask(MediaItem mediaItem, Context context) {
            mItem = mediaItem;
            mRefContext = new WeakReference<>(context);
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
            Context context = mRefContext.get();
            try {
                if (mediaItem == null) {
                    return null;
                } else if (mediaItem instanceof UriMediaItem && context != null) {
                    Uri uri = ((UriMediaItem) mediaItem).getUri();
                    retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(mRefContext.get(), uri);
                }
            } catch (IllegalArgumentException e) {
                return mediaItem.getMetadata();
            }

            if (retriever == null) {
                return mediaItem.getMetadata();
            }

            String title = extractString(retriever, MediaMetadataRetriever.METADATA_KEY_TITLE);
            String musicArtistText = extractString(retriever,
                    MediaMetadataRetriever.METADATA_KEY_ARTIST);
            Bitmap musicAlbumBitmap = extractAlbumArt(retriever);

            if (retriever != null) {
                retriever.release();
            }

            MediaMetadata metadata = mediaItem.getMetadata();
            MediaMetadata.Builder builder = metadata == null
                    ? new MediaMetadata.Builder() : new MediaMetadata.Builder(metadata);
            builder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
            builder.putString(MediaMetadata.METADATA_KEY_ARTIST, musicArtistText);
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
