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

import static androidx.media.widget.MediaControlView2.COMMAND_HIDE_SUBTITLE;
import static androidx.media.widget.MediaControlView2.COMMAND_SHOW_SUBTITLE;
import static androidx.media.widget.MediaControlView2.EVENT_UPDATE_SUBTITLE_DESELECTED;
import static androidx.media.widget.MediaControlView2.EVENT_UPDATE_SUBTITLE_SELECTED;
import static androidx.media.widget.MediaControlView2.EVENT_UPDATE_TRACK_STATUS;
import static androidx.media.widget.MediaControlView2.KEY_SELECTED_SUBTITLE_INDEX;
import static androidx.media.widget.MediaControlView2.KEY_SUBTITLE_TRACK_COUNT;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.core.content.ContextCompat;
import androidx.media.widget.test.R;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaController2;
import androidx.media2.MediaController2.ControllerResult;
import androidx.media2.MediaItem2;
import androidx.media2.SessionCommand2;
import androidx.media2.SessionCommandGroup2;
import androidx.media2.SessionPlayer2;
import androidx.media2.UriMediaItem2;
import androidx.test.InstrumentationRegistry;
import androidx.test.annotation.UiThreadTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;

import java.util.concurrent.Executor;

/**
 * Test {@link VideoView2}.
 *
 * TODO: Lower minSdkVersion to Kitkat.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoView2Test {

    /** Debug TAG. **/
    private static final String TAG = "VideoView2Test";
    /** The maximum time to wait for an operation. */
    private static final long TIME_OUT = 1000L;

    private Context mContext;
    private Executor mMainHandlerExecutor;
    private Instrumentation mInstrumentation;

    private Activity mActivity;
    private VideoView2 mVideoView;
    private MediaItem2 mMediaItem;
    private MediaController2.ControllerCallback mControllerCallback;
    private MediaController2 mController;

    @Rule
    public ActivityTestRule<VideoView2TestActivity> mActivityRule =
            new ActivityTestRule<>(VideoView2TestActivity.class);

    @Before
    public void setup() throws Throwable {
        mContext = InstrumentationRegistry.getTargetContext();
        mMainHandlerExecutor = ContextCompat.getMainExecutor(mContext);
        mInstrumentation = InstrumentationRegistry.getInstrumentation();

        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mMediaItem = createTestMediaItem2();

        setKeepScreenOn();
        checkAttachedToWindow();

        mControllerCallback = mock(MediaController2.ControllerCallback.class);
        when(mControllerCallback.onCustomCommand(
                nullable(MediaController2.class),
                nullable(SessionCommand2.class),
                nullable(Bundle.class))).thenReturn(
                        new ControllerResult(ControllerResult.RESULT_CODE_SUCCESS, null));
        mController = new MediaController2(mVideoView.getContext(),
                mVideoView.getMediaSessionToken2(), mMainHandlerExecutor, mControllerCallback);
    }

    @After
    public void tearDown() throws Throwable {
        if (mController != null) {
            mController.close();
        }
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

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mMediaItem);
            }
        });
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PAUSED));

        mController.play();
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PLAYING));
    }

    @Test
    public void testSetMediaItem2() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testPlayVideo(): codec is not supported");
            return;
        }
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mMediaItem);
            }
        });
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PAUSED));
        verify(mControllerCallback, after(TIME_OUT).never()).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PLAYING));
        assertEquals(SessionPlayer2.PLAYER_STATE_PAUSED, mController.getPlayerState());
    }

    @Test
    public void testPlayVideoWithMediaItemFromFileDescriptor() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testPlayVideoWithMediaItemFromFileDescriptor(): "
                    + "codec is not supported");
            return;
        }

        AssetFileDescriptor afd = mContext.getResources()
                .openRawResourceFd(R.raw.testvideo_with_2_subtitle_tracks);
        final MediaItem2 item = new FileMediaItem2.Builder(
                afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength())
                .build();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(item);
            }
        });
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PAUSED));

        mController.play();
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PLAYING));
    }

    @Test
    public void testPlayVideoOnTextureView() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testPlayVideoOnTextureView(): codec is not supported");
            return;
        }

        final VideoView2.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView2.OnViewTypeChangedListener.class);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), mVideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(mVideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setMediaItem2(mMediaItem);
            }
        });
        verify(mockViewTypeListener, timeout(TIME_OUT))
                .onViewTypeChanged(mVideoView, VideoView2.VIEW_TYPE_TEXTUREVIEW);
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        verify(mControllerCallback, timeout(TIME_OUT).atLeast(1)).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PAUSED));

        mController.play();
        verify(mControllerCallback, timeout(TIME_OUT).atLeast(1)).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PLAYING));
    }

    @Test
    public void testSetViewType() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testPlayVideoOnTextureView(): codec is not supported");
            return;
        }

        final VideoView2.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView2.OnViewTypeChangedListener.class);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), mVideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(mVideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(mVideoView.VIEW_TYPE_SURFACEVIEW);
                mVideoView.setViewType(mVideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(mVideoView.VIEW_TYPE_SURFACEVIEW);
                mVideoView.setMediaItem2(mMediaItem);
            }
        });

        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        verify(mControllerCallback, timeout(TIME_OUT).atLeast(1)).onPlayerStateChanged(
                any(MediaController2.class), eq(SessionPlayer2.PLAYER_STATE_PAUSED));

        assertEquals(mVideoView.getViewType(), mVideoView.VIEW_TYPE_SURFACEVIEW);
    }

    @Test
    public void testSubtitleSelection() throws Throwable {
        if (!hasCodec()) {
            Log.i(TAG, "SKIPPING testSubtitleSelection(): codec is not supported");
            return;
        }
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mMediaItem);
            }
        });
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onConnected(
                any(MediaController2.class), any(SessionCommandGroup2.class));
        mController.play();

        // Verify the subtitle track count
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onCustomCommand(
                any(MediaController2.class),
                argThat(new CommandMatcher(EVENT_UPDATE_TRACK_STATUS)),
                argThat(new CommandArgumentMatcher(KEY_SUBTITLE_TRACK_COUNT, 2)));

        // Select the first subtitle track
        Bundle extra = new Bundle();
        extra.putInt(KEY_SELECTED_SUBTITLE_INDEX, 0);
        mController.sendCustomCommand(
                new SessionCommand2(COMMAND_SHOW_SUBTITLE, null), extra);
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onCustomCommand(
                any(MediaController2.class),
                argThat(new CommandMatcher(EVENT_UPDATE_SUBTITLE_SELECTED)),
                argThat(new CommandArgumentMatcher(KEY_SELECTED_SUBTITLE_INDEX, 0)));

        // Select the second subtitle track
        extra.putInt(KEY_SELECTED_SUBTITLE_INDEX, 1);
        mController.sendCustomCommand(
                new SessionCommand2(COMMAND_SHOW_SUBTITLE, null), extra);
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onCustomCommand(
                any(MediaController2.class),
                argThat(new CommandMatcher(EVENT_UPDATE_SUBTITLE_SELECTED)),
                argThat(new CommandArgumentMatcher(KEY_SELECTED_SUBTITLE_INDEX, 1)));

        // Deselect subtitle track
        mController.sendCustomCommand(
                new SessionCommand2(COMMAND_HIDE_SUBTITLE, null), null);
        verify(mControllerCallback, timeout(TIME_OUT).atLeastOnce()).onCustomCommand(
                any(MediaController2.class),
                argThat(new CommandMatcher(EVENT_UPDATE_SUBTITLE_DESELECTED)),
                nullable(Bundle.class));
    }

    class CommandMatcher implements ArgumentMatcher<SessionCommand2> {
        final String mExpectedCommand;

        CommandMatcher(String command) {
            mExpectedCommand = command;
        }

        @Override
        public boolean matches(SessionCommand2 command) {
            return mExpectedCommand.equals(command.getCustomCommand());
        }
    }

    class CommandArgumentMatcher implements ArgumentMatcher<Bundle> {
        final String mKey;
        final int mExpectedValue;

        CommandArgumentMatcher(String key, int expectedValue) {
            mKey = key;
            mExpectedValue = expectedValue;
        }

        @Override
        public boolean matches(Bundle argument) {
            return argument.getInt(mKey, -1) == mExpectedValue;
        }
    }

    private void setKeepScreenOn() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= 27) {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mActivity.setTurnScreenOn(true);
                    mActivity.setShowWhenLocked(true);
                    KeyguardManager keyguardManager = (KeyguardManager)
                            mInstrumentation.getTargetContext().getSystemService(KEYGUARD_SERVICE);
                    keyguardManager.requestDismissKeyguard(mActivity, null);
                } else {
                    mActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                }
            }
        });
        mInstrumentation.waitForIdleSync();
    }

    private void checkAttachedToWindow() {
        final View.OnAttachStateChangeListener mockAttachListener =
                mock(View.OnAttachStateChangeListener.class);
        if (!mVideoView.isAttachedToWindow()) {
            mVideoView.addOnAttachStateChangeListener(mockAttachListener);
            verify(mockAttachListener, timeout(TIME_OUT)).onViewAttachedToWindow(same(mVideoView));
        }
    }

    private boolean hasCodec() {
        return TestUtils.hasCodecsForResource(mActivity, R.raw.testvideo_with_2_subtitle_tracks);
    }

    private MediaItem2 createTestMediaItem2() {
        Uri testVideoUri = Uri.parse(
                "android.resource://" + mContext.getPackageName() + "/"
                        + R.raw.testvideo_with_2_subtitle_tracks);
        return new UriMediaItem2.Builder(mVideoView.getContext(), testVideoUri)
                .build();
    }
}
