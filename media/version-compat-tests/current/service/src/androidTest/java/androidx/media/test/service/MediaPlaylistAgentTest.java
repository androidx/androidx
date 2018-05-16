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

import static androidx.media.test.lib.CommonConstants.KEY_ITEM_INDEX;
import static androidx.media.test.lib.CommonConstants.KEY_MEDIA_ITEM;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST;
import static androidx.media.test.lib.CommonConstants.KEY_PLAYLIST_METADATA;
import static androidx.media.test.lib.CommonConstants.KEY_REPEAT_MODE;
import static androidx.media.test.lib.CommonConstants.KEY_SHUFFLE_MODE;
import static androidx.media.test.lib.MediaController2Constants.ADD_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.REMOVE_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.REPLACE_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.SET_PLAYLIST;
import static androidx.media.test.lib.MediaController2Constants.SET_REPEAT_MODE;
import static androidx.media.test.lib.MediaController2Constants.SET_SHUFFLE_MODE;
import static androidx.media.test.lib.MediaController2Constants.SKIP_TO_NEXT_ITEM;
import static androidx.media.test.lib.MediaController2Constants.SKIP_TO_PLAYLIST_ITEM;
import static androidx.media.test.lib.MediaController2Constants.SKIP_TO_PREVIOUS_ITEM;
import static androidx.media.test.lib.MediaController2Constants.UPDATE_PLAYLIST_METADATA;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.os.Bundle;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.MediaPlaylistAgent;
import androidx.media.MediaSession2;
import androidx.media.SessionCommandGroup2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tests whether the methods of {@link MediaPlaylistAgent} are triggered by the
 * session/controller.
 */
// TODO(jaewan): Fix flaky failure -- see MediaController2Impl.getController()
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
@FlakyTest
public class MediaPlaylistAgentTest extends MediaSession2TestBase {

    MediaSession2 mSession;
    MockPlaylistAgent mMockAgent;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mMockAgent = new MockPlaylistAgent();
        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(new MockPlayer(1))
                .setPlaylistAgent(mMockAgent)
                .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                    @Override
                    public SessionCommandGroup2 onConnect(MediaSession2 session,
                            MediaSession2.ControllerInfo controller) {
                        if (CLIENT_PACKAGE_NAME.equals(controller.getPackageName())) {
                            return super.onConnect(session, controller);
                        }
                        return null;
                    }
                }).build();
        TestServiceRegistry.getInstance().setHandler(sHandler);
        // Create a default MediaController2 in client app.
        mTestHelper.createMediaController2(mSession.getToken());
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        if (mSession != null) {
            mSession.close();
        }
        TestServiceRegistry.getInstance().cleanUp();
    }

    @Test
    public void testSetPlaylistBySession() {
        prepareLooper();
        final List<MediaItem2> list = MediaTestUtils.createPlaylist(2);
        mSession.setPlaylist(list, null);
        assertTrue(mMockAgent.mSetPlaylistCalled);
        assertSame(list, mMockAgent.mPlaylist);
        assertNull(mMockAgent.mMetadata);
    }

    @Test
    public void testSetPlaylistByController() throws InterruptedException {
        final List<MediaItem2> list = MediaTestUtils.createPlaylist(2);
        Bundle args = new Bundle();
        args.putParcelableArrayList(
                KEY_PLAYLIST, MediaTestUtils.playlistToParcelableArrayList(list));
        mTestHelper.callMediaController2Method(SET_PLAYLIST, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetPlaylistCalled);
        assertNull(mMockAgent.mMetadata);

        assertNotNull(mMockAgent.mPlaylist);
        assertEquals(list.size(), mMockAgent.mPlaylist.size());
        for (int i = 0; i < list.size(); i++) {
            // MediaController2.setPlaylist does not ensure the equality of the items.
            assertEquals(list.get(i).getMediaId(), mMockAgent.mPlaylist.get(i).getMediaId());
        }
    }

    @Test
    public void testUpdatePlaylistMetadataBySession() {
        prepareLooper();
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();
        mSession.updatePlaylistMetadata(testMetadata);
        assertTrue(mMockAgent.mUpdatePlaylistMetadataCalled);
        assertSame(testMetadata, mMockAgent.mMetadata);
    }

    @Test
    public void testUpdatePlaylistMetadataByController() throws InterruptedException {
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();

        Bundle args = new Bundle();
        args.putBundle(KEY_PLAYLIST_METADATA, testMetadata.toBundle());
        mTestHelper.callMediaController2Method(UPDATE_PLAYLIST_METADATA, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mUpdatePlaylistMetadataCalled);
        assertNotNull(mMockAgent.mMetadata);
        assertEquals(testMetadata.getMediaId(), mMockAgent.mMetadata.getMediaId());
    }

    @Test
    public void testAddPlaylistItemBySession() {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.addPlaylistItem(testIndex, testMediaItem);
        assertTrue(mMockAgent.mAddPlaylistItemCalled);
        assertEquals(testIndex, mMockAgent.mIndex);
        assertSame(testMediaItem, mMockAgent.mItem);
    }

    @Test
    public void testAddPlaylistItemByController() throws InterruptedException {
        final int testIndex = 12;
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();

        Bundle args = new Bundle();
        args.putInt(KEY_ITEM_INDEX, testIndex);
        args.putBundle(KEY_MEDIA_ITEM, testMediaItem.toBundle());
        mTestHelper.callMediaController2Method(ADD_PLAYLIST_ITEM, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mAddPlaylistItemCalled);
        assertEquals(testIndex, mMockAgent.mIndex);
        // MediaController2.addPlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaItem.getMediaId(), mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testRemovePlaylistItemBySession() {
        prepareLooper();
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.removePlaylistItem(testMediaItem);
        assertTrue(mMockAgent.mRemovePlaylistItemCalled);
        assertSame(testMediaItem, mMockAgent.mItem);
    }

    @Test
    public void testRemovePlaylistItemByController() throws InterruptedException {
        mMockAgent.mPlaylist = MediaTestUtils.createPlaylist(2);
        MediaItem2 targetItem = mMockAgent.mPlaylist.get(0);

        Bundle args = new Bundle();
        args.putBundle(KEY_MEDIA_ITEM, targetItem.toBundle());
        mTestHelper.callMediaController2Method(REMOVE_PLAYLIST_ITEM, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mRemovePlaylistItemCalled);
        assertEquals(targetItem, mMockAgent.mItem);
    }

    @Test
    public void testReplacePlaylistItemBySession() throws InterruptedException {
        prepareLooper();
        final int testIndex = 12;
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.replacePlaylistItem(testIndex, testMediaItem);
        assertTrue(mMockAgent.mReplacePlaylistItemCalled);
        assertEquals(testIndex, mMockAgent.mIndex);
        assertSame(testMediaItem, mMockAgent.mItem);
    }

    @Test
    public void testReplacePlaylistItemByController() throws InterruptedException {
        final int testIndex = 12;
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();

        Bundle args = new Bundle();
        args.putInt(KEY_ITEM_INDEX, testIndex);
        args.putBundle(KEY_MEDIA_ITEM, testMediaItem.toBundle());
        mTestHelper.callMediaController2Method(REPLACE_PLAYLIST_ITEM, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mReplacePlaylistItemCalled);
        // MediaController2.replacePlaylistItem does not ensure the equality of the items.
        assertEquals(testMediaItem.getMediaId(), mMockAgent.mItem.getMediaId());
    }

    @Test
    public void testSkipToPreviousItemBySession() {
        prepareLooper();
        mSession.skipToPreviousItem();
        assertTrue(mMockAgent.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToPreviousItemByController() throws InterruptedException {
        mTestHelper.callMediaController2Method(SKIP_TO_PREVIOUS_ITEM, null);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToPreviousItemCalled);
    }

    @Test
    public void testSkipToNextItemBySession() throws Exception {
        prepareLooper();
        mSession.skipToNextItem();
        assertTrue(mMockAgent.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToNextItemByController() throws InterruptedException {
        mTestHelper.callMediaController2Method(SKIP_TO_NEXT_ITEM, null);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));
        assertTrue(mMockAgent.mSkipToNextItemCalled);
    }

    @Test
    public void testSkipToPlaylistItemBySession() throws Exception {
        prepareLooper();
        final MediaItem2 testMediaItem = MediaTestUtils.createMediaItemWithMetadata();
        mSession.skipToPlaylistItem(testMediaItem);
        assertTrue(mMockAgent.mSkipToPlaylistItemCalled);
        assertSame(testMediaItem, mMockAgent.mItem);
    }

    @Test
    public void testSkipToPlaylistItemByController() throws InterruptedException {
        MediaItem2 targetItem = MediaTestUtils.createMediaItemWithMetadata();

        Bundle args = new Bundle();
        args.putBundle(KEY_MEDIA_ITEM, targetItem.toBundle());
        mTestHelper.callMediaController2Method(SKIP_TO_PLAYLIST_ITEM, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSkipToPlaylistItemCalled);
        assertEquals(targetItem, mMockAgent.mItem);
    }

    @Test
    public void testSetShuffleModeBySession() {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        mSession.setShuffleMode(testShuffleMode);
        assertTrue(mMockAgent.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mMockAgent.mShuffleMode);
    }

    @Test
    public void testSetShuffleModeByController() throws InterruptedException {
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;

        Bundle args = new Bundle();
        args.putInt(KEY_SHUFFLE_MODE, testShuffleMode);
        mTestHelper.callMediaController2Method(SET_SHUFFLE_MODE, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetShuffleModeCalled);
        assertEquals(testShuffleMode, mMockAgent.mShuffleMode);
    }

    @Test
    public void testSetRepeatModeBySession() {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        mSession.setRepeatMode(testRepeatMode);
        assertTrue(mMockAgent.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mMockAgent.mRepeatMode);
    }

    @Test
    public void testSetRepeatModeByController() throws InterruptedException {
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;

        Bundle args = new Bundle();
        args.putInt(KEY_REPEAT_MODE, testRepeatMode);
        mTestHelper.callMediaController2Method(SET_REPEAT_MODE, args);
        assertTrue(mMockAgent.mCountDownLatch.await(TIMEOUT_MS, TimeUnit.MILLISECONDS));

        assertTrue(mMockAgent.mSetRepeatModeCalled);
        assertEquals(testRepeatMode, mMockAgent.mRepeatMode);
    }
}
