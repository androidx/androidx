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

import static android.support.mediacompat.testlib.util.IntentUtil.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;

import androidx.media.test.lib.TestUtils;
import androidx.media.test.service.MediaTestUtils;
import androidx.media.test.service.MockPlayer;
import androidx.media.test.service.RemoteMediaController2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
import androidx.media2.MediaSession2;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionPlayer2;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether the methods of {@link SessionPlayer2} are triggered by the
 * session/controller.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SessionPlayerTest extends MediaSession2TestBase {

    MediaSession2 mSession;
    MockPlayer mPlayer;
    RemoteMediaController2 mController2;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);
        mSession = new MediaSession2.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            MediaSession2.ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public MediaItem2 onCreateMediaItem(MediaSession2 session,
                            ControllerInfo controller, String mediaId) {
                        return MediaTestUtils.createMediaItem(mediaId);
                    }
                }).build();

        // Create a default MediaController2 in client app.
        mController2 = createRemoteController2(mSession.getToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
    }

    @Test
    public void testPlayBySession() throws Exception {
        prepareLooper();
        mSession.getPlayer().play();
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPlayByController() {
        mController2.play();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPlayCalled);
    }

    @Test
    public void testPauseBySession() throws Exception {
        prepareLooper();
        mSession.getPlayer().pause();
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testPauseByController() {
        mController2.pause();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPauseCalled);
    }

    @Test
    public void testPrepareBySession() throws Exception {
        prepareLooper();
        mSession.getPlayer().prepare();
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void testPrepareByController() {
        mController2.prepare();
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mPrepareCalled);
    }

    @Test
    public void testSeekToBySession() throws Exception {
        prepareLooper();
        final long pos = 1004L;
        mSession.getPlayer().seekTo(pos);
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(pos, mPlayer.mSeekPosition);
    }

    @Test
    public void testSeekToByController() {
        final long seekPosition = 12125L;
        mController2.seekTo(seekPosition);
        try {
            assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        assertTrue(mPlayer.mSeekToCalled);
        assertEquals(seekPosition, mPlayer.mSeekPosition);
    }

    @Test
    public void testSetPlaybackSpeedBySession() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mSession.getPlayer().setPlaybackSpeed(speed);
        assertTrue(mPlayer.mSetPlaybackSpeedCalled);
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    @Test
    public void testSetPlaybackSpeedByController() throws Exception {
        final float speed = 1.5f;
        mController2.setPlaybackSpeed(speed);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    @Test
    public void testSetPlaylistBySession() {
        prepareLooper();
        final List<MediaItem2> list = MediaTestUtils.createPlaylist(2);
        mSession.getPlayer().setPlaylist(list, null);
        assertTrue(mPlayer.mSetPlaylistCalled);
        assertSame(list, mPlayer.mPlaylist);
        assertNull(mPlayer.mMetadata);
    }

    @Test
    public void testSetPlaylistByController() throws InterruptedException {
        final List<String> list = MediaTestUtils.createMediaIds(2);
        mController2.setPlaylist(list, null /* metadata */);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetPlaylistCalled);
        assertNull(mPlayer.mMetadata);

        assertNotNull(mPlayer.mPlaylist);
        assertEquals(list.size(), mPlayer.mPlaylist.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), mPlayer.mPlaylist.get(i).getMediaId());
        }
    }

    @Test
    @LargeTest
    public void testSetPlaylistByControllerWithLongPlaylist() throws InterruptedException {
        final int listSize = 5000;
        // Make client app to generate a long list, and call setPlaylist() with it.
        mController2.createAndSetDummyPlaylist(listSize, null /* metadata */);
        assertTrue(mPlayer.mCountDownLatch.await(10, TimeUnit.SECONDS));

        assertTrue(mPlayer.mSetPlaylistCalled);
        assertNull(mPlayer.mMetadata);

        assertNotNull(mPlayer.mPlaylist);
        assertEquals(listSize, mPlayer.mPlaylist.size());
        for (int i = 0; i < listSize; i++) {
            // Each item's media ID will be same as its index.
            assertEquals(TestUtils.getMediaIdInDummyList(i), mPlayer.mPlaylist.get(i).getMediaId());
        }
    }

    @Test
    public void testUpdatePlaylistMetadataBySession() {
        prepareLooper();
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();
        mSession.getPlayer().updatePlaylistMetadata(testMetadata);
        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertSame(testMetadata, mPlayer.mMetadata);
    }

    @Test
    public void testUpdatePlaylistMetadataByController() throws InterruptedException {
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();
        mController2.updatePlaylistMetadata(testMetadata);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertNotNull(mPlayer.mMetadata);
        assertEquals(testMetadata.getMediaId(), mPlayer.mMetadata.getMediaId());
    }

    @Test
    public void testAddPlaylistItemBySession() {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.getPlayer().addPlaylistItem(testIndex, testMediaItem);
        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void testAddPlaylistItemByController() throws InterruptedException {
        final int testIndex = 12;
        final String testMediaId = "testAddPlaylistItemByController";

        mController2.addPlaylistItem(testIndex, testMediaId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        // MediaController2.addPlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void testRemovePlaylistItemBySession() {
        prepareLooper();
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.getPlayer().removePlaylistItem(testMediaItem);
        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void testRemovePlaylistItemByController() throws InterruptedException {
        mPlayer.mPlaylist = MediaTestUtils.createPlaylist(2);
        MediaItem2 targetItem = mPlayer.mPlaylist.get(0);

        mController2.removePlaylistItem(0);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertEquals(targetItem, mPlayer.mItem);
    }

    @Test
    public void testReplacePlaylistItemBySession() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.getPlayer().replacePlaylistItem(testIndex, testMediaItem);
        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void testReplacePlaylistItemByController() throws InterruptedException {
        final int testIndex = 12;
        final String testMediaId = "testReplacePlaylistItemByController";

        mController2.replacePlaylistItem(testIndex, testMediaId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        // MediaController2.replacePlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void testSkipToPreviousItemBySession() {
        prepareLooper();
        mSession.getPlayer().skipToPreviousPlaylistItem();
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToPreviousItemByController() throws InterruptedException {
        mController2.skipToPreviousItem();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToNextItemBySession() throws Exception {
        prepareLooper();
        mSession.getPlayer().skipToNextPlaylistItem();
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToNextItemByController() throws InterruptedException {
        mController2.skipToNextItem();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToPlaylistItemBySession() throws Exception {
        prepareLooper();
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.getPlayer().skipToPlaylistItem(testMediaItem);
        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void testSkipToPlaylistItemByController() throws InterruptedException {
        mPlayer.mPlaylist = MediaTestUtils.createPlaylist(3);

        mController2.skipToPlaylistItem(2);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertEquals(mPlayer.mPlaylist.get(2), mPlayer.mItem);
    }

    @Test
    public void testSetShuffleModeBySession() {
        prepareLooper();
        final int testShuffleMode = SessionPlayer2.SHUFFLE_MODE_GROUP;
        mSession.getPlayer().setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    @Test
    public void testSetShuffleModeByController() throws InterruptedException {
        final int testShuffleMode = SessionPlayer2.SHUFFLE_MODE_GROUP;
        mController2.setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    @Test
    public void testSetRepeatModeBySession() {
        prepareLooper();
        final int testRepeatMode = SessionPlayer2.REPEAT_MODE_GROUP;
        mSession.getPlayer().setRepeatMode(testRepeatMode);
        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }

    @Test
    public void testSetRepeatModeByController() throws InterruptedException {
        final int testRepeatMode = SessionPlayer2.REPEAT_MODE_GROUP;
        mController2.setRepeatMode(testRepeatMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }
}
