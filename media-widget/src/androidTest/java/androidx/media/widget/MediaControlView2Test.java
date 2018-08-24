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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.media.widget.test.R;
import androidx.media2.DataSourceDesc2;
import androidx.media2.MediaController2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaPlayerConnector;
import androidx.media2.UriDataSourceDesc2;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link MediaControlView2}.
 *
 * TODO: Lower minSdkVersion to Kitkat.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MediaControlView2Test {
    private static final String TAG = "MediaControlView2Test";
    // Expected success time
    private static final int WAIT_TIME_MS = 1000;
    private static final int HTTPS_WAIT_TIME_MS = 5000;
    private static final long FFWD_MS = 30000L;
    private static final long REW_MS = 10000L;

    private Context mContext;
    private Executor mMainHandlerExecutor;
    private Instrumentation mInstrumentation;

    private Activity mActivity;
    private VideoView2 mVideoView;
    private Uri mFileSchemeUri;
    private Uri mHttpsSchemeUri;
    private Uri mHttpSchemeUri;
    private MediaItem2 mFileSchemeMediaItem;
    private MediaItem2 mHttpsSchemeMediaItem;
    private MediaItem2 mHttpSchemeMediaItem;
    private List<MediaController2> mControllers = new ArrayList<>();

    @Rule
    public ActivityTestRule<MediaControlView2TestActivity> mActivityRule =
            new ActivityTestRule<>(MediaControlView2TestActivity.class);

    @Before
    public void setup() throws Throwable {
        mContext = InstrumentationRegistry.getTargetContext();
        mMainHandlerExecutor = MainHandlerExecutor.getExecutor(mContext);
        mInstrumentation = InstrumentationRegistry.getInstrumentation();

        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mFileSchemeUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_file_scheme_video);
        mHttpsSchemeUri = Uri.parse(mContext.getResources().getString(
                R.string.test_https_scheme_video));
        mHttpSchemeUri = Uri.parse(mContext.getResources().getString(
                R.string.test_http_scheme_video));
        mFileSchemeMediaItem = createTestMediaItem2(mFileSchemeUri);
        mHttpsSchemeMediaItem = createTestMediaItem2(mHttpsSchemeUri);
        mHttpSchemeMediaItem = createTestMediaItem2(mHttpSchemeUri);

        setKeepScreenOn();
        checkAttachedToWindow();
    }

    @After
    public void tearDown() throws Throwable {
        for (int i = 0; i < mControllers.size(); i++) {
            mControllers.get(i).close();
        }
    }

    @UiThreadTest
    @Test
    public void testConstructor() {
        new MediaControlView2(mActivity);
        new MediaControlView2(mActivity, null);
        new MediaControlView2(mActivity, null, 0);
    }

    @Test
    public void testPlayPauseButtonClick() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mFileSchemeUri)) {
            Log.i(TAG, "SKIPPING testPlayPauseButtonClick(): codec is not supported");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(2);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        switch ((int) latch.getCount()) {
                            case 2:
                                assertEquals(state, MediaPlayerConnector.PLAYER_STATE_PAUSED);
                                break;
                            case 1:
                                assertEquals(state, MediaPlayerConnector.PLAYER_STATE_PLAYING);
                        }
                        latch.countDown();
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mFileSchemeMediaItem);
            }
        });
        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.pause)).perform(click());
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFfwdButtonClick() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mFileSchemeUri)) {
            Log.i(TAG, "SKIPPING testFfwdButtonClick(): codec is not supported");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onSeekCompleted(@NonNull MediaController2 controller,
                            long position) {
                        if (position >= FFWD_MS) {
                            latch.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mFileSchemeMediaItem);
            }
        });
        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.ffwd)).perform(click());
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRewButtonClick() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mFileSchemeUri)) {
            Log.i(TAG, "SKIPPING testRewButtonClick(): codec is not supported");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(3);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        if (state == MediaPlayerConnector.PLAYER_STATE_PAUSED) {
                            controller.seekTo(FFWD_MS);
                            latch.countDown();
                        }
                    }
                    @Override
                    public void onSeekCompleted(@NonNull MediaController2 controller,
                            long position) {
                        switch ((int) latch.getCount()) {
                            case 2:
                                if (position == FFWD_MS) {
                                    latch.countDown();
                                }
                                break;
                            case 1:
                                if (position == FFWD_MS - REW_MS) {
                                    latch.countDown();
                                }
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mFileSchemeMediaItem);
            }
        });
        assertFalse(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.rew)).perform(click());
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayHttpsSchemeVideo() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mHttpsSchemeUri)) {
            Log.i(TAG, "SKIPPING testPlayHttpsSchemeVideo(): codec is not supported");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(2);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        switch ((int) latch.getCount()) {
                            case 2:
                                assertEquals(state, MediaPlayerConnector.PLAYER_STATE_PAUSED);
                                break;
                            case 1:
                                assertEquals(state, MediaPlayerConnector.PLAYER_STATE_PLAYING);
                        }
                        latch.countDown();
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mHttpsSchemeMediaItem);
            }
        });
        assertFalse(latch.await(WAIT_TIME_MS * 5, TimeUnit.MILLISECONDS));
        onView(withId(R.id.pause)).perform(click());
        assertTrue(latch.await(WAIT_TIME_MS * 5, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayHttpSchemeVideo() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mHttpSchemeUri)) {
            Log.i(TAG, "SKIPPING testPlayHttpSchemeVideo(): codec is not supported");
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        latch.countDown();
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mHttpSchemeMediaItem);
            }
        });
        assertFalse(latch.await(HTTPS_WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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

    private void checkAttachedToWindow() throws Exception {
        if (!mVideoView.isAttachedToWindow()) {
            final CountDownLatch latch = new CountDownLatch(1);
            View.OnAttachStateChangeListener listener = new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    latch.countDown();
                }
                @Override
                public void onViewDetachedFromWindow(View v) {
                }
            };
            mVideoView.addOnAttachStateChangeListener(listener);
            assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        }
    }

    private MediaItem2 createTestMediaItem2(Uri uri) {
        DataSourceDesc2 dsd = new UriDataSourceDesc2.Builder(mVideoView.getContext(), uri).build();
        return new MediaItem2.Builder(MediaItem2.FLAG_PLAYABLE).setDataSourceDesc(dsd).build();
    }

    private boolean hasCodec(Uri uri) {
        return TestUtils.hasCodecsForUri(mActivity, uri);
    }

    private MediaController2 createController(MediaController2.ControllerCallback callback) {
        MediaController2 controller = new MediaController2(mVideoView.getContext(),
                mVideoView.getMediaSessionToken2(), mMainHandlerExecutor, callback);
        mControllers.add(controller);
        return controller;
    }
}
