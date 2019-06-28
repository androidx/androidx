/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.media2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.widget.test.R;
import androidx.test.annotation.UiThreadTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * Base class for testing {@link VideoView} with a {@link SessionPlayer} or
 * {@link MediaController}.
 */
public abstract class VideoView_WithSthTestBase extends MediaWidgetTestBase {
    private Activity mActivity;
    private VideoView mVideoView;
    private MediaItem mMediaItem;

    @Rule
    public ActivityTestRule<VideoViewTestActivity> mActivityRule =
            new ActivityTestRule<>(VideoViewTestActivity.class);

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mMediaItem = createTestMediaItem();

        setKeepScreenOn(mActivityRule);
        checkAttachedToWindow(mVideoView);
    }

    @After
    public void tearDown() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeAll();
            }
        });
    }

    @Test
    public void testPlayVideo() throws Throwable {
        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback.mPlayingLatch.getCount());
        assertEquals(SessionPlayer.PLAYER_STATE_PAUSED, playerWrapper.getPlayerState());

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayVideoWithMediaItemFromFileDescriptor() throws Throwable {
        AssetFileDescriptor afd = mContext.getResources()
                .openRawResourceFd(R.raw.testvideo_with_2_subtitle_tracks);
        final MediaItem item = new FileMediaItem.Builder(
                ParcelFileDescriptor.dup(afd.getFileDescriptor()))
                .setFileDescriptorOffset(afd.getStartOffset())
                .setFileDescriptorLength(afd.getLength())
                .build();
        afd.close();

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, item);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayVideoOnTextureView() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetViewType() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
            }
        });

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        // WAIT_TIME_MS multiplied by the number of operations.
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS * 5, TimeUnit.MILLISECONDS));
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
    }

    @UiThreadTest
    @Test
    public void testAttachedMediaControlView_setPlayerOrController() {
        PlayerWrapper playerWrapper = createPlayerWrapper(new DefaultPlayerCallback(), mMediaItem);

        MediaControlView defaultMediaControlView = mVideoView.getMediaControlView();
        assertNotNull(defaultMediaControlView);
        try {
            if (playerWrapper.mPlayer != null) {
                defaultMediaControlView.setPlayer(playerWrapper.mPlayer);
            } else if (playerWrapper.mController != null) {
                defaultMediaControlView.setMediaController(playerWrapper.mController);
            } else {
                fail("playerWrapper doesn't have neither mPlayer or mController");
            }
            fail("setPlayer or setMediaController should not be allowed "
                    + "for MediaControlView attached to VideoView");
        } catch (IllegalStateException ex) {
            // expected
        }

        MediaControlView newMediaControlView = new MediaControlView(mContext);
        mVideoView.setMediaControlView(newMediaControlView, -1);
        try {
            if (playerWrapper.mPlayer != null) {
                newMediaControlView.setPlayer(playerWrapper.mPlayer);
            } else if (playerWrapper.mController != null) {
                newMediaControlView.setMediaController(playerWrapper.mController);
            } else {
                fail("playerWrapper doesn't have neither mPlayer or mController");
            }
            fail("setPlayer or setMediaController should not be allowed "
                    + "for MediaControlView attached to VideoView");
        } catch (IllegalStateException ex) {
            // expected
        }
    }

    private void setPlayerWrapper(final PlayerWrapper playerWrapper) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (playerWrapper.mPlayer != null) {
                    mVideoView.setPlayer(playerWrapper.mPlayer);
                } else if (playerWrapper.mController != null) {
                    mVideoView.setMediaController(playerWrapper.mController);
                }
            }
        });
    }

    abstract PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item);
}
