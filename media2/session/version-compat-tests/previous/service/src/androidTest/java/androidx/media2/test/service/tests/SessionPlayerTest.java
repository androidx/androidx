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

package androidx.media2.test.service.tests;

import static androidx.media2.test.common.CommonConstants.CLIENT_PACKAGE_NAME;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;

import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaSession;
import androidx.media2.session.MediaSession.ControllerInfo;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.test.common.TestUtils;
import androidx.media2.test.service.MediaTestUtils;
import androidx.media2.test.service.MockPlayer;
import androidx.media2.test.service.RemoteMediaController;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether the methods of {@link SessionPlayer} are triggered by the
 * session/controller.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionPlayerTest extends MediaSessionTestBase {

    MediaSession mSession;
    MockPlayer mPlayer;
    RemoteMediaController mController;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(1);
        mSession = new MediaSession.Builder(mContext, mPlayer)
                .setSessionCallback(sHandlerExecutor, new MediaSession.SessionCallback() {
                    @Override
                    public SessionCommandGroup onConnect(MediaSession session,
                            MediaSession.ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }

                    @Override
                    public MediaItem onCreateMediaItem(MediaSession session,
                            ControllerInfo controller, String mediaId) {
                        return MediaTestUtils.createMediaItem(mediaId);
                    }
                }).build();

        // Create a default MediaController in client app.
        mController = createRemoteController(mSession.getToken());
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
        mController.play();
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
        mController.pause();
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
        mController.prepare();
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
        mController.seekTo(seekPosition);
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
        mController.setPlaybackSpeed(speed);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(speed, mPlayer.mPlaybackSpeed, 0.0f);
    }

    @Test
    public void testSetPlaylistBySession() {
        prepareLooper();
        final List<MediaItem> list = MediaTestUtils.createPlaylist(2);
        mSession.getPlayer().setPlaylist(list, null);
        assertTrue(mPlayer.mSetPlaylistCalled);
        assertSame(list, mPlayer.mPlaylist);
        assertNull(mPlayer.mMetadata);
    }

    @Test
    public void testSetPlaylistByController() throws InterruptedException {
        final List<String> list = MediaTestUtils.createMediaIds(2);
        mController.setPlaylist(list, null /* metadata */);
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
        mController.createAndSetDummyPlaylist(listSize, null /* metadata */);
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
        final MediaMetadata testMetadata = MediaTestUtils.createMetadata();
        mSession.getPlayer().updatePlaylistMetadata(testMetadata);
        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertSame(testMetadata, mPlayer.mMetadata);
    }

    @Test
    public void testUpdatePlaylistMetadataByController() throws InterruptedException {
        final MediaMetadata testMetadata = MediaTestUtils.createMetadata();
        mController.updatePlaylistMetadata(testMetadata);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mUpdatePlaylistMetadataCalled);
        assertNotNull(mPlayer.mMetadata);
        assertEquals(testMetadata.getMediaId(), mPlayer.mMetadata.getMediaId());
    }

    @Test
    public void testAddPlaylistItemBySession() {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.getPlayer().addPlaylistItem(testIndex, testMediaItem);
        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void testAddPlaylistItemByController() throws InterruptedException {
        final int testIndex = 12;
        final String testMediaId = "testAddPlaylistItemByController";

        mController.addPlaylistItem(testIndex, testMediaId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mAddPlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        // MediaController.addPlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaId, mPlayer.mItem.getMediaId());
    }

    @Test
    public void testRemovePlaylistItemBySession() {
        prepareLooper();
        final List<MediaItem> list = MediaTestUtils.createPlaylist(2);
        mSession.getPlayer().setPlaylist(list, null);
        mSession.getPlayer().removePlaylistItem(0);
        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertSame(0, mPlayer.mIndex);
    }

    @Test
    public void testRemovePlaylistItemByController() throws InterruptedException {
        mPlayer.mPlaylist = MediaTestUtils.createPlaylist(2);

        mController.removePlaylistItem(0);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mRemovePlaylistItemCalled);
        assertEquals(0, mPlayer.mIndex);
    }

    @Test
    public void testReplacePlaylistItemBySession() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.getPlayer().replacePlaylistItem(testIndex, testMediaItem);
        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        assertEquals(testIndex, mPlayer.mIndex);
        assertSame(testMediaItem, mPlayer.mItem);
    }

    @Test
    public void testReplacePlaylistItemByController() throws InterruptedException {
        final int testIndex = 12;
        final String testMediaId = "testReplacePlaylistItemByController";

        mController.replacePlaylistItem(testIndex, testMediaId);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mReplacePlaylistItemCalled);
        // MediaController.replacePlaylistItem does not ensure the equality of the items.
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
        mController.skipToPreviousItem();
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
        mController.skipToNextItem();
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mPlayer.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToPlaylistItemBySession() throws Exception {
        prepareLooper();
        final List<MediaItem> list = MediaTestUtils.createPlaylist(2);
        int targetIndex = 0;
        mSession.getPlayer().setPlaylist(list, null);
        mSession.getPlayer().skipToPlaylistItem(targetIndex);
        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertSame(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void testSkipToPlaylistItemByController() throws InterruptedException {
        mPlayer.mPlaylist = MediaTestUtils.createPlaylist(3);
        int targetIndex = 2;

        mController.skipToPlaylistItem(targetIndex);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSkipToPlaylistItemCalled);
        assertEquals(targetIndex, mPlayer.mIndex);
    }

    @Test
    public void testSetShuffleModeBySession() {
        prepareLooper();
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        mSession.getPlayer().setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    @Test
    public void testSetShuffleModeByController() throws InterruptedException {
        final int testShuffleMode = SessionPlayer.SHUFFLE_MODE_GROUP;
        mController.setShuffleMode(testShuffleMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mPlayer.mShuffleMode);
    }

    @Test
    public void testSetRepeatModeBySession() {
        prepareLooper();
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        mSession.getPlayer().setRepeatMode(testRepeatMode);
        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }

    @Test
    public void testSetRepeatModeByController() throws InterruptedException {
        final int testRepeatMode = SessionPlayer.REPEAT_MODE_GROUP;
        mController.setRepeatMode(testRepeatMode);
        assertTrue(mPlayer.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mPlayer.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mPlayer.mRepeatMode);
    }
}
