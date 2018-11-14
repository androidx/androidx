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

package androidx.media2;

import static androidx.media2.MediaMetadata.BROWSABLE_TYPE_NONE;
import static androidx.media2.MediaMetadata.METADATA_KEY_BROWSABLE;
import static androidx.media2.MediaMetadata.METADATA_KEY_DURATION;
import static androidx.media2.MediaMetadata.METADATA_KEY_MEDIA_ID;
import static androidx.media2.MediaMetadata.METADATA_KEY_PLAYABLE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.core.util.ObjectsCompat;
import androidx.media2.MediaLibraryService.LibraryParams;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utilities for tests.
 */
public final class TestUtils {
    private static final int TIMEOUT_MS = 1000;

    /**
     * Finds the session with id in this test package.
     *
     * @param context
     * @param id
     * @return
     */
    public static SessionToken getServiceToken(Context context, String id) {
        switch (id) {
            case MockMediaSessionService.ID:
                return new SessionToken(context, new ComponentName(
                        context.getPackageName(), MockMediaSessionService.class.getName()));
            case MockMediaLibraryService.ID:
                return new SessionToken(context, new ComponentName(
                        context.getPackageName(), MockMediaLibraryService.class.getName()));
        }
        fail("Unknown id=" + id);
        return null;
    }

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
     * Create a list of media items for testing purpose
     * <p>
     * Caller's method name will be used for prefix of each media item's media id.
     *
     * @param size list size
     * @return the newly created media item list
     */
    public static List<MediaItem> createMediaItems(int size) {
        final List<MediaItem> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
        for (int i = 0; i < size; i++) {
            MediaItem item = new FileMediaItem.Builder(new FileDescriptor())
                    .setMetadata(new MediaMetadata.Builder()
                            .putString(METADATA_KEY_MEDIA_ID, caller + "_item_" + (i + 1))
                            .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                            .putLong(METADATA_KEY_PLAYABLE, 1)
                            .build())
                    .build();
            list.add(item);
        }
        return list;
    }

    /**
     * Create a list of media ids for testing purpose
     * <p>
     * Caller's method name will be used for prefix of media id.
     *
     * @param size list size
     * @return the newly created ids
     */
    public static List<String> createMediaIds(int size) {
        final List<String> list = new ArrayList<>();
        String caller = Thread.currentThread().getStackTrace()[3].getMethodName();
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
                .setMetadata(createMetadata()).build();
    }

    /**
     * Create a media metadata for testing purpose.
     * <p>
     * Caller's method name will be used for the media id.
     *
     * @return the newly created media item
     */
    public static MediaMetadata createMetadata() {
        String mediaId = Thread.currentThread().getStackTrace()[3].getMethodName();
        return new MediaMetadata.Builder()
                .putString(METADATA_KEY_MEDIA_ID, mediaId)
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                .putLong(METADATA_KEY_PLAYABLE, 1)
                .build();
    }

    /**
     * Create a media metadata for testing purpose.
     *
     * @return the newly created media item
     */
    public static MediaMetadata createMetadata(String mediaId, long duration) {
        return new MediaMetadata.Builder()
                .putString(METADATA_KEY_MEDIA_ID, mediaId)
                .putLong(METADATA_KEY_DURATION, duration)
                .putLong(METADATA_KEY_BROWSABLE, BROWSABLE_TYPE_NONE)
                .putLong(METADATA_KEY_PLAYABLE, 1)
                .build();
    }

    /**
     * Create a {@link MediaItem} with the id.
     *
     * @return the newly created media item.
     */
    public static MediaItem createMediaItem(String mediaId) {
        return new MediaItem.Builder().setMetadata(createMetadata(mediaId, 0)).build();
    }

    public static LibraryParams createLibraryParams() {
        String callingTestName = Thread.currentThread().getStackTrace()[3].getMethodName();

        Bundle extras = new Bundle();
        extras.putString(callingTestName, callingTestName);
        return new LibraryParams.Builder().setExtras(extras).build();
    }

    /**
     * Asserts if two lists equals
     *
     * @param a a list
     * @param b another list
     */
    public static void assertMediaItemListEquals(List<MediaItem> a, List<MediaItem> b) {
        if (a == null || b == null) {
            assertEquals(a, b);
        }
        assertEquals(a.size(), b.size());

        for (int i = 0; i < a.size(); i++) {
            MediaItem aItem = a.get(i);
            MediaItem bItem = b.get(i);

            if (aItem == null || bItem == null) {
                assertEquals(aItem, bItem);
                continue;
            }

            assertEquals(aItem.getMediaId(), bItem.getMediaId());
            TestUtils.assertMetadataEquals(aItem.getMetadata(), bItem.getMetadata());

            // Note: Here it does not check whether MediaItem are equal,
            // since there DataSourceDec is not comparable.
        }
    }

    public static void assertPaginatedListEquals(List<MediaItem> fullList, int page, int pageSize,
            List<MediaItem> paginatedList) {
        int fromIndex = page * pageSize;
        int toIndex = Math.min((page + 1) * pageSize, fullList.size());
        // Compare the given results with originals.
        for (int originalIndex = fromIndex; originalIndex < toIndex; originalIndex++) {
            int relativeIndex = originalIndex - fromIndex;
            assertMediaItemEquals(fullList.get(originalIndex), paginatedList.get(relativeIndex));
        }
    }

    public static void assertMetadataEquals(MediaMetadata expected, MediaMetadata actual) {
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

    public static void assertMediaItemWithId(String expectedId, MediaItem item) {
        assertNotNull(item);
        assertNotNull(item.getMetadata());
        assertEquals(expectedId, item.getMetadata().getString(
                METADATA_KEY_MEDIA_ID));
    }

    public static void assertMediaItemEquals(MediaItem a, MediaItem b) {
        if (a == null || b == null) {
            assertEquals(a, b);
        } else {
            assertMetadataEquals(a.getMetadata(), b.getMetadata());
        }
    }

    public static void assertLibraryParamsEquals(LibraryParams a, LibraryParams b) {
        if (a == null || b == null) {
            assertEquals(a, b);
        } else {
            assertEquals(a.isSuggested(), b.isSuggested());
            assertEquals(a.isOffline(), b.isOffline());
            assertEquals(a.isRecent(), b.isRecent());
            assertTrue(TestUtils.equals(a.getExtras(), b.getExtras()));
        }
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
                assertTrue(latch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
            }
        }
    }

    public static class Monitor {
        private int mNumSignal;

        public synchronized void reset() {
            mNumSignal = 0;
        }

        public synchronized void signal() {
            mNumSignal++;
            notifyAll();
        }

        public synchronized boolean waitForSignal() throws InterruptedException {
            return waitForCountedSignals(1) > 0;
        }

        public synchronized int waitForCountedSignals(int targetCount) throws InterruptedException {
            while (mNumSignal < targetCount) {
                wait();
            }
            return mNumSignal;
        }

        public synchronized boolean waitForSignal(long timeoutMs) throws InterruptedException {
            return waitForCountedSignals(1, timeoutMs) > 0;
        }

        public synchronized int waitForCountedSignals(int targetCount, long timeoutMs)
                throws InterruptedException {
            if (timeoutMs == 0) {
                return waitForCountedSignals(targetCount);
            }
            long deadline = System.currentTimeMillis() + timeoutMs;
            while (mNumSignal < targetCount) {
                long delay = deadline - System.currentTimeMillis();
                if (delay <= 0) {
                    break;
                }
                wait(delay);
            }
            return mNumSignal;
        }

        public synchronized boolean isSignalled() {
            return mNumSignal >= 1;
        }

        public synchronized int getNumSignal() {
            return mNumSignal;
        }
    }
}
