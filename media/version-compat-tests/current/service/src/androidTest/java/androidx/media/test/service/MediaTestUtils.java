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
import android.os.Parcelable;
import android.util.Log;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.test.lib.TestUtils;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerInfo;

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
//    public static SessionToken2 getServiceToken(Context context, String id) {
//        switch (id) {
//            case MockMediaSessionService2.ID:
//                return new SessionToken2(context, new ComponentName(
//                        context.getPackageName(), MockMediaSessionService2.class.getName()));
//            case MockMediaLibraryService2.ID:
//                return new SessionToken2(context, new ComponentName(
//                        context.getPackageName(), MockMediaLibraryService2.class.getName()));
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
    public static List<MediaItem2> createPlaylist(int size) {
        final List<MediaItem2> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[1].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(createMediaItem(caller + "_item_" + (size + 1)));
        }
        return list;
    }

    public static MediaItem2 createMediaItem(String id) {
        return new FileMediaItem2.Builder(new FileDescriptor())
                .setMetadata(new MediaMetadata2.Builder()
                        .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, id)
                        .putLong(MediaMetadata2.METADATA_KEY_BROWSABLE,
                                MediaMetadata2.BROWSABLE_TYPE_NONE)
                        .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1)
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
    public static MediaItem2 createMediaItemWithMetadata() {
        return new FileMediaItem2.Builder(new FileDescriptor())
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
    public static MediaMetadata2 createMetadata() {
        String mediaId = Thread.currentThread().getStackTrace()[1].getMethodName();
        return new MediaMetadata2.Builder()
                .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId)
                .putLong(MediaMetadata2.METADATA_KEY_BROWSABLE, MediaMetadata2.BROWSABLE_TYPE_NONE)
                .putLong(MediaMetadata2.METADATA_KEY_PLAYABLE, 1)
                .build();
    }

    public static List<MediaItem2> playlistFromParcelableList(List<Parcelable> parcelables,
            boolean createItem) {
        if (parcelables == null) {
            return null;
        }

        List<MediaItem2> result = new ArrayList<>();
        if (createItem) {
            for (Parcelable itemBundle : parcelables) {
                MediaItem2 item = MediaItem2.fromBundle((Bundle) itemBundle);
                result.add(new FileMediaItem2.Builder(new FileDescriptor())
                        .setMetadata(item.getMetadata())
                        .build());
            }
        } else {
            for (Parcelable itemBundle : parcelables) {
                result.add(MediaItem2.fromBundle((Bundle) itemBundle));
            }
        }
        return result;
    }

    public static List<CommandButton> buttonListFromBundleList(List<Bundle> bundleList) {
        if (bundleList == null) {
            return null;
        }
        List<CommandButton> result = new ArrayList<>();
        for (int i = 0; i < bundleList.size(); i++) {
            result.add(CommandButton.fromBundle(bundleList.get(i)));
        }
        return result;
    }

    public static ControllerInfo getTestControllerInfo(MediaSession2 session2) {
        if (session2 == null) {
            return null;
        }
        for (ControllerInfo info : session2.getConnectedControllers()) {
            if (CLIENT_PACKAGE_NAME.equals(info.getPackageName())) {
                return info;
            }
        }
        Log.e(TAG, "Test controller was not found in connected controllers. session=" + session2);
        return null;
    }

    public static LibraryParams createLibraryParams() {
        String callingTestName = Thread.currentThread().getStackTrace()[3].getMethodName();

        Bundle extras = new Bundle();
        extras.putString(callingTestName, callingTestName);
        return new LibraryParams.Builder().setExtras(extras).build();
    }

    public static void assertLibraryParamsEquals(LibraryParams a, LibraryParams b) {
        if (a == null || b == null) {
            assertEquals(a, b);
        } else {
            assertTrue(TestUtils.equals(a.getExtras(), b.getExtras()));
        }
    }

    public static void assertLibraryParamsWithBundle(LibraryParams a, Bundle b) {
        if (a == null || b == null) {
            assertEquals(a, b);
        } else {
            assertEquals(a.isRecent(), b.getBoolean(BrowserRoot.EXTRA_RECENT));
            assertEquals(a.isOffline(), b.getBoolean(BrowserRoot.EXTRA_OFFLINE));
            assertEquals(a.isSuggested(), b.getBoolean(BrowserRoot.EXTRA_SUGGESTED));
            assertTrue(TestUtils.contains(b, a.getExtras()));
        }
    }
}
