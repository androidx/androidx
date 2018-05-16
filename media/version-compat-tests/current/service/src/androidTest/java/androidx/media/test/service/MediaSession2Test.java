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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;
import android.os.Process;
import android.support.test.filters.SdkSuppress;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import androidx.media.BaseMediaPlayer;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests {@link MediaSession2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class MediaSession2Test extends MediaSession2TestBase {

    private MediaSession2 mSession;
    private MockPlayer mPlayer;
    private MockPlaylistAgent mMockAgent;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        mPlayer = new MockPlayer(0);
        mMockAgent = new MockPlaylistAgent();

        mSession = new MediaSession2.Builder(mContext)
                .setPlayer(mPlayer)
                .setPlaylistAgent(mMockAgent)
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
    }

    @After
    @Override
    public void cleanUp() throws Exception {
        super.cleanUp();
        mSession.close();
    }

    @Test
    public void testBuilder() {
        prepareLooper();
        MediaSession2.Builder builder = new MediaSession2.Builder(mContext);
        try {
            builder.setPlayer(null);
            fail("null player shouldn't be allowed");
        } catch (IllegalArgumentException e) {
            // expected. pass-through
        }
        try {
            builder.setId(null);
            fail("null id shouldn't be allowed");
        } catch (IllegalArgumentException e) {
            // expected. pass-through
        }
    }

    @Test
    public void testGetDuration() throws Exception {
        prepareLooper();
        final long testDuration = 9999;
        mPlayer.mDuration = testDuration;
        assertEquals(testDuration, mSession.getDuration());
    }

    @Test
    public void testGetPlaybackSpeed() throws Exception {
        prepareLooper();
        final float speed = 1.5f;
        mPlayer.setPlaybackSpeed(speed);
        assertEquals(speed, mSession.getPlaybackSpeed(), 0.0f);
    }

    @Test
    public void testGetPlayerState() {
        prepareLooper();
        final int state = BaseMediaPlayer.PLAYER_STATE_PLAYING;
        mPlayer.mLastPlayerState = state;
        assertEquals(state, mSession.getPlayerState());
    }

    @Test
    public void testGetBufferingState() {
        prepareLooper();
        final int bufferingState = BaseMediaPlayer.BUFFERING_STATE_BUFFERING_AND_PLAYABLE;
        mPlayer.mLastBufferingState = bufferingState;
        assertEquals(bufferingState, mSession.getBufferingState());
    }

    @Test
    public void testGetPosition() {
        prepareLooper();
        final long position = 150000;
        mPlayer.mCurrentPosition = position;
        assertEquals(position, mSession.getCurrentPosition());
    }

    @Test
    public void testGetBufferedPosition() {
        prepareLooper();
        final long bufferedPosition = 900000;
        mPlayer.mBufferedPosition = bufferedPosition;
        assertEquals(bufferedPosition, mSession.getBufferedPosition());
    }

    @Test
    public void testGetCurrentMediaItem() {
        prepareLooper();
        MediaItem2 item = MediaTestUtils.createMediaItemWithMetadata();
        mMockAgent.mCurrentMediaItem = item;
        assertEquals(item, mSession.getCurrentMediaItem());
    }

    @Test
    public void testGetPlaylist() {
        prepareLooper();
        final List<MediaItem2> list = MediaTestUtils.createPlaylist(2);
        mMockAgent.mPlaylist = list;
        assertEquals(list, mSession.getPlaylist());
    }

    @Test
    public void testGetPlaylistMetadata() {
        prepareLooper();
        final MediaMetadata2 testMetadata = MediaTestUtils.createMetadata();
        mMockAgent.mMetadata = testMetadata;
        assertEquals(testMetadata, mSession.getPlaylistMetadata());
    }

    @Test
    public void testGetShuffleMode() throws InterruptedException {
        prepareLooper();
        final int testShuffleMode = MediaPlaylistAgent.SHUFFLE_MODE_GROUP;
        mMockAgent.setShuffleMode(testShuffleMode);
        assertEquals(testShuffleMode, mSession.getShuffleMode());
    }

    @Test
    public void testGetRepeatMode() throws InterruptedException {
        prepareLooper();
        final int testRepeatMode = MediaPlaylistAgent.REPEAT_MODE_GROUP;
        mMockAgent.setRepeatMode(testRepeatMode);
        assertEquals(testRepeatMode, mSession.getRepeatMode());
    }

    @Test
    public void testUpdatePlayer() throws Exception {
        prepareLooper();
        final int targetState = BaseMediaPlayer.PLAYER_STATE_PLAYING;
        final CountDownLatch latch = new CountDownLatch(1);
        sHandler.postAndSync(new Runnable() {
            @Override
            public void run() {
                mSession.close();
                mSession = new MediaSession2.Builder(mContext).setPlayer(mPlayer)
                        .setSessionCallback(sHandlerExecutor, new MediaSession2.SessionCallback() {
                            @Override
                            public void onPlayerStateChanged(MediaSession2 session,
                                    BaseMediaPlayer player, int state) {
                                assertEquals(targetState, state);
                                latch.countDown();
                            }
                        }).build();
            }
        });

        MockPlayer player = new MockPlayer(0);

        // Test if setPlayer doesn't crash with various situations.
        mSession.updatePlayer(mPlayer, null, null);
        assertEquals(mPlayer, mSession.getPlayer());
        MediaPlaylistAgent agent = mSession.getPlaylistAgent();
        assertNotNull(agent);

        mSession.updatePlayer(player, null, null);
        assertEquals(player, mSession.getPlayer());
        assertNotNull(mSession.getPlaylistAgent());
        assertNotEquals(agent, mSession.getPlaylistAgent());

        player.notifyPlayerStateChanged(BaseMediaPlayer.PLAYER_STATE_PLAYING);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }
}
