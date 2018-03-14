/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.supportv4.media.utils;

import static com.example.android.supportv4.media.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE;
import static com.example.android.supportv4.media.utils.MediaIDHelper.MEDIA_ID_MUSICS_BY_SEARCH;

import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import com.example.android.supportv4.media.model.MusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class to help on queue related tasks.
 */
public class QueueHelper {

    private static final String TAG = "QueueHelper";

    public static List<MediaSessionCompat.QueueItem> getPlayingQueue(String mediaId,
            MusicProvider musicProvider) {

        // extract the browsing hierarchy from the media ID:
        String[] hierarchy = MediaIDHelper.getHierarchy(mediaId);

        if (hierarchy.length != 2) {
            Log.e(TAG, "Could not build a playing queue for this mediaId: " + mediaId);
            return null;
        }

        String categoryType = hierarchy[0];
        String categoryValue = hierarchy[1];
        Log.d(TAG, "Creating playing queue for " + categoryType + ",  " + categoryValue);

        Iterable<MediaMetadataCompat> tracks = null;
        // This sample only supports genre and by_search category types.
        if (categoryType.equals(MEDIA_ID_MUSICS_BY_GENRE)) {
            tracks = musicProvider.getMusicsByGenre(categoryValue);
        } else if (categoryType.equals(MEDIA_ID_MUSICS_BY_SEARCH)) {
            tracks = musicProvider.searchMusic(categoryValue);
        }

        if (tracks == null) {
            Log.e(TAG, "Unrecognized category type: " + categoryType + " for mediaId " + mediaId);
            return null;
        }

        return convertToQueue(tracks, hierarchy[0], hierarchy[1]);
    }

    public static List<MediaSessionCompat.QueueItem> getPlayingQueueFromSearch(String query,
            MusicProvider musicProvider) {

        Log.d(TAG, "Creating playing queue for musics from search " + query);

        return convertToQueue(musicProvider.searchMusic(query), MEDIA_ID_MUSICS_BY_SEARCH, query);
    }


    public static int getMusicIndexOnQueue(Iterable<MediaSessionCompat.QueueItem> queue,
             String mediaId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSessionCompat.QueueItem> queue,
             long queueId) {
        int index = 0;
        for (MediaSessionCompat.QueueItem item : queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static List<MediaSessionCompat.QueueItem> convertToQueue(
            Iterable<MediaMetadataCompat> tracks, String... categories) {
        List<MediaSessionCompat.QueueItem> queue = new ArrayList<>();
        int count = 0;
        for (MediaMetadataCompat track : tracks) {

            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            String hierarchyAwareMediaID = MediaIDHelper.createMediaID(
                    track.getDescription().getMediaId(), categories);

            MediaMetadataCompat trackCopy = new MediaMetadataCompat.Builder(track)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                    .build();

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSessionCompat.QueueItem item = new MediaSessionCompat.QueueItem(
                    trackCopy.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }

    /**
     * Create a random queue. For simplicity sake, instead of a random queue, we create a
     * queue using the first genre.
     *
     * @param musicProvider the provider used for fetching music.
     * @return list containing {@link android.media.session.MediaSession.QueueItem}'s
     */
    public static List<MediaSessionCompat.QueueItem> getRandomQueue(MusicProvider musicProvider) {
        Iterator<String> genres = musicProvider.getGenres().iterator();
        if (!genres.hasNext()) {
            return Collections.emptyList();
        }
        String genre = genres.next();
        Iterable<MediaMetadataCompat> tracks = musicProvider.getMusicsByGenre(genre);

        return convertToQueue(tracks, MEDIA_ID_MUSICS_BY_GENRE, genre);
    }

    public static boolean isIndexPlayable(int index, List<MediaSessionCompat.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }
}
