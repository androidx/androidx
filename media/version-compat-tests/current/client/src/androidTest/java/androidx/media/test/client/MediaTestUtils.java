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

package androidx.media.test.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media.test.lib.TestUtils;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaLibraryService2.LibraryParams;
import androidx.media2.MediaMetadata2;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for tests.
 */
public final class MediaTestUtils {

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
            list.add(new FileMediaItem2.Builder(new FileDescriptor())
                    .setMetadata(new MediaMetadata2.Builder()
                            .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID,
                                    caller + "_item_" + (i + 1)).build())
                    .build());
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
                .putString(MediaMetadata2.METADATA_KEY_MEDIA_ID, mediaId).build();
    }

    public static ArrayList<Parcelable> playlistToParcelableArrayList(List<MediaItem2> playlist) {
        if (playlist == null) {
            return null;
        }
        ArrayList<Parcelable> result = new ArrayList<>();
        for (MediaItem2 item : playlist) {
            result.add(item.toBundle());
        }
        return result;
    }

    public static List<Bundle> mediaItem2ListToBundleList(List<MediaItem2> list) {
        if (list == null) {
            return null;
        }
        List<Bundle> result = new ArrayList<>();
        for (MediaItem2 item : list) {
            result.add(item.toBundle());
        }
        return result;
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

    public static void assertMediaItemWithId(String expectedId, MediaItem2 item) {
        assertNotNull(item);
        assertNotNull(item.getMetadata());
        assertEquals(expectedId, item.getMetadata().getString(
                MediaMetadata2.METADATA_KEY_MEDIA_ID));
    }

    public static void assertPaginatedListWithIds(List<String> fullIdList, int page, int pageSize,
            List<MediaItem2> paginatedList) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min((page + 1) * pageSize, fullIdList.size());
        // Compare the given results with originals.
        for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
            int relativeIndex = originalIndex - fromIndex;
            assertMediaItemWithId(fullIdList.get(originalIndex),
                    paginatedList.get(relativeIndex));
        }
    }

    public static void assertMediaItemsWithId(MediaItem2 a, MediaItem2 b) {
        assertEquals(a.getMetadata().getString(MediaMetadata2.METADATA_KEY_MEDIA_ID),
                b.getMetadata().getString(MediaMetadata2.METADATA_KEY_MEDIA_ID));
    }

    public static void assertMediaItemListsWithId(List<MediaItem2> a, List<MediaItem2> b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertMediaItemsWithId(a.get(i), b.get(i));
        }
    }
}
