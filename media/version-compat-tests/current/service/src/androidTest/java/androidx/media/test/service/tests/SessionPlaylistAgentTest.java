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

package androidx.media.test.service.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Process;
import android.support.mediacompat.service.R;

import androidx.media.test.service.MediaTestUtils;
import androidx.media2.DataSourceDesc2;
import androidx.media2.FileDataSourceDesc2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaPlayer2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.MediaPlaylistAgent;
import androidx.media2.MediaPlaylistAgent.PlaylistEventCallback;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.OnDataSourceMissingHelper;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionPlaylistAgentImplBase;
import androidx.media2.UriDataSourceDesc2;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SessionPlaylistAgentTest extends MediaSession2TestBase {
    // Expected success
    private static final int WAIT_TIME_MS = 300;
    private static final int INVALID_SHUFFLE_MODE = -1000;
    private static final int INVALID_REPEAT_MODE = -1000;

    private static final int VIDEO_RES_1 =
            R.raw.video_480x360_mp4_h264_500kbps_30fps_aac_stereo_128kbps_44100hz;
    private static final int VIDEO_RES_2 =
            R.raw.video_480x360_mp4_h264_500kbps_25fps_aac_stereo_128kbps_44100hz;

    private MediaPlayer2 mMediaPlayer;
    private MediaPlayerConnector mBasePlayer;
    private MediaSession2 mSession;
    private SessionPlaylistAgentImplBase mPlaylistAgent;
    private OnDataSourceMissingHelper mDataSourceHelper;
    private PlaylistEventCallbackLatch mPlaylistEventCallback;

    private Resources mResources;
    private List<AssetFileDescriptor> mFdsToClose = new ArrayList<>();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mMediaPlayer = MediaPlayer2.create(mContext);
                mBasePlayer = mMediaPlayer.getMediaPlayerConnector();
            }
        });

        mDataSourceHelper = new MyDataSourceHelper();
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mBasePlayer)
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            MediaSession2.ControllerInfo controller) {
                        if (Process.myUid() == controller.getUid()) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                }).build();
        mSession.setOnDataSourceMissingHelper(mDataSourceHelper);

        mPlaylistEventCallback = new PlaylistEventCallbackLatch(1);
        mPlaylistAgent = (SessionPlaylistAgentImplBase) mSession.getPlaylistAgent();
        mPlaylistAgent.registerPlaylistEventCallback(sHandlerExecutor, mPlaylistEventCallback);

        mResources = mContext.getResources();
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        if (mSession != null) {
            mSession.close();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.close();
            mMediaPlayer = null;
        }
        for (AssetFileDescriptor afd : mFdsToClose) {
            afd.close();
        }
    }

    @Test
    public void testSetAndGetShuflleMode() throws Exception {
        int shuffleMode = mPlaylistAgent.getShuffleMode();
        if (shuffleMode != MediaPlaylistAgent.SHUFFLE_MODE_NONE) {
            mPlaylistEventCallback.resetCount(1);
            mPlaylistAgent.setShuffleMode(MediaPlaylistAgent.SHUFFLE_MODE_NONE);
            assertTrue(mPlaylistEventCallback.await());
            assertTrue(mPlaylistEventCallback.onShuffleModeChangedCalled);
            assertEquals(MediaPlaylistAgent.SHUFFLE_MODE_NONE, mPlaylistEventCallback.shuffleMode);
            assertEquals(MediaPlaylistAgent.SHUFFLE_MODE_NONE, mPlaylistAgent.getShuffleMode());
        }

        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setShuffleMode(MediaPlaylistAgent.SHUFFLE_MODE_ALL);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onShuffleModeChangedCalled);
        assertEquals(MediaPlaylistAgent.SHUFFLE_MODE_ALL, mPlaylistEventCallback.shuffleMode);
        assertEquals(MediaPlaylistAgent.SHUFFLE_MODE_ALL, mPlaylistAgent.getShuffleMode());

        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setShuffleMode(MediaPlaylistAgent.SHUFFLE_MODE_GROUP);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onShuffleModeChangedCalled);
        assertEquals(MediaPlaylistAgent.SHUFFLE_MODE_GROUP, mPlaylistEventCallback.shuffleMode);
        assertEquals(MediaPlaylistAgent.SHUFFLE_MODE_GROUP, mPlaylistAgent.getShuffleMode());

        // INVALID_SHUFFLE_MODE will not change the shuffle mode.
        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setShuffleMode(INVALID_SHUFFLE_MODE);
        assertFalse(mPlaylistEventCallback.await());
    }

    @Test
    public void testSetAndGetRepeatMode() throws Exception {
        int repeatMode = mPlaylistAgent.getRepeatMode();
        if (repeatMode != MediaPlaylistAgent.REPEAT_MODE_NONE) {
            mPlaylistEventCallback.resetCount(1);
            mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_NONE);
            assertTrue(mPlaylistEventCallback.await());
            assertTrue(mPlaylistEventCallback.onRepeatModeChangedCalled);
            assertEquals(MediaPlaylistAgent.REPEAT_MODE_NONE, mPlaylistEventCallback.repeatMode);
            assertEquals(MediaPlaylistAgent.REPEAT_MODE_NONE, mPlaylistAgent.getRepeatMode());
        }

        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_ONE);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onRepeatModeChangedCalled);
        assertEquals(MediaPlaylistAgent.REPEAT_MODE_ONE, mPlaylistEventCallback.repeatMode);
        assertEquals(MediaPlaylistAgent.REPEAT_MODE_ONE, mPlaylistAgent.getRepeatMode());

        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_ALL);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onRepeatModeChangedCalled);
        assertEquals(MediaPlaylistAgent.REPEAT_MODE_ALL, mPlaylistEventCallback.repeatMode);
        assertEquals(MediaPlaylistAgent.REPEAT_MODE_ALL, mPlaylistAgent.getRepeatMode());

        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_GROUP);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onRepeatModeChangedCalled);
        assertEquals(MediaPlaylistAgent.REPEAT_MODE_GROUP, mPlaylistEventCallback.repeatMode);
        assertEquals(MediaPlaylistAgent.REPEAT_MODE_GROUP, mPlaylistAgent.getRepeatMode());

        // INVALID_SHUFFLE_MODE will not change the shuffle mode.
        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setRepeatMode(INVALID_REPEAT_MODE);
        assertFalse(mPlaylistEventCallback.await());
    }

    @Test
    public void testSetPlaylist() throws Exception {
        int listSize = 10;
        createAndSetPlaylist(10);
        assertEquals(listSize, mPlaylistAgent.getPlaylist().size());
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());
    }

    @Test
    public void testSkipItems() throws Exception {
        int listSize = 5;
        List<MediaItem2> playlist = createAndSetPlaylist(listSize);

        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_NONE);
        // Test skipToPlaylistItem
        for (int i = listSize - 1; i >= 0; --i) {
            mPlaylistAgent.skipToPlaylistItem(playlist.get(i));
            assertEquals(i, mPlaylistAgent.getCurShuffledIndex());
        }

        // Test skipToNextItem
        // curPlayPos = 0
        for (int curPlayPos = 0; curPlayPos < listSize - 1; ++curPlayPos) {
            mPlaylistAgent.skipToNextItem();
            assertEquals(curPlayPos + 1, mPlaylistAgent.getCurShuffledIndex());
        }
        mPlaylistAgent.skipToNextItem();
        assertEquals(listSize - 1, mPlaylistAgent.getCurShuffledIndex());

        // Test skipToPrevious
        // curPlayPos = listSize - 1
        for (int curPlayPos = listSize - 1; curPlayPos > 0; --curPlayPos) {
            mPlaylistAgent.skipToPreviousItem();
            assertEquals(curPlayPos - 1, mPlaylistAgent.getCurShuffledIndex());
        }
        mPlaylistAgent.skipToPreviousItem();
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_ALL);
        // Test skipToPrevious with repeat mode all
        // curPlayPos = 0
        mPlaylistAgent.skipToPreviousItem();
        assertEquals(listSize - 1, mPlaylistAgent.getCurShuffledIndex());

        // Test skipToNext with repeat mode all
        // curPlayPos = listSize - 1
        mPlaylistAgent.skipToNextItem();
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.skipToPreviousItem();
        // curPlayPos = listSize - 1, nextPlayPos = 0
        // Test next play pos after setting repeat mode none.
        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_NONE);
        assertEquals(listSize - 1, mPlaylistAgent.getCurShuffledIndex());
    }

    @Test
    public void testEditPlaylist() throws Exception {
        int listSize = 5;
        List<MediaItem2> playlist = createAndSetPlaylist(listSize);

        // Test add item: [0 (cur), 1, 2, 3, 4] -> [0 (cur), 1, 5, 2, 3, 4]
        mPlaylistEventCallback.resetCount(1);
        MediaItem2 item5 = generateMediaItem(5);
        playlist.add(2, item5);
        mPlaylistAgent.addPlaylistItem(2, item5);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());

        mPlaylistEventCallback.resetCount(1);
        // Move current: [0 (cur), 1, 5, 2, 3, 4] -> [0, 1, 5 (cur), 2, 3, 4]
        mPlaylistAgent.skipToPlaylistItem(item5);
        // Remove current item: [0, 1, 5 (cur), 2, 3, 4] -> [0, 1, 2 (cur), 3, 4]
        playlist.remove(item5);
        mPlaylistAgent.removePlaylistItem(item5);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(2, mPlaylistAgent.getCurShuffledIndex());

        // Remove previous item: [0, 1, 2 (cur), 3, 4] -> [0, 2 (cur), 3, 4]
        mPlaylistEventCallback.resetCount(1);
        MediaItem2 previousItem = playlist.get(1);
        playlist.remove(previousItem);
        mPlaylistAgent.removePlaylistItem(previousItem);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Remove next item: [0, 2 (cur), 3, 4] -> [0, 2 (cur), 4]
        mPlaylistEventCallback.resetCount(1);
        MediaItem2 nextItem = playlist.get(2);
        playlist.remove(nextItem);
        mPlaylistAgent.removePlaylistItem(nextItem);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Replace item: [0, 2 (cur), 4] -> [0, 2 (cur), 5]
        mPlaylistEventCallback.resetCount(1);
        playlist.set(2, item5);
        mPlaylistAgent.replacePlaylistItem(2, item5);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Move last and remove the last item: [0, 2 (cur), 5] -> [0, 2, 5 (cur)] -> [0, 2 (cur)]
        MediaItem2 lastItem = playlist.get(1);
        mPlaylistAgent.skipToPlaylistItem(lastItem);
        mPlaylistEventCallback.resetCount(1);
        playlist.remove(lastItem);
        mPlaylistAgent.removePlaylistItem(lastItem);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Remove all items
        for (int i = playlist.size() - 1; i >= 0; --i) {
            MediaItem2 item = playlist.get(i);
            mPlaylistAgent.skipToPlaylistItem(item);
            mPlaylistEventCallback.resetCount(1);
            playlist.remove(item);
            mPlaylistAgent.removePlaylistItem(item);
            assertTrue(mPlaylistEventCallback.await());
            assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
            assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        }
        assertEquals(SessionPlaylistAgentImplBase.NO_VALID_ITEMS,
                mPlaylistAgent.getCurShuffledIndex());
    }

    @Test
    public void testPlaylistWithInvalidItem() throws Exception {
        int listSize = 2;
        List<MediaItem2> playlist = createAndSetPlaylist(listSize);

        // Add item: [0 (cur), 1] -> [0 (cur), 3 (no_dsd), 1]
        mPlaylistEventCallback.resetCount(1);
        MediaItem2 invalidItem2 = generateMediaItemWithoutDataSourceDesc(2);
        playlist.add(1, invalidItem2);
        mPlaylistAgent.addPlaylistItem(1, invalidItem2);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        // Test skip to next item:  [0 (cur), 2 (no_dsd), 1] -> [0, 2 (no_dsd), 1 (cur)]
        mPlaylistAgent.skipToNextItem();
        assertEquals(2, mPlaylistAgent.getCurShuffledIndex());

        // Test skip to previous item: [0, 2 (no_dsd), 1 (cur)] -> [0 (cur), 2 (no_dsd), 1]
        mPlaylistAgent.skipToPreviousItem();
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        // Remove current item: [0 (cur), 2 (no_dsd), 1] -> [2 (no_dsd), 1 (cur)]
        mPlaylistEventCallback.resetCount(1);
        MediaItem2 item = playlist.get(0);
        playlist.remove(item);
        mPlaylistAgent.removePlaylistItem(item);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Remove current item: [2 (no_dsd), 1 (cur)] -> [2 (no_dsd)]
        mPlaylistEventCallback.resetCount(1);
        item = playlist.get(1);
        playlist.remove(item);
        mPlaylistAgent.removePlaylistItem(item);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(SessionPlaylistAgentImplBase.NO_VALID_ITEMS,
                mPlaylistAgent.getCurShuffledIndex());

        // Add invalid item: [2 (no_dsd)] -> [0 (no_dsd), 2 (no_dsd)]
        MediaItem2 invalidItem0 = generateMediaItemWithoutDataSourceDesc(0);
        mPlaylistEventCallback.resetCount(1);
        playlist.add(0, invalidItem0);
        mPlaylistAgent.addPlaylistItem(0, invalidItem0);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(SessionPlaylistAgentImplBase.NO_VALID_ITEMS,
                mPlaylistAgent.getCurShuffledIndex());

        // Add valid item: [0 (no_dsd), 2 (no_dsd)] -> [0 (no_dsd), 1, 2 (no_dsd)]
        MediaItem2 invalidItem1 = generateMediaItem(1);
        mPlaylistEventCallback.resetCount(1);
        playlist.add(1, invalidItem1);
        mPlaylistAgent.addPlaylistItem(1, invalidItem1);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Replace the valid item with an invalid item:
        // [0 (no_dsd), 1 (cur), 2 (no_dsd)] -> [0 (no_dsd), 3 (no_dsd), 2 (no_dsd)]
        MediaItem2 invalidItem3 = generateMediaItemWithoutDataSourceDesc(3);
        mPlaylistEventCallback.resetCount(1);
        playlist.set(1, invalidItem3);
        mPlaylistAgent.replacePlaylistItem(1, invalidItem3);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        assertPlaylistEquals(playlist, mPlaylistAgent.getPlaylist());
        assertEquals(SessionPlaylistAgentImplBase.END_OF_PLAYLIST,
                mPlaylistAgent.getCurShuffledIndex());
    }

    @Test
    public void testPlaylistAfterSkipToNextItem() throws Exception {
        int listSize = 2;
        createAndSetPlaylist(listSize);
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.skipToNextItem();
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        // Will not go to the next if the next is end of the playlist
        mPlaylistAgent.skipToNextItem();
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_ALL);
        mPlaylistAgent.skipToNextItem();
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.skipToNextItem();
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());
    }

    @Test
    public void testPlaylistAfterSkipToPreviousItem() throws Exception {
        int listSize = 2;
        createAndSetPlaylist(listSize);
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        // Will not go to the previous if the next is the first one
        mPlaylistAgent.skipToPreviousItem();
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.setRepeatMode(MediaPlaylistAgent.REPEAT_MODE_ALL);
        mPlaylistAgent.skipToPreviousItem();
        assertEquals(1, mPlaylistAgent.getCurShuffledIndex());

        mPlaylistAgent.skipToPreviousItem();
        assertEquals(0, mPlaylistAgent.getCurShuffledIndex());
    }

    @Test
    public void testCurrentMediaItemChangedAfterSetPlayList() throws Exception {
        prepareLooper();
        final List<MediaItem2> list = new ArrayList<>();
        list.add(MediaTestUtils.createMediaItem("testItem1", createDataSourceDesc(VIDEO_RES_1)));
        list.add(MediaTestUtils.createMediaItem("testItem2", createDataSourceDesc(VIDEO_RES_2)));

        final CountDownLatch mediaItemChangedLatch = new CountDownLatch(1);

        try (MediaSession2 session = new MediaSession2.Builder(mContext)
                .setPlayer(mBasePlayer)
                .setId("testCurrentDataSourceChanged")
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public void onCurrentMediaItemChanged(MediaSession2 session,
                            MediaPlayerConnector player, MediaItem2 item) {
                        assertEquals(list.get(0), item);
                        mediaItemChangedLatch.countDown();
                    }
                }).build()) {

            session.setPlaylist(list, null);
            assertTrue(mediaItemChangedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

            // TODO: Check that MediaController2 in remote process is also notified.
        }
    }

    private DataSourceDesc2 createDataSourceDesc(int resid) throws Exception {
        AssetFileDescriptor afd = mResources.openRawResourceFd(resid);
        mFdsToClose.add(afd);
        return new FileDataSourceDesc2.Builder(
                afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength()).build();
    }

    private List<MediaItem2> createAndSetPlaylist(int listSize) throws Exception {
        List<MediaItem2> items = new ArrayList<>();
        for (int i = 0; i < listSize; ++i) {
            items.add(generateMediaItem(i));
        }
        mPlaylistEventCallback.resetCount(1);
        mPlaylistAgent.setPlaylist(items, null);
        assertTrue(mPlaylistEventCallback.await());
        assertTrue(mPlaylistEventCallback.onPlaylistChangedCalled);
        return items;
    }

    private void assertPlaylistEquals(List<MediaItem2> expected, List<MediaItem2> actual) {
        if (expected == actual) {
            return;
        }
        assertTrue(expected != null && actual != null);
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); ++i) {
            assertTrue(expected.get(i).equals(actual.get(i)));
        }
    }

    private MediaItem2 generateMediaItemWithoutDataSourceDesc(int key) {
        return new MediaItem2.Builder(0)
                .setMediaId("TEST_MEDIA_ID_WITHOUT_DSD_" + key)
                .build();
    }

    private MediaItem2 generateMediaItem(int key) {
        return new MediaItem2.Builder(0)
                .setMediaId("TEST_MEDIA_ID_" + key)
                .build();
    }

    public class PlaylistEventCallbackLatch extends PlaylistEventCallback {
        public boolean onPlaylistChangedCalled;
        public boolean onPlaylistMetadataChangedCalled;
        public boolean onRepeatModeChangedCalled;
        public boolean onShuffleModeChangedCalled;

        public int repeatMode;
        public int shuffleMode;
        public List<MediaItem2> playlist;
        public MediaMetadata2 metadata;

        private CountDownLatch mCountDownLatch;

        PlaylistEventCallbackLatch(int count) {
            super();
            mCountDownLatch = new CountDownLatch(count);
        }

        void resetCount(int count) {
            onPlaylistChangedCalled = false;
            onPlaylistMetadataChangedCalled = false;
            onRepeatModeChangedCalled = false;
            onShuffleModeChangedCalled = false;
            mCountDownLatch = new CountDownLatch(count);
        }

        void countDown() {
            mCountDownLatch.countDown();
        }

        boolean await() throws Exception {
            return mCountDownLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS);
        }

        public void onPlaylistChanged(MediaPlaylistAgent playlistAgent, List<MediaItem2> list,
                MediaMetadata2 metadata) {
            onPlaylistChangedCalled = true;
            playlist = list;
            mCountDownLatch.countDown();
        }

        public void onPlaylistMetadataChanged(MediaPlaylistAgent playlistAgent,
                MediaMetadata2 metadata) {
            onPlaylistMetadataChangedCalled = true;
            this.metadata = metadata;
            mCountDownLatch.countDown();
        }

        public void onRepeatModeChanged(MediaPlaylistAgent playlistAgent, int repeatMode) {
            onRepeatModeChangedCalled = true;
            this.repeatMode = repeatMode;
            mCountDownLatch.countDown();
        }

        public void onShuffleModeChanged(MediaPlaylistAgent playlistAgent, int shuffleMode) {
            onShuffleModeChangedCalled = true;
            this.shuffleMode = shuffleMode;
            mCountDownLatch.countDown();
        }
    }

    public class MyDataSourceHelper implements OnDataSourceMissingHelper {
        @Override
        public DataSourceDesc2 onDataSourceMissing(MediaSession2 session, MediaItem2 item) {
            if (item.getMediaId().contains("WITHOUT_DSD")) {
                return null;
            }
            return new UriDataSourceDesc2.Builder(mContext, Uri.parse("dsd://test"))
                    .setMediaId(item.getMediaId())
                    .build();
        }
    }
}
