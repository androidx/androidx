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

package androidx.media.test.service;

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.test.lib.TestUtils;
import androidx.media2.FileMediaItem;
import androidx.media2.MediaItem;
import androidx.media2.MediaLibraryService.LibraryParams;
import androidx.media2.MediaMetadata;
import androidx.media2.MediaSession;
import androidx.media2.MediaSession.ControllerInfo;
import androidx.media2.MediaUtils;
import androidx.versionedparcelable.ParcelImpl;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for tests.
 */
public final class MediaTestUtils {

    private static final String TAG = "MediaTestUtils";

    // Temporaily commenting out, since we don't have the Mock services yet.
//    /**
//     * Finds the session with id in this test package.
//     *
//     * @param context
//     * @param id
//     * @return
//     */
//    public static SessionToken getServiceToken(Context context, String id) {
//        switch (id) {
//            case MockMediaSessionService2.ID:
//                return new SessionToken(context, new ComponentName(
//                        context.getPackageName(), MockMediaSessionService2.class.getName()));
//            case MockMediaLibraryService.ID:
//                return new SessionToken(context, new ComponentName(
//                        context.getPackageName(), MockMediaLibraryService.class.getName()));
//        }
//        fail("Unknown id=" + id);
//        return null;
//    }

    /**
     * Create a playlist for testing purpose
     * <p>
     * Caller's method name will be used for prefix of each media item's media id.
     *
     * @param size list size
     * @return the newly created playlist
     */
    public static List<MediaItem> createPlaylist(int size) {
        final List<MediaItem> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[1].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(createMediaItem(caller + "_item_" + (size + 1)));
        }
        return list;
    }

    public static MediaItem createMediaItem(String id) {
        return new FileMediaItem.Builder(new FileDescriptor())
                .setMetadata(new MediaMetadata.Builder()
                        .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, id)
                        .putLong(MediaMetadata.METADATA_KEY_BROWSABLE,
                                MediaMetadata.BROWSABLE_TYPE_NONE)
                        .putLong(MediaMetadata.METADATA_KEY_PLAYABLE, 1)
                        .build())
                .build();
    }

    public static List<String> createMediaIds(int size) {
        final List<String> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[1].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(caller + "_item_" + (size + 1));
        }
        return list;
    }

    /**
     * Create a media item with the metadata for testing purpose.
     *
     * @return the newly created media item
     * @see #createMetadata()
     */
    public static MediaItem createMediaItemWithMetadata() {
        return new FileMediaItem.Builder(new FileDescriptor())
                .setMetadata(createMetadata())
                .build();
    }

    /**
     * Create a media metadata for testing purpose.
     * <p>
     * Caller's method name will be used for the media id.
     *
     * @return the newly created media item
     */
    public static MediaMetadata createMetadata() {
        String mediaId = Thread.currentThread().getStackTrace()[1].getMethodName();
        return new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                .putLong(MediaMetadata.METADATA_KEY_BROWSABLE, MediaMetadata.BROWSABLE_TYPE_NONE)
                .putLong(MediaMetadata.METADATA_KEY_PLAYABLE, 1)
                .build();
    }

    public static List<MediaItem> convertToMediaItems(List<ParcelImpl> list,
            boolean createItem) {
        if (list == null) {
            return null;
        }

        List<MediaItem> result = new ArrayList<>();
        if (createItem) {
            for (ParcelImpl parcel : list) {
                MediaItem item = MediaUtils.fromParcelable(parcel);
                result.add(new FileMediaItem.Builder(new FileDescriptor())
                        .setMetadata(item.getMetadata())
                        .build());
            }
        } else {
            for (ParcelImpl parcel : list) {
                result.add((MediaItem) MediaUtils.fromParcelable(parcel));
            }
        }
        return result;
    }

    public static ControllerInfo getTestControllerInfo(MediaSession session) {
        if (session == null) {
            return null;
        }
        for (ControllerInfo info : session.getConnectedControllers()) {
            if (CLIENT_PACKAGE_NAME.equals(info.getPackageName())) {
                return info;
            }
        }
        Log.e(TAG, "Test controller was not found in connected controllers. session=" + session);
        return null;
    }

    public static LibraryParams createLibraryParams() {
        String callingTestName = Thread.currentThread().getStackTrace()[3].getMethodName();

        Bundle extras = new Bundle();
        extras.putString(callingTestName, callingTestName);
        return new LibraryParams.Builder().setExtras(extras).build();
    }

    public static void assertEqualLibraryParams(LibraryParams a, LibraryParams b) {
        if (a == null || b == null) {
            assertEquals(a, b);
        } else {
            assertTrue(TestUtils.equals(a.getExtras(), b.getExtras()));
        }
    }

    public static void assertEqualLibraryParams(LibraryParams params, Bundle rootExtras) {
        if (params == null || rootExtras == null) {
            assertEquals(params, rootExtras);
        } else {
            assertEquals(params.isRecent(), rootExtras.getBoolean(BrowserRoot.EXTRA_RECENT));
            assertEquals(params.isOffline(), rootExtras.getBoolean(BrowserRoot.EXTRA_OFFLINE));
            assertEquals(params.isSuggested(), rootExtras.getBoolean(BrowserRoot.EXTRA_SUGGESTED));
            assertTrue(TestUtils.contains(rootExtras, params.getExtras()));
        }
    }
}
