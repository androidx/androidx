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

import android.os.Bundle;
import android.os.Parcelable;

import androidx.media.DataSourceDesc;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaSession2.CommandButton;

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
            list.add(new MediaItem2.Builder(MediaItem2.FLAG_PLAYABLE)
                    .setMediaId(caller + "_item_" + (size + 1))
                    .setDataSourceDesc(createDSD()).build());
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
        return new MediaItem2.Builder(MediaItem2.FLAG_PLAYABLE)
                .setMetadata(createMetadata()).setDataSourceDesc(createDSD()).build();
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

    private static DataSourceDesc createDSD() {
        return new DataSourceDesc.Builder().setDataSource(new FileDescriptor()).build();
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

    public static List<MediaItem2> playlistFromParcelableList(List<Parcelable> parcelables,
            boolean createDsd) {
        if (parcelables == null) {
            return null;
        }

        List<MediaItem2> result = new ArrayList<>();
        if (createDsd) {
            for (Parcelable itemBundle : parcelables) {
                MediaItem2 item = MediaItem2.fromBundle((Bundle) itemBundle);
                result.add(new MediaItem2.Builder(item.getFlags())
                        .setMediaId(item.getMediaId())
                        .setMetadata(item.getMetadata())
                        .setDataSourceDesc(new DataSourceDesc.Builder()
                                .setDataSource(new FileDescriptor())
                                .build())
                        .build());
            }
        } else {
            for (Parcelable itemBundle : parcelables) {
                result.add(MediaItem2.fromBundle((Bundle) itemBundle));
            }
        }
        return result;
    }

    public static List<CommandButton> buttonListFromParcelableArrayList(
            List<Parcelable> parcelables) {
        if (parcelables == null) {
            return null;
        }
        List<CommandButton> result = new ArrayList<>();
        for (Parcelable button : parcelables) {
            result.add(CommandButton.fromBundle((Bundle) button));
        }
        return result;
    }
}
