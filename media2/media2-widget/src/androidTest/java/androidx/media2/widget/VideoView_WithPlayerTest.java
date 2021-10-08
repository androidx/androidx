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

import static androidx.media2.widget.AspectRatioMatcher.withAspectRatio;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.widget.test.R;
import androidx.test.filters.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link VideoView} with a {@link SessionPlayer} or a {@link MediaController}.
 */
@RunWith(Parameterized.class)
@LargeTest
public class VideoView_WithPlayerTest extends MediaWidgetTestBase {
    static final String TAG = "VideoView_WithPlayerTest";
    @Parameterized.Parameters(name = "PlayerType={0}")
    public static List<String> getPlayerTypes() {
        return Arrays.asList(PLAYER_TYPE_MEDIA_CONTROLLER, PLAYER_TYPE_MEDIA_PLAYER);
    }
    private String mPlayerType;
    private Activity mActivity;
    private VideoView mVideoView;
    private MediaItem mMediaItem;
    private SynchronousPixelCopy mPixelCopyHelper;

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<VideoViewTestActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(VideoViewTestActivity.class);

    public VideoView_WithPlayerTest(String playerType) {
        mPlayerType = playerType;
    }

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mMediaItem = createTestMediaItem();
        mPixelCopyHelper = new SynchronousPixelCopy();
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
        mPixelCopyHelper.release();
    }

    @Test
    public void playVideo() throws Throwable {
        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback.mPlayingLatch.getCount());
        assertEquals(SessionPlayer.PLAYER_STATE_PAUSED, playerWrapper.getPlayerState());

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering(true);
    }

    @Test
    public void playVideoWithMediaItemFromFileDescriptor() throws Throwable {
        AssetFileDescriptor afd = mContext.getResources()
                .openRawResourceFd(R.raw.testvideo_with_2_subtitle_tracks);
        final MediaItem item = new FileMediaItem.Builder(
                ParcelFileDescriptor.dup(afd.getFileDescriptor()))
                .setFileDescriptorOffset(afd.getStartOffset())
                .setFileDescriptorLength(afd.getLength())
                .build();
        afd.close();

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, item, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering(true);
    }

    @Test
    public void playVideoOnTextureView() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);
        if (setViewTypeMayCrash()) {
            return;
        }
        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
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
        checkVideoRendering(true);
    }

    @Test
    public void playVideoWithVisibilityChange() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);
        if (setViewTypeMayCrash()) {
            return;
        }

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setVisibility(View.GONE);
            }
        });
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering(false);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setVisibility(View.VISIBLE);
            }
        });
        // Note: Actual view type change is done when VideoView has a valid surface.
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_TEXTUREVIEW);
        checkVideoRendering(true);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_SURFACEVIEW);
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);
        checkVideoRendering(true);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setVisibility(View.GONE);
            }
        });
        // Although it is not flaky, since checkVideoRendering() waits a bit before actual
        // screen capturing, we might need to define a listener to ensure the player's surface
        // has been released.
        checkVideoRendering(false);
    }

    @Test
    public void setViewType() throws Throwable {
        if (setViewTypeMayCrash()) {
            return;
        }
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem, null);
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
        checkVideoRendering(true);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        checkVideoRendering(true);
    }

    // @UiThreadTest will be ignored by Parameterized test runner (b/30746303)
    @Test
    public void attachedMediaControlView_setPlayerOrController() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayerWrapper playerWrapper = createPlayerWrapper(new DefaultPlayerCallback(),
                        mMediaItem, null);

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
        });
    }

    @Test
    public void aspectRatioOfSurfaceView() throws Throwable {
        MediaItem testMediaItem = createTestMediaItem(getResourceUri(R.raw.test_file_scheme_video));
        VideoSize testVideoSize = new VideoSize(352, 288);
        CountDownLatch latch = new CountDownLatch(1);

        mActivityRule.runOnUiThread(() -> {
            int parentWidth = mVideoView.getWidth();
            int parentHeight = mVideoView.getHeight();

            View surfaceView = findVideoSurfaceView();
            assertNotNull("Couldn't find VideoSurfaceView", surfaceView);
            surfaceView.addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        if (left == 0 && top == 0 && right == parentWidth
                                && bottom == parentHeight) {
                            // Ignore layout changes to the default size
                            return;
                        }
                        latch.countDown();
                    });
        });

        DefaultPlayerCallback playerCallback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(playerCallback, testMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(playerCallback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(playerCallback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(instanceOf(VideoSurfaceView.class)).check(matches(
                withAspectRatio(testVideoSize.getWidth(), testVideoSize.getHeight())));

        // Unable to test the case for multiple media items with different aspect ratio due to the
        // flakiness of onVideoSizeChanged of MediaPlayer (b/144876689, b/144972397)
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

    private PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item, @Nullable List<MediaItem> playlist) {
        return createPlayerWrapperOfType(callback, item, playlist, mPlayerType);
    }

    private void checkVideoRendering(boolean expectRendering) throws InterruptedException {
        if (Build.VERSION.SDK_INT == 28) {
            // TODO: This if-block for API 28 should be removed. (b/137321781)
            return;
        }
        if (Build.DEVICE.startsWith("generic_") && Build.VERSION.SDK_INT == 26) {
            return;
        }
        if (Build.DEVICE.equals("fugu") && Build.VERSION.SDK_INT == 24) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 24) {
            final int bufferQueueToleranceMs = 200;
            final int elapsedTimeForSecondScreenshotMs = 400;

            // Tolerance until the video buffers are actually queued.
            Thread.sleep(bufferQueueToleranceMs);
            Bitmap beforeBitmap = getVideoScreenshot();
            Thread.sleep(elapsedTimeForSecondScreenshotMs);
            Bitmap afterBitmap = getVideoScreenshot();
            assertEquals(expectRendering, !afterBitmap.sameAs(beforeBitmap));
        }
    }

    private boolean setViewTypeMayCrash() {
        // TODO(b/143496920): Remove this method which is a guard to avoid crash.
        // Need to skip the tests, which call VV#setViewType(), on the emulator with API 26.
        if (Build.DEVICE.startsWith("generic_") && Build.VERSION.SDK_INT == 26) {
            return true;
        }
        return false;
    }

    private Bitmap getVideoScreenshot() {
        Bitmap bitmap = Bitmap.createBitmap(mVideoView.getWidth(),
                mVideoView.getHeight(), Bitmap.Config.RGB_565);
        if (mVideoView.getViewType() == mVideoView.VIEW_TYPE_SURFACEVIEW) {
            if (mVideoView.mSurfaceView.hasAvailableSurface()) {
                int copyResult = mPixelCopyHelper.request(mVideoView.mSurfaceView, bitmap);
                if (copyResult != PixelCopy.ERROR_SOURCE_NO_DATA) {
                    assertEquals("PixelCopy failed.", PixelCopy.SUCCESS, copyResult);
                }
            }
        } else {
            bitmap = mVideoView.mTextureView.getBitmap(bitmap);
        }
        return bitmap;
    }

    @UiThread
    private VideoSurfaceView findVideoSurfaceView() {
        for (int i = 0; i < mVideoView.getChildCount(); i++) {
            View child = mVideoView.getChildAt(i);
            if (child instanceof VideoSurfaceView) {
                return (VideoSurfaceView) child;
            }
        }
        return null;
    }

    private static class SynchronousPixelCopy {
        private Handler mHandler;
        private HandlerThread mHandlerThread;
        private int mStatus = PixelCopy.SUCCESS;

        SynchronousPixelCopy() {
            if (Build.VERSION.SDK_INT >= 24) {
                this.mHandlerThread = new HandlerThread("PixelCopyHelper");
                mHandlerThread.start();
                this.mHandler = new Handler(mHandlerThread.getLooper());
            }
        }

        public void release() {
            if (Build.VERSION.SDK_INT >= 24) {
                if (mHandlerThread.isAlive()) {
                    mHandlerThread.quitSafely();
                }
            }
        }

        public int request(SurfaceView source, Bitmap dest) {
            if (Build.VERSION.SDK_INT < 24) {
                return -1;
            }
            synchronized (this) {
                try {
                    PixelCopy.request(source, dest, new PixelCopy.OnPixelCopyFinishedListener() {
                        @Override
                        public void onPixelCopyFinished(int copyResult) {
                            synchronized (this) {
                                mStatus = copyResult;
                                this.notify();
                            }
                        }
                    }, mHandler);
                    return getResultLocked();
                } catch (Exception e) {
                    Log.e(TAG, "Exception occurred when copying a SurfaceView.", e);
                    return -1;
                }
            }
        }

        private int getResultLocked() {
            try {
                this.wait(1000);
            } catch (InterruptedException e) {
                /* PixelCopy request didn't complete within 1s */
                mStatus = PixelCopy.ERROR_TIMEOUT;
            }
            return mStatus;
        }
    }
}
