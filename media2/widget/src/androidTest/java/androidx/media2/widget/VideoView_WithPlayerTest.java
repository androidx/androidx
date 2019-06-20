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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.os.ParcelFileDescriptor;

import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.player.MediaPlayer;
import androidx.media2.widget.test.R;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test {@link VideoView} with a {@link SessionPlayer}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoView_WithPlayerTest extends MediaWidgetTestBase {
    private Activity mActivity;
    private VideoView mVideoView;
    private MediaItem mMediaItem;
    private SessionPlayer.PlayerCallback mPlayerCallback;
    private SessionPlayer mPlayer;

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

        mPlayerCallback = mock(SessionPlayer.PlayerCallback.class);
        mPlayer = new MediaPlayer(mContext);
        mPlayer.registerPlayerCallback(mMainHandlerExecutor, mPlayerCallback);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setPlayer(mPlayer);
            }
        });
    }

    @After
    public void tearDown() throws Throwable {
        mActivityRule.finishActivity();
        mPlayer.close();
    }

    @Test
    public void testPlayVideo() throws Throwable {
        waitToPrepare(mMediaItem);
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onCurrentMediaItemChanged(
                any(SessionPlayer.class), any(MediaItem.class));
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PAUSED));
        verify(mPlayerCallback, after(WAIT_TIME_MS).never()).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PLAYING));
        assertEquals(SessionPlayer.PLAYER_STATE_PAUSED, mPlayer.getPlayerState());

        mPlayer.play();
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PLAYING));
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

        waitToPrepare(item);
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onCurrentMediaItemChanged(
                any(SessionPlayer.class), eq(item));
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PAUSED));

        mPlayer.play();
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PLAYING));
    }

    @Test
    public void testPlayVideoOnTextureView() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        waitToPrepare(mMediaItem);
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onCurrentMediaItemChanged(
                any(SessionPlayer.class), any(MediaItem.class));
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeast(1)).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PAUSED));

        mPlayer.play();
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeast(1)).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PLAYING));
    }

    @Test
    public void testSetViewType() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

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

        waitToPrepare(mMediaItem);
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onCurrentMediaItemChanged(
                any(SessionPlayer.class), any(MediaItem.class));
        // WAIT_TIME_MS multiplied by the number of operations.
        verify(mPlayerCallback, timeout(WAIT_TIME_MS * 5).atLeast(1)).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PAUSED));
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mPlayer.play();
        verify(mPlayerCallback, timeout(WAIT_TIME_MS).atLeastOnce()).onPlayerStateChanged(
                any(SessionPlayer.class), eq(SessionPlayer.PLAYER_STATE_PLAYING));

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
    }

    private void waitToPrepare(MediaItem item) throws Exception {
        mPlayer.setMediaItem(item);
        mPlayer.prepare().get();
    }
}
