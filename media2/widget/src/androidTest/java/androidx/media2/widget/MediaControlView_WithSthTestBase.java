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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.net.Uri;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.session.MediaController;
import androidx.media2.widget.test.R;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Base class for testing {@link MediaControlView} with a {@link SessionPlayer} or
 * {@link MediaController}.
 */
public abstract class MediaControlView_WithSthTestBase extends MediaWidgetTestBase {
    private static final long FFWD_MS = 30000L;
    private static final long REW_MS = 10000L;

    private MediaControlViewTestActivity mActivity;
    private MediaControlView mMediaControlView;
    private MediaItem mFileSchemeMediaItem;

    @Rule
    public ActivityTestRule<MediaControlViewTestActivity> mActivityRule =
            new ActivityTestRule<>(MediaControlViewTestActivity.class);

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mMediaControlView = mActivity.findViewById(R.id.mediacontrolview);

        Uri fileSchemeUri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_file_scheme_video);
        mFileSchemeMediaItem = createTestMediaItem(fileSchemeUri);

        setKeepScreenOn(mActivityRule);
        checkAttachedToWindow(mMediaControlView);
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

    /**
     * It also tests clicking play button
     */
    @Test
    public void testSetPlayerOrController_PausedState() throws Throwable {
        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForPlayingState = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
                if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    latchForPausedState.countDown();
                } else if (state == SessionPlayer.PLAYER_STATE_PLAYING) {
                    latchForPlayingState.countDown();
                }
            }
        }, mFileSchemeMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.mcv2_play_button_desc)))
                .perform(click());
        assertTrue(latchForPlayingState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.mcv2_pause_button_desc)));
    }

    /**
     * It also tests clicking pause button
     */
    @Test
    public void testSetPlayerOrController_PlayingState() throws Throwable {
        final CountDownLatch latchForPreparedState = new CountDownLatch(1);
        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForPlayingState = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            private int mState = SessionPlayer.PLAYER_STATE_IDLE;

            @Override
            public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
                if (mState == SessionPlayer.PLAYER_STATE_IDLE
                        && state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    latchForPreparedState.countDown();
                }
                if (state == SessionPlayer.PLAYER_STATE_PLAYING) {
                    latchForPlayingState.countDown();
                }
                if (mState == SessionPlayer.PLAYER_STATE_PLAYING
                        && state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    latchForPausedState.countDown();
                }
                mState = state;
            }
        }, mFileSchemeMediaItem);
        assertTrue(latchForPreparedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        playerWrapper.play();
        assertTrue(latchForPlayingState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        setPlayerWrapper(playerWrapper);
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.mcv2_pause_button_desc)))
                .perform(click());
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.mcv2_play_button_desc)));
    }

    @Test
    public void testFfwdButtonClick() throws Throwable {
        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForFfwd = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            public void onSeekCompleted(@NonNull PlayerWrapper player, long position) {
                if (position >= FFWD_MS) {
                    latchForFfwd.countDown();
                }
            }

            @Override
            public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
                if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    latchForPausedState.countDown();
                }
            }
        }, mFileSchemeMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.ffwd), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testRewButtonClick() throws Throwable {
        final CountDownLatch latchForFfwd = new CountDownLatch(1);
        final CountDownLatch latchForRew = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            long mExpectedPosition = FFWD_MS;
            final long mDelta = 1000L;

            @Override
            public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
                if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    mExpectedPosition = FFWD_MS;
                    player.seekTo(mExpectedPosition);
                }
            }

            @Override
            public void onSeekCompleted(@NonNull PlayerWrapper player, long position) {
                // Ignore the initial seek. Internal MediaPlayer behavior can be changed.
                if (position == 0 && mExpectedPosition == FFWD_MS) {
                    return;
                }
                assertTrue(equalsSeekPosition(mExpectedPosition, position, mDelta));
                if (mExpectedPosition == FFWD_MS) {
                    mExpectedPosition = position - REW_MS;
                    latchForFfwd.countDown();
                } else {
                    latchForRew.countDown();
                }
            }

            private boolean equalsSeekPosition(long expected, long actual, long delta) {
                return (actual < expected + delta) && (actual > expected - delta);
            }
        }, mFileSchemeMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.rew), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForRew.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testSetMetadataForNonMusicFile() throws Throwable {
        final String title = "BigBuckBunny";
        final CountDownLatch latch = new CountDownLatch(1);
        final MediaMetadata metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title).build();
        mFileSchemeMediaItem.setMetadata(metadata);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            public void onCurrentMediaItemChanged(@NonNull PlayerWrapper player,
                    @Nullable MediaItem item) {
                if (item != null) {
                    assertNotNull(item.getMetadata());
                    assertEquals(title, metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                    latch.countDown();
                }
            }
        }, mFileSchemeMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.title_text)).check(matches(withText(title)));
    }

    @Test
    public void testButtonVisibilityForMusicFile() throws Throwable {
        Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.test_music);
        final MediaItem uriMediaItem = createTestMediaItem(uri);

        final CountDownLatch latch = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            public void onTrackInfoChanged(@NonNull PlayerWrapper player,
                    @NonNull List<TrackInfo> trackInfos) {
                latch.countDown();
            }
        }, uriMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.subtitle)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testUpdateAndSelectSubtitleTrack() throws Throwable {
        Uri uri = Uri.parse("android.resource://" + mContext.getPackageName() + "/"
                + R.raw.testvideo_with_2_subtitle_tracks);

        final String subtitleTrackOffText = mContext.getResources().getString(
                R.string.MediaControlView_subtitle_off_text);
        final String subtitleTrack1Text = mContext.getResources().getString(
                R.string.MediaControlView_subtitle_track_number_text, 1);

        final MediaItem mediaItem = createTestMediaItem(uri);

        final CountDownLatch latchForReady = new CountDownLatch(1);
        final CountDownLatch latchForTrackUpdate = new CountDownLatch(1);
        final CountDownLatch latchForSubtitleSelect = new CountDownLatch(1);
        final CountDownLatch latchForSubtitleDeselect = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            private TrackInfo mFirstSubtitleTrack;

            @Override
            public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
                if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    latchForReady.countDown();
                }
            }

            @Override
            public void onTrackInfoChanged(@NonNull PlayerWrapper player,
                    @NonNull List<TrackInfo> trackInfos) {
                if (mFirstSubtitleTrack != null) {
                    return;
                }
                assertNotNull(trackInfos);
                for (int i = 0; i < trackInfos.size(); i++) {
                    TrackInfo trackInfo = trackInfos.get(i);
                    if (trackInfo.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE) {
                        mFirstSubtitleTrack = trackInfo;
                        latchForTrackUpdate.countDown();
                        break;
                    }
                }
            }

            @Override
            public void onTrackSelected(@NonNull PlayerWrapper player,
                    @NonNull TrackInfo trackInfo) {
                assertEquals(mFirstSubtitleTrack, trackInfo);
                latchForSubtitleSelect.countDown();
            }

            @Override
            public void onTrackDeselected(@NonNull PlayerWrapper player,
                    @NonNull TrackInfo trackInfo) {
                assertEquals(mFirstSubtitleTrack, trackInfo);
                latchForSubtitleDeselect.countDown();
            }
        }, mediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForReady.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        // MediaPlayer needs a surface to be set in order to produce subtitle tracks
        playerWrapper.setSurface(mActivity.getSurfaceHolder().getSurface());
        assertTrue(latchForTrackUpdate.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        onView(withId(R.id.subtitle)).check(matches(isClickable()));
        onView(withId(R.id.subtitle)).perform(click());
        onView(withText(subtitleTrack1Text)).inRoot(isPlatformPopup())
                .check(matches(isCompletelyDisplayed()));
        onView(withText(subtitleTrack1Text)).inRoot(isPlatformPopup()).perform(click());
        assertTrue(latchForSubtitleSelect.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        onView(withId(R.id.subtitle)).check(matches(isClickable()));
        onView(withId(R.id.subtitle)).perform(click());
        onView(withText(subtitleTrackOffText)).inRoot(isPlatformPopup())
                .check(matches(isCompletelyDisplayed()));
        onView(withText(subtitleTrackOffText)).inRoot(isPlatformPopup()).perform(click());
        assertTrue(latchForSubtitleDeselect.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testAttachViewAndPlayAfterSetPlayerOrController() throws Throwable {
        final CountDownLatch latchForPausedState = new CountDownLatch(1);
        final CountDownLatch latchForPlayingState = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            public void onPlayerStateChanged(@NonNull PlayerWrapper player, int state) {
                if (state == SessionPlayer.PLAYER_STATE_PAUSED) {
                    latchForPausedState.countDown();
                } else if (state == SessionPlayer.PLAYER_STATE_PLAYING) {
                    latchForPlayingState.countDown();
                }
            }
        }, mFileSchemeMediaItem);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup layout = mActivity.findViewById(R.id.framelayout);
                layout.removeView(mMediaControlView);
                mMediaControlView = new MediaControlView(mActivity);
                if (playerWrapper.mPlayer != null) {
                    mMediaControlView.setPlayer(playerWrapper.mPlayer);
                } else if (playerWrapper.mController != null) {
                    mMediaControlView.setMediaController(playerWrapper.mController);
                }
                layout.addView(mMediaControlView);
            }
        });
        checkAttachedToWindow(mMediaControlView);
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.mcv2_play_button_desc)))
                .perform(click());
        assertTrue(latchForPlayingState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed()))
                .check(matches(withContentDescription(R.string.mcv2_pause_button_desc)));
    }

    private void setPlayerWrapper(final PlayerWrapper playerWrapper) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (playerWrapper.mPlayer != null) {
                    mMediaControlView.setPlayer(playerWrapper.mPlayer);
                } else if (playerWrapper.mController != null) {
                    mMediaControlView.setMediaController(playerWrapper.mController);
                }
            }
        });
    }

    abstract PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item);
}
