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
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.media.widget.test.R;
import androidx.media2.FileMediaItem2;
import androidx.media2.MediaController2;
import androidx.media2.MediaItem2;
import androidx.media2.MediaMetadata2;
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
        mMainHandlerExecutor = ContextCompat.getMainExecutor(mContext);
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

        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForPlayingState = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        if (state == SessionPlayer2.PLAYER_STATE_PAUSED) {
                            latchForPausedState.countDown();
                        } else if (state == SessionPlayer2.PLAYER_STATE_PLAYING) {
                            latchForPlayingState.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mFileSchemeMediaItem);
            }
        });
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.pause)).perform(click());
        assertTrue(latchForPlayingState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testFfwdButtonClick() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mFileSchemeUri)) {
            Log.i(TAG, "SKIPPING testFfwdButtonClick(): codec is not supported");
            return;
        }

        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForFfwd = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onSeekCompleted(@NonNull MediaController2 controller,
                            long position) {
                        if (position >= FFWD_MS) {
                            latchForFfwd.countDown();
                        }
                    }

                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        if (state == SessionPlayer2.PLAYER_STATE_PAUSED) {
                            latchForPausedState.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mFileSchemeMediaItem);
            }
        });
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.ffwd)).perform(click());
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRewButtonClick() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mFileSchemeUri)) {
            Log.i(TAG, "SKIPPING testRewButtonClick(): codec is not supported");
            return;
        }

        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForRew = new CountDownLatch(2);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        if (state == SessionPlayer2.PLAYER_STATE_PAUSED) {
                            controller.seekTo(FFWD_MS);
                            latchForPausedState.countDown();
                        }
                    }
                    @Override
                    public void onSeekCompleted(@NonNull MediaController2 controller,
                            long position) {
                        switch ((int) latchForRew.getCount()) {
                            case 2:
                                if (position == FFWD_MS) {
                                    latchForRew.countDown();
                                }
                                break;
                            case 1:
                                if (position == FFWD_MS - REW_MS) {
                                    latchForRew.countDown();
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
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.rew)).perform(click());
        assertTrue(latchForRew.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testPlayHttpsSchemeVideo() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mHttpsSchemeUri)) {
            Log.i(TAG, "SKIPPING testPlayHttpsSchemeVideo(): codec is not supported");
            return;
        }

        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForPlayingState = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlayerStateChanged(@NonNull MediaController2 controller,
                            int state) {
                        if (state == SessionPlayer2.PLAYER_STATE_PAUSED) {
                            latchForPausedState.countDown();
                        } else if (state == SessionPlayer2.PLAYER_STATE_PLAYING) {
                            latchForPlayingState.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(mHttpsSchemeMediaItem);
            }
        });
        assertTrue(latchForPausedState.await(HTTPS_WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.pause)).perform(click());
        assertTrue(latchForPlayingState.await(HTTPS_WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetMetadata() throws Throwable {
        // Don't run the test if the codec isn't supported.
        if (!hasCodec(mFileSchemeUri)) {
            Log.i(TAG, "SKIPPING testGetMetadata(): codec is not supported");
            return;
        }

        final long duration = 49056L;
        final String title = "BigBuckBunny";
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlaylistChanged(@NonNull MediaController2 controller,
                            @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
                        MediaMetadata2 itemMetadata = list.get(0).getMetadata();
                        if (itemMetadata != null) {
                            if (itemMetadata.containsKey(MediaMetadata2.METADATA_KEY_TITLE)) {
                                assertEquals(title, itemMetadata.getString(
                                        MediaMetadata2.METADATA_KEY_TITLE));
                            }
                            if (itemMetadata.containsKey(MediaMetadata2.METADATA_KEY_DURATION)) {
                                assertEquals(duration, itemMetadata.getLong(
                                        MediaMetadata2.METADATA_KEY_DURATION));
                            }
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
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testGetMetadataFromMusic() throws Throwable {
        Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_music);
        AssetFileDescriptor afd = mContext.getResources().openRawResourceFd(R.raw.test_music);

        // Don't run the test if the codec isn't supported.
        if (!hasCodec(uri) || !hasCodec(afd)) {
            Log.i(TAG, "SKIPPING testGetMetadataFromMusic(): codec is not supported");
            return;
        }

        final long duration = 4206L;
        final String title = "Chimey Phone";
        final String artist = "Android";
        final MediaItem2 uriMediaItem = createTestMediaItem2(uri);
        final MediaItem2 fileMediaItem = new FileMediaItem2.Builder(afd.getFileDescriptor(),
                afd.getStartOffset(), afd.getLength()).build();
        final CountDownLatch latchForUri = new CountDownLatch(1);
        final CountDownLatch latchForFile = new CountDownLatch(1);
        final MediaController2 controller =
                createController(new MediaController2.ControllerCallback() {
                    @Override
                    public void onPlaylistChanged(@NonNull MediaController2 controller,
                            @NonNull List<MediaItem2> list, @Nullable MediaMetadata2 metadata) {
                        MediaMetadata2 itemMetadata = list.get(0).getMetadata();
                        if (itemMetadata != null) {
                            if (itemMetadata.containsKey(MediaMetadata2.METADATA_KEY_TITLE)) {
                                assertEquals(title, itemMetadata.getString(
                                        MediaMetadata2.METADATA_KEY_TITLE));
                            }
                            if (itemMetadata.containsKey(MediaMetadata2.METADATA_KEY_ARTIST)) {
                                assertEquals(artist, itemMetadata.getString(
                                        MediaMetadata2.METADATA_KEY_ARTIST));
                            }
                            if (itemMetadata.containsKey(MediaMetadata2.METADATA_KEY_DURATION)) {
                                assertEquals(duration, itemMetadata.getLong(
                                        MediaMetadata2.METADATA_KEY_DURATION));
                            }
                        }
                        if (latchForUri.getCount() != 0) {
                            latchForUri.countDown();
                        } else {
                            latchForFile.countDown();
                        }
                    }
                });
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(uriMediaItem);
            }
        });
        assertTrue(latchForUri.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setMediaItem2(fileMediaItem);
            }
        });
        assertTrue(latchForFile.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
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
        return new UriMediaItem2.Builder(mVideoView.getContext(), uri).build();
    }

    private boolean hasCodec(Uri uri) {
        return TestUtils.hasCodecsForUri(mActivity, uri);
    }

    private boolean hasCodec(AssetFileDescriptor afd) {
        return TestUtils.hasCodecsForFileDescriptor(afd);
    }

    private MediaController2 createController(MediaController2.ControllerCallback callback) {
        MediaController2 controller = new MediaController2(mVideoView.getContext(),
                mVideoView.getMediaSessionToken2(), mMainHandlerExecutor, callback);
        mControllers.add(controller);
        return controller;
    }
}
