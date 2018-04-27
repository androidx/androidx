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

import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.util.ObjectsCompat;
import androidx.media.DataSourceDesc;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for tests.
 */
public final class TestUtils {
    private static final int WAIT_TIME_MS = 1000;
    private static final int WAIT_SERVICE_TIME_MS = 5000;

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
     * Compares contents of two bundles.
     *
     * @param a a bundle
     * @param b another bundle
     * @return {@code true} if two bundles are the same. {@code false} otherwise. This may be
     *     incorrect if any bundle contains a bundle.
     */
    public static boolean equals(Bundle a, Bundle b) {
        return contains(a, b) && contains(b, a);
    }

    /**
     * Checks whether a Bundle contains another bundle.
     *
     * @param a a bundle
     * @param b another bundle
     * @return {@code true} if a contains b. {@code false} otherwise. This may be incorrect if any
     *      bundle contains a bundle.
     */
    public static boolean contains(Bundle a, Bundle b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return b == null;
        }
        if (!a.keySet().containsAll(b.keySet())) {
            return false;
        }
        for (String key : b.keySet()) {
            if (!ObjectsCompat.equals(a.get(key), b.get(key))) {
                return false;
            }
        }
        return true;
    }

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

    /**
     * Create a bundle for testing purpose.
     *
     * @return the newly created bundle.
     */
    public static Bundle createTestBundle() {
        Bundle bundle = new Bundle();
        bundle.putString("test_key", "test_value");
        return bundle;
    }

    /**
     * Handler that always waits until the Runnable finishes.
     */
    public static class SyncHandler extends Handler {
        public SyncHandler(Looper looper) {
            super(looper);
        }

        public void postAndSync(final Runnable runnable) throws InterruptedException {
            if (getLooper() == Looper.myLooper()) {
                runnable.run();
            } else {
                final CountDownLatch latch = new CountDownLatch(1);
                post(new Runnable() {
                    @Override
                    public void run() {
                        runnable.run();
                        latch.countDown();
                    }
                });
                assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
            }
        }
    }
}
