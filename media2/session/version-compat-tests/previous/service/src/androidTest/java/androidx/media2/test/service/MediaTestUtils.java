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

package androidx.media2.test.service;

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.test.common.TestUtils;
import androidx.versionedparcelable.ParcelImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
            list.add(createMediaItem(caller + "_item_" + (i + 1)));
        }
        return list;
    }

    public static MediaItem createMediaItem(String id) {
        return new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1))
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
            list.add(caller + "_item_" + (i + 1));
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
        return new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1))
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
                MediaItem item = MediaParcelUtils.fromParcelable(parcel);
                result.add(new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1))
                        .setMetadata(item.getMetadata())
                        .build());
            }
        } else {
            for (ParcelImpl parcel : list) {
                result.add((MediaItem) MediaParcelUtils.fromParcelable(parcel));
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

    // Note: It's not assertEquals() to avoid issue with the static import of JUnit's assertEquals.
    // Otherwise, this API hides the statically imported JUnit's assertEquals and compile will fail.
    public static void assertMediaMetadataEquals(MediaMetadata expected, MediaMetadata actual) {
        if (expected == null || actual == null) {
            assertEquals(expected, actual);
        } else {
            Set<String> expectedKeySet = expected.keySet();
            Set<String> actualKeySet = actual.keySet();

            assertEquals(expectedKeySet, actualKeySet);
            for (String key : expectedKeySet) {
                assertEquals(expected.getObject(key), actual.getObject(key));
            }
        }
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
