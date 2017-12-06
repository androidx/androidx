/*
 * Copyright (C) 2017 The Android Open Source Project
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

package android.support.v4.media;

import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.session.MediaSessionCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of {@link android.support.v4.media.MediaBrowserServiceCompat}.
 */
public class StubRemoteMediaBrowserServiceCompat extends MediaBrowserServiceCompat {
    static final String EXTRAS_KEY = "test_extras_key";
    static final String EXTRAS_VALUE = "test_extras_value";

    static final String MEDIA_ID_ROOT = "test_media_id_root";
    static final String MEDIA_METADATA = "test_media_metadata";

    static final String[] MEDIA_ID_CHILDREN = new String[]{
            "test_media_id_children_0", "test_media_id_children_1",
            "test_media_id_children_2", "test_media_id_children_3"
    };

    private static MediaSessionCompat mSession;
    private Bundle mExtras;

    @Override
    public void onCreate() {
        super.onCreate();
        mSession = new MediaSessionCompat(this, "StubRemoteMediaBrowserServiceCompat");
        setSessionToken(mSession.getSessionToken());
    }

    @Override
    public BrowserRoot onGetRoot(String clientPackageName, int clientUid, Bundle rootHints) {
        mExtras = new Bundle();
        mExtras.putString(EXTRAS_KEY, EXTRAS_VALUE);
        return new BrowserRoot(MEDIA_ID_ROOT, mExtras);
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result) {
        List<MediaItem> mediaItems = new ArrayList<>();
        if (MEDIA_ID_ROOT.equals(parentMediaId)) {
            Bundle rootHints = getBrowserRootHints();
            for (String id : MEDIA_ID_CHILDREN) {
                mediaItems.add(createMediaItem(id));
            }
            result.sendResult(mediaItems);
        }
    }

    @Override
    public void onLoadChildren(final String parentMediaId, final Result<List<MediaItem>> result,
            final Bundle options) {
        MediaMetadataCompat metadata = options.getParcelable(MEDIA_METADATA);
        if (metadata == null) {
            super.onLoadChildren(parentMediaId, result, options);
        } else {
            List<MediaItem> mediaItems = new ArrayList<>();
            mediaItems.add(new MediaItem(metadata.getDescription(), MediaItem.FLAG_PLAYABLE));
            result.sendResult(mediaItems);
        }
    }

    private MediaItem createMediaItem(String id) {
        return new MediaItem(new MediaDescriptionCompat.Builder()
                .setMediaId(id).setExtras(getBrowserRootHints()).build(),
                MediaItem.FLAG_BROWSABLE);
    }
}
