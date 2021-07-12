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
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
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
import androidx.test.filters.FlakyTest;
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
 * Test {@link MediaControlView} with a {@link SessionPlayer} or a {@link MediaController}.
 */
@RunWith(Parameterized.class)
@LargeTest
public class MediaControlView_WithPlayerTest extends MediaWidgetTestBase {
    @Parameterized.Parameters(name = "PlayerType={0}")
    public static List<String> getPlayerTypes() {
        return Arrays.asList(PLAYER_TYPE_MEDIA_CONTROLLER, PLAYER_TYPE_MEDIA_PLAYER);
    }

    private static final long FFWD_MS = 30000L;
    private static final long REW_MS = 10000L;

    private String mPlayerType;
    private MediaControlViewTestActivity mActivity;
    private MediaControlView mMediaControlView;
    private MediaItem mFileSchemeMediaItem;

    @SuppressWarnings("deprecation")
    @Rule
    public androidx.test.rule.ActivityTestRule<MediaControlViewTestActivity> mActivityRule =
            new androidx.test.rule.ActivityTestRule<>(MediaControlViewTestActivity.class);

    public MediaControlView_WithPlayerTest(String playerType) {
        mPlayerType = playerType;
    }

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mMediaControlView = mActivity.findViewById(
                androidx.media2.widget.test.R.id.mediacontrolview);

        Uri fileSchemeUri = getResourceUri(
                androidx.media2.widget.test.R.raw.test_file_scheme_video);
        mFileSchemeMediaItem = createTestMediaItem(fileSchemeUri);
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
    public void setPlayerOrController_PausedState() throws Throwable {
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
        }, mFileSchemeMediaItem, null);
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
    public void setPlayerOrController_PlayingState() throws Throwable {
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
        }, mFileSchemeMediaItem, null);
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
    public void setPlayerAndController_MultipleTimes() throws Throwable {
        DefaultPlayerCallback callback1 = new DefaultPlayerCallback();
        PlayerWrapper wrapper1 = createPlayerWrapper(callback1, mFileSchemeMediaItem, null);
        assertTrue(callback1.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        DefaultPlayerCallback callback2 = new DefaultPlayerCallback();
        PlayerWrapper wrapper2 = createPlayerWrapperOfPlayer(callback2, mFileSchemeMediaItem, null);
        assertTrue(callback2.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        DefaultPlayerCallback callback3 = new DefaultPlayerCallback();
        PlayerWrapper wrapper3 = createPlayerWrapperOfController(callback3, mFileSchemeMediaItem,
                null);
        assertTrue(callback3.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        DefaultPlayerCallback callback4 = new DefaultPlayerCallback();
        PlayerWrapper wrapper4 = createPlayerWrapper(callback4, mFileSchemeMediaItem, null);
        assertTrue(callback4.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        setPlayerWrapper(wrapper1);
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed())).perform(click());
        assertTrue(callback1.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback2.mPlayingLatch.getCount());
        assertEquals(1, callback3.mPlayingLatch.getCount());
        assertEquals(1, callback4.mPlayingLatch.getCount());
        callback1.mPlayingLatch = new CountDownLatch(1);

        setPlayerWrapper(wrapper2);
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed())).perform(click());
        assertTrue(callback2.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback1.mPlayingLatch.getCount());
        assertEquals(1, callback3.mPlayingLatch.getCount());
        assertEquals(1, callback4.mPlayingLatch.getCount());
        callback2.mPlayingLatch = new CountDownLatch(1);

        setPlayerWrapper(wrapper3);
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed())).perform(click());
        assertTrue(callback3.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback1.mPlayingLatch.getCount());
        assertEquals(1, callback2.mPlayingLatch.getCount());
        assertEquals(1, callback4.mPlayingLatch.getCount());
        callback3.mPlayingLatch = new CountDownLatch(1);

        setPlayerWrapper(wrapper4);
        onView(allOf(withId(R.id.pause), isCompletelyDisplayed())).perform(click());
        assertTrue(callback4.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback1.mPlayingLatch.getCount());
        assertEquals(1, callback2.mPlayingLatch.getCount());
        assertEquals(1, callback3.mPlayingLatch.getCount());
    }

    @Test
    public void ffwdButtonClick() throws Throwable {
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
        }, mFileSchemeMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForPausedState.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.ffwd), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void rewButtonClick() throws Throwable {
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
        }, mFileSchemeMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(latchForFfwd.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(allOf(withId(R.id.rew), isCompletelyDisplayed())).perform(click());
        assertTrue(latchForRew.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    public void prevNextButtonClick() throws Throwable {
        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        final List<MediaItem> playlist = createTestPlaylist();
        final PlayerWrapper playerWrapper = createPlayerWrapper(callback, null, playlist);
        setPlayerWrapper(playerWrapper);

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(0, playerWrapper.getCurrentMediaItemIndex());
        onView(allOf(withId(R.id.prev), isCompletelyDisplayed()))
                .check(matches(not(isEnabled())));
        onView(allOf(withId(R.id.next), isCompletelyDisplayed())).check(matches(isEnabled()));
        callback.mItemLatch = new CountDownLatch(1);
        onView(allOf(withId(R.id.next), isCompletelyDisplayed())).perform(click());

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, playerWrapper.getCurrentMediaItemIndex());
        onView(allOf(withId(R.id.prev), isCompletelyDisplayed())).check(matches(isEnabled()));
        onView(allOf(withId(R.id.next), isCompletelyDisplayed())).check(matches(isEnabled()));
        callback.mItemLatch = new CountDownLatch(1);
        onView(allOf(withId(R.id.next), isCompletelyDisplayed())).perform(click());

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(2, playerWrapper.getCurrentMediaItemIndex());
        onView(allOf(withId(R.id.prev), isCompletelyDisplayed())).check(matches(isEnabled()));
        onView(allOf(withId(R.id.next), isCompletelyDisplayed()))
                .check(matches(not(isEnabled())));
        callback.mItemLatch = new CountDownLatch(1);
        onView(allOf(withId(R.id.prev), isCompletelyDisplayed())).perform(click());

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, playerWrapper.getCurrentMediaItemIndex());
        onView(allOf(withId(R.id.prev), isCompletelyDisplayed())).check(matches(isEnabled()));
        onView(allOf(withId(R.id.next), isCompletelyDisplayed())).check(matches(isEnabled()));
    }

    @FlakyTest(bugId = 179623359)
    @Test
    public void setMetadataForNonMusicFile() throws Throwable {
        final String title = "BigBuckBunny";
        final CountDownLatch latch = new CountDownLatch(1);

        MediaMetadata existingMetadata = mFileSchemeMediaItem.getMetadata();
        MediaMetadata.Builder metadataBuilder = existingMetadata == null
                ? new MediaMetadata.Builder()
                : new MediaMetadata.Builder(existingMetadata);
        MediaMetadata metadata = metadataBuilder
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .build();
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
        }, mFileSchemeMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.title_text)).check(matches(withText(title)));
    }

    @Test
    public void subtitleButtonVisibilityForMusicFile() throws Throwable {
        Uri uri = getResourceUri(androidx.media2.widget.test.R.raw.test_music);
        final MediaItem uriMediaItem = createTestMediaItem(uri);

        final CountDownLatch latch = new CountDownLatch(1);
        final PlayerWrapper playerWrapper = createPlayerWrapper(new PlayerWrapper.PlayerCallback() {
            @Override
            void onTracksChanged(@NonNull PlayerWrapper player, @NonNull List<TrackInfo> tracks) {
                assertNotNull(tracks);
                if (tracks.isEmpty()) {
                    // This callback can be called before tracks are available after setMediaItem
                    return;
                }
                latch.countDown();
            }
        }, uriMediaItem, null);
        setPlayerWrapper(playerWrapper);
        assertTrue(latch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        onView(withId(R.id.subtitle)).check(matches(not(isDisplayed())));
    }

    @Test
    public void updateAndSelectSubtitleTrack() throws Throwable {
        Uri uri = getResourceUri(
                androidx.media2.widget.test.R.raw.testvideo_with_2_subtitle_tracks);

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
            void onTracksChanged(@NonNull PlayerWrapper player, @NonNull List<TrackInfo> tracks) {
                if (mFirstSubtitleTrack != null) {
                    return;
                }
                assertNotNull(tracks);
                for (int i = 0; i < tracks.size(); i++) {
                    TrackInfo trackInfo = tracks.get(i);
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
        }, mediaItem, null);
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
    public void attachViewAndPlayAfterSetPlayerOrController() throws Throwable {
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
        }, mFileSchemeMediaItem, null);
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewGroup layout = mActivity.findViewById(
                        androidx.media2.widget.test.R.id.framelayout);
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

    private PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item, @Nullable List<MediaItem> playlist) {
        return createPlayerWrapperOfType(callback, item, playlist, mPlayerType);
    }
}
