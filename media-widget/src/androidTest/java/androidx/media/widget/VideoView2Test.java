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

package androidx.media.widget;

import static android.content.Context.KEYGUARD_SERVICE;

import static junit.framework.TestCase.assertEquals;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.media.AudioAttributesCompat;
import androidx.media.BaseMediaPlayer;
import androidx.media.MediaController2;
import androidx.media.MediaItem2;
import androidx.media.MediaMetadata2;
import androidx.media.SessionCommandGroup2;
import androidx.media.SessionToken2;
import androidx.media.widget.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Test {@link VideoView2}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P) // TODO: KITKAT
@LargeTest
@RunWith(AndroidJUnit4.class)
public class VideoView2Test {
    /** Debug TAG. **/
    private static final String TAG = "VideoView2Test";
    /** The maximum time to wait for an operation. */
    private static final long   TIME_OUT = 15000L;
    /** The interval time to wait for completing an operation. */
    private static final long   OPERATION_INTERVAL  = 1500L;
    /** The duration of R.raw.testvideo. */
    private static final int    TEST_VIDEO_DURATION = 11047;
    /** The full name of R.raw.testvideo. */
    private static final String VIDEO_NAME   = "testvideo.3gp";
    /** delta for duration in case user uses different decoders on different
        hardware that report a duration that's different by a few milliseconds */
    private static final int DURATION_DELTA = 100;
    /** AudioAttributes to be used by this player */
    private static final AudioAttributesCompat AUDIO_ATTR = new AudioAttributesCompat.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();
    private Instrumentation mInstrumentation;
    private Activity mActivity;
    private KeyguardManager mKeyguardManager;
    private VideoView2 mVideoView;
    private MediaController2 mController;
    private String mVideoPath;
    private Context mContext;

    @Rule
    public ActivityTestRule<VideoView2TestActivity> mActivityRule =
            new ActivityTestRule<>(VideoView2TestActivity.class);

    @Before
    public void setup() throws Throwable {
        mContext = InstrumentationRegistry.getTargetContext();
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
        mKeyguardManager = (KeyguardManager)
                mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
        mActivity = mActivityRule.getActivity();
        mVideoView = (VideoView2) mActivity.findViewById(R.id.videoview);
        mVideoPath = prepareSampleVideo();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Keep screen on while testing.
                if (Build.VERSION.SDK_INT >= 27) {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mActivity.setTurnScreenOn(true);
                    mActivity.setShowWhenLocked(true);
                    mKeyguardManager.requestDismissKeyguard(mActivity, null);
                } else {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }
        });
        mInstrumentation.waitForIdleSync();
        final View.OnAttachStateChangeListener mockAttachListener =
                mock(View.OnAttachStateChangeListener.class);
        if (!mVideoView.isAttachedToWindow()) {
            mVideoView.addOnAttachStateChangeListener(mockAttachListener);
            verify(mockAttachListener, timeout(TIME_OUT)).onViewAttachedToWindow(same(mVideoView));
        }
    }

    @After
    public void tearDown() throws Throwable {
        /** call media controller's stop */
    }

    private boolean hasCodec() {
        return MediaUtils2.hasCodecsForResource(mActivity, R.raw.testvideo);
    }

    private String prepareSampleVideo() throws IOException {
        try (InputStream source = mActivity.getResources().openRawResource(R.raw.testvideo);
             OutputStream target = mActivity.openFileOutput(VIDEO_NAME, Context.MODE_PRIVATE)) {
            final byte[] buffer = new byte[1024];
            for (int len = source.read(buffer); len > 0; len = source.read(buffer)) {
                target.write(buffer, 0, len);
            }
        }

        return mActivity.getFileStreamPath(VIDEO_NAME).getAbsolutePath();
    }

    @UiThreadTest
    @Test
    public void testConstructor() {
        new VideoView2(mActivity);
        new VideoView2(mActivity, null);
        new VideoView2(mActivity, null, 0);
    }

    @Test
    public void testPlayVideo() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testPlayVideo(): codec is not supported");
            return;
        }

        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        final MediaController2.ControllerCallback mockControllerCallback =
                mock(MediaController2.ControllerCallback.class);
        final MediaController2.ControllerCallback callbackHelper =
                new MediaController2.ControllerCallback() {
            @Override
            public void onPlayerStateChanged(final MediaController2 controller, final int state) {
                mockControllerCallback.onPlayerStateChanged(controller, state);
            }

            @Override
            public void onConnected(@NonNull MediaController2 controller,
                    @NonNull SessionCommandGroup2 allowedCommands) {
                mockControllerCallback.onConnected(controller, allowedCommands);
            }

            @Override
            public void onPlaylistChanged(MediaController2 controller,
                    List<MediaItem2> list,
                    MediaMetadata2 metadata) {
                mockControllerCallback.onPlaylistChanged(controller, list, metadata);
            }
        };

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SessionToken2 token = mVideoView.getMediaSessionToken();
                mController = new MediaController2(
                        mContext, token,
                        MainHandlerExecutor.getExecutor(mContext), callbackHelper);
                mVideoView.setVideoPath(mVideoPath);
            }
        });

        verify(mockControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        verify(mockControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(BaseMediaPlayer.PLAYER_STATE_PAUSED));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mController.play();
            }
        });
        verify(mockControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(BaseMediaPlayer.PLAYER_STATE_PLAYING));
    }

    @Test
    public void testPlayVideoOnTextureView() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testPlayVideoOnTextureView(): codec is not supported");
            return;
        }
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }
        final VideoView2.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView2.OnViewTypeChangedListener.class);
        SessionToken2 token = mVideoView.getMediaSessionToken();

        final MediaController2.ControllerCallback mockControllerCallback =
                mock(MediaController2.ControllerCallback.class);
        final MediaController2.ControllerCallback callbackHelper =
                new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(
                            final MediaController2 controller, final int state) {
                        mockControllerCallback.onPlayerStateChanged(controller, state);
                    }
                };

        mController = new MediaController2(
                mVideoView.getContext(), token,
                MainHandlerExecutor.getExecutor(mVideoView.getContext()), callbackHelper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), mVideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(mVideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setVideoPath(mVideoPath);
            }
        });
        verify(mockViewTypeListener, timeout(TIME_OUT))
                .onViewTypeChanged(mVideoView, VideoView2.VIEW_TYPE_TEXTUREVIEW);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mController.play();
            }
        });
        verify(mockControllerCallback, timeout(TIME_OUT).atLeast(1)).onPlayerStateChanged(
                any(MediaController2.class), eq(BaseMediaPlayer.PLAYER_STATE_PLAYING));
        verify(mockControllerCallback, timeout(TIME_OUT).atLeast(1)).onPlayerStateChanged(
                any(MediaController2.class), eq(BaseMediaPlayer.PLAYER_STATE_PAUSED));
    }
}
