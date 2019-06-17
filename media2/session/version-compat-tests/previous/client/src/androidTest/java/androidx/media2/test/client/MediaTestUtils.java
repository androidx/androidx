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

package androidx.media2.test.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.media.MediaBrowserServiceCompat.BrowserRoot;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.MediaParcelUtils;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaLibraryService.LibraryParams;
import androidx.media2.test.common.TestUtils;
import androidx.versionedparcelable.ParcelImpl;

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
//            case MockMediaSessionService.ID:
//                return new SessionToken2(context, new ComponentName(
//                        context.getPackageName(), MockMediaSessionService.class.getName()));
//            case MockMediaLibraryService.ID:
//                return new SessionToken2(context, new ComponentName(
//                        context.getPackageName(), MockMediaLibraryService.class.getName()));
//        }
//        fail("Unknown id=" + id);
//        return null;
//    }

    /**
     * Create a list of {@link FileMediaItem} for testing purpose.
     * <p>
     * Caller's method name will be used for prefix of each media item's media id.
     *
     * @param size list size
     * @return the newly created playlist
     */
    public static List<MediaItem> createFileMediaItems(int size) {
        final List<MediaItem> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[1].getMethodName();
        for (int i = 0; i < size; i++) {
            list.add(new FileMediaItem.Builder(ParcelFileDescriptor.adoptFd(-1))
                    .setMetadata(new MediaMetadata.Builder()
                            .putString(MediaMetadata.METADATA_KEY_MEDIA_ID,
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
    public static MediaItem createFileMediaItemWithMetadata() {
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
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId).build();
    }

    public static List<SessionPlayer.TrackInfo> createTrackInfoList() {
        List<SessionPlayer.TrackInfo> list = new ArrayList<>();
        list.add(createTrackInfo(0, "test_0", SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_VIDEO));
        list.add(createTrackInfo(1, "test_1", SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO));
        list.add(createTrackInfo(2, "test_2", SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE));
        return list;
    }

    public static SessionPlayer.TrackInfo createTrackInfo(int index, String mediaId,
            int trackType) {
        MediaMetadata metadata = new MediaMetadata.Builder().putString(
                MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId).build();
        MediaItem mediaItem = new MediaItem.Builder().setMetadata(metadata).build();
        MediaFormat format = null;
        if (trackType == SessionPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
            format = new MediaFormat();
            format.setString(MediaFormat.KEY_LANGUAGE, "eng");
            format.setString(MediaFormat.KEY_MIME, "text/cea-608");
            format.setInteger(MediaFormat.KEY_IS_FORCED_SUBTITLE, 1);
            format.setInteger(MediaFormat.KEY_IS_AUTOSELECT, 0);
            format.setInteger(MediaFormat.KEY_IS_DEFAULT, 1);
        }
        return new SessionPlayer.TrackInfo(index, mediaItem, trackType, format);
    }

    public static List<ParcelImpl> convertToParcelImplList(List<MediaItem> list) {
        if (list == null) {
            return null;
        }
        List<ParcelImpl> result = new ArrayList<>();
        for (MediaItem item : list) {
            result.add(MediaParcelUtils.toParcelable(item));
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
            assertEquals(a.isRecent(), b.isRecent());
            assertEquals(a.isOffline(), b.isOffline());
            assertEquals(a.isSuggested(), b.isSuggested());
            assertTrue(TestUtils.equals(a.getExtras(), b.getExtras()));
        }
    }

    public static void assertLibraryParamsEquals(LibraryParams params, Bundle rootExtras) {
        if (params == null || rootExtras == null) {
            assertEquals(params, rootExtras);
        } else {
            assertEquals(params.isRecent(), rootExtras.getBoolean(BrowserRoot.EXTRA_RECENT));
            assertEquals(params.isOffline(), rootExtras.getBoolean(BrowserRoot.EXTRA_OFFLINE));
            assertEquals(params.isSuggested(), rootExtras.getBoolean(BrowserRoot.EXTRA_SUGGESTED));
            assertTrue(TestUtils.contains(rootExtras, params.getExtras()));
        }
    }

    public static void assertMediaItemHasId(MediaItem item, String expectedId) {
        assertNotNull(item);
        assertNotNull(item.getMetadata());
        assertEquals(expectedId, item.getMetadata().getString(
                MediaMetadata.METADATA_KEY_MEDIA_ID));
    }

    public static void assertPaginatedListHasIds(List<MediaItem> paginatedList,
            List<String> fullIdList, int page, int pageSize) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min((page + 1) * pageSize, fullIdList.size());
        // Compare the given results with originals.
        for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
            int relativeIndex = originalIndex - fromIndex;
            assertMediaItemHasId(paginatedList.get(relativeIndex), fullIdList.get(originalIndex));
        }
    }

    public static void assertMediaIdEquals(MediaItem a, MediaItem b) {
        assertEquals(a.getMetadata().getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
                b.getMetadata().getString(MediaMetadata.METADATA_KEY_MEDIA_ID));
    }

    public static void assertMediaIdEquals(List<MediaItem> a, List<MediaItem> b) {
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertMediaIdEquals(a.get(i), b.get(i));
        }
    }

    public static void assertNotMediaItemSubclass(List<MediaItem> list) {
        for (MediaItem item : list) {
            assertNotMediaItemSubclass(item);
        }
    }

    public static void assertNotMediaItemSubclass(MediaItem item) {
        assertEquals(MediaItem.class, item.getClass());
    }
}
