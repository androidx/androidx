/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.leanback.media.PlaybackControlGlue;
import androidx.leanback.media.PlaybackGlue;
import androidx.leanback.media.PlaybackGlueHost;
import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.testutils.PollingCheck;
import androidx.leanback.widget.ControlButtonPresenterSelector;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackControlsRowPresenter;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SparseArrayObjectAdapter;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.FlakyTest;
import androidx.test.filters.LargeTest;
import androidx.test.filters.Suppress;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class PlaybackSupportFragmentTest extends SingleSupportFragmentTestBase {

    private static final String TAG = "PlaybackSupportFragmentTest";
    private static final long ACTIVITY_LOAD_MS = 1000;

    private static final long WAIT_FOR_NO_EVENT_MS = 500;

    private static final float MINIMAL_FADEOUT_TRANSLATION = 50f;
    private static final float DELTA = 1f;

    @Test
    public void testDetachCalledWhenDestroyFragment() throws Throwable {
        final SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(PlaybackTestSupportFragment.class, ACTIVITY_LOAD_MS);
        final PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) activity.getTestFragment();
        PlaybackGlue glue = fragment.getGlue();
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }
        });
        // wait one more cycle for fragment destroyed
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
            }
        });
        assertNull(glue.getHost());
    }

    @Test
    public void testSelectedListener() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(PlaybackTestSupportFragment.class, ACTIVITY_LOAD_MS);
        PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) activity.getTestFragment();

        assertTrue(fragment.getView().hasFocus());

        OnItemViewSelectedListener selectedListener = Mockito.mock(
                OnItemViewSelectedListener.class);
        fragment.setOnItemViewSelectedListener(selectedListener);


        PlaybackControlsRow controlsRow = fragment.getGlue().getControlsRow();
        SparseArrayObjectAdapter primaryActionsAdapter = (SparseArrayObjectAdapter)
                controlsRow.getPrimaryActionsAdapter();

        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction)
                primaryActionsAdapter.lookup(PlaybackControlGlue.ACTION_PLAY_PAUSE);

        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction)
                primaryActionsAdapter.lookup(PlaybackControlGlue.ACTION_REWIND);

        PlaybackControlsRow.MultiAction thumbsUp = (PlaybackControlsRow.MultiAction)
                primaryActionsAdapter.lookup(PlaybackControlGlue.ACTION_CUSTOM_LEFT_FIRST);

        ArgumentCaptor<Presenter.ViewHolder> itemVHCaptor =
                ArgumentCaptor.forClass(Presenter.ViewHolder.class);
        ArgumentCaptor<Object> itemCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<RowPresenter.ViewHolder> rowVHCaptor =
                ArgumentCaptor.forClass(RowPresenter.ViewHolder.class);
        ArgumentCaptor<Row> rowCaptor = ArgumentCaptor.forClass(Row.class);


        // First navigate left within PlaybackControlsRow items.
        verify(selectedListener, times(0)).onItemSelected(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        verify(selectedListener, times(1)).onItemSelected(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The selected action should be rewind", rewind, itemCaptor.getValue());

        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        verify(selectedListener, times(2)).onItemSelected(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The selected action should be thumbsUp", thumbsUp, itemCaptor.getValue());

        // Now navigate down to a ListRow item.
        ListRow listRow0 = (ListRow) fragment.getAdapter().get(1);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(fragment.getVerticalGridView());
        verify(selectedListener, times(3)).onItemSelected(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same list row should be passed to the listener", listRow0,
                rowCaptor.getValue());
        // Depending on the focusSearch algorithm, one of the items in the first ListRow must be
        // selected.
        boolean listRowItemPassed = (itemCaptor.getValue() == listRow0.getAdapter().get(0)
                || itemCaptor.getValue() == listRow0.getAdapter().get(1));
        assertTrue("None of the items in the first ListRow are passed to the selected listener.",
                listRowItemPassed);
    }

    @Test
    public void testClickedListener() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(PlaybackTestSupportFragment.class, ACTIVITY_LOAD_MS);
        PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) activity.getTestFragment();

        assertTrue(fragment.getView().hasFocus());

        OnItemViewClickedListener clickedListener = Mockito.mock(OnItemViewClickedListener.class);
        fragment.setOnItemViewClickedListener(clickedListener);


        PlaybackControlsRow controlsRow = fragment.getGlue().getControlsRow();
        SparseArrayObjectAdapter primaryActionsAdapter = (SparseArrayObjectAdapter)
                controlsRow.getPrimaryActionsAdapter();

        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction)
                primaryActionsAdapter.lookup(PlaybackControlGlue.ACTION_PLAY_PAUSE);

        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction)
                primaryActionsAdapter.lookup(PlaybackControlGlue.ACTION_REWIND);

        PlaybackControlsRow.MultiAction thumbsUp = (PlaybackControlsRow.MultiAction)
                primaryActionsAdapter.lookup(PlaybackControlGlue.ACTION_CUSTOM_LEFT_FIRST);

        ArgumentCaptor<Presenter.ViewHolder> itemVHCaptor =
                ArgumentCaptor.forClass(Presenter.ViewHolder.class);
        ArgumentCaptor<Object> itemCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<RowPresenter.ViewHolder> rowVHCaptor =
                ArgumentCaptor.forClass(RowPresenter.ViewHolder.class);
        ArgumentCaptor<Row> rowCaptor = ArgumentCaptor.forClass(Row.class);


        // First navigate left within PlaybackControlsRow items.
        verify(clickedListener, times(0)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(clickedListener, times(1)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The clicked action should be playPause", playPause, itemCaptor.getValue());

        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        verify(clickedListener, times(1)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(clickedListener, times(2)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The clicked action should be rewind", rewind, itemCaptor.getValue());

        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        verify(clickedListener, times(2)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(clickedListener, times(3)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The clicked action should be thumbsUp", thumbsUp, itemCaptor.getValue());

        // Now navigate down to a ListRow item.
        ListRow listRow0 = (ListRow) fragment.getAdapter().get(1);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        waitForScrollIdle(fragment.getVerticalGridView());
        verify(clickedListener, times(3)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(clickedListener, times(4)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same list row should be passed to the listener", listRow0,
                rowCaptor.getValue());
        boolean listRowItemPassed = (itemCaptor.getValue() == listRow0.getAdapter().get(0)
                || itemCaptor.getValue() == listRow0.getAdapter().get(1));
        assertTrue("None of the items in the first ListRow are passed to the click listener.",
                listRowItemPassed);
    }

    @FlakyTest
    @Suppress
    @Test
    public void alignmentRowToBottom() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(PlaybackTestSupportFragment.class, ACTIVITY_LOAD_MS);
        final PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) activity.getTestFragment();

        assertTrue(fragment.getAdapter().size() > 2);

        View playRow = fragment.getVerticalGridView().getChildAt(0);
        assertTrue(playRow.hasFocus());
        assertEquals(playRow.getResources().getDimensionPixelSize(
                androidx.leanback.R.dimen.lb_playback_controls_padding_bottom),
                fragment.getVerticalGridView().getHeight() - playRow.getBottom());

        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fragment.getVerticalGridView().setSelectedPositionSmooth(
                        fragment.getAdapter().size() - 1);
            }
        });
        waitForScrollIdle(fragment.getVerticalGridView());

        View lastRow = fragment.getVerticalGridView().getChildAt(
                fragment.getVerticalGridView().getChildCount() - 1);
        assertEquals(fragment.getAdapter().size() - 1,
                fragment.getVerticalGridView().getChildAdapterPosition(lastRow));
        assertTrue(lastRow.hasFocus());
        assertEquals(lastRow.getResources().getDimensionPixelSize(
                androidx.leanback.R.dimen.lb_playback_controls_padding_bottom),
                fragment.getVerticalGridView().getHeight() - lastRow.getBottom());
    }

    public static class PurePlaybackSupportFragment extends PlaybackSupportFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setFadingEnabled(false);
            PlaybackControlsRow row = new PlaybackControlsRow();
            SparseArrayObjectAdapter primaryAdapter = new SparseArrayObjectAdapter(
                    new ControlButtonPresenterSelector());
            primaryAdapter.set(0, new PlaybackControlsRow.SkipPreviousAction(getActivity()));
            primaryAdapter.set(1, new PlaybackControlsRow.PlayPauseAction(getActivity()));
            primaryAdapter.set(2, new PlaybackControlsRow.SkipNextAction(getActivity()));
            row.setPrimaryActionsAdapter(primaryAdapter);
            row.setSecondaryActionsAdapter(null);
            setPlaybackRow(row);
            setPlaybackRowPresenter(new PlaybackControlsRowPresenter());
        }
    }

    @Test
    public void setupRowAndPresenterWithoutGlue() {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(PurePlaybackSupportFragment.class, ACTIVITY_LOAD_MS);
        final PurePlaybackSupportFragment fragment = (PurePlaybackSupportFragment)
                activity.getTestFragment();

        assertTrue(fragment.getAdapter().size() == 1);
        View playRow = fragment.getVerticalGridView().getChildAt(0);
        assertTrue(playRow.hasFocus());
        assertEquals(playRow.getResources().getDimensionPixelSize(
                androidx.leanback.R.dimen.lb_playback_controls_padding_bottom),
                fragment.getVerticalGridView().getHeight() - playRow.getBottom());
    }

    public static class ControlGlueFragment extends PlaybackSupportFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            int[] ffspeeds = new int[] {PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0,
                    PlaybackControlGlue.PLAYBACK_SPEED_FAST_L1};
            PlaybackGlue glue = new PlaybackControlGlue(
                    getActivity(), ffspeeds) {
                @Override
                public boolean hasValidMedia() {
                    return true;
                }

                @Override
                public boolean isMediaPlaying() {
                    return false;
                }

                @Override
                public CharSequence getMediaTitle() {
                    return "Title";
                }

                @Override
                public CharSequence getMediaSubtitle() {
                    return "SubTitle";
                }

                @Override
                public int getMediaDuration() {
                    return 100;
                }

                @Override
                public Drawable getMediaArt() {
                    return null;
                }

                @Override
                public long getSupportedActions() {
                    return PlaybackControlGlue.ACTION_PLAY_PAUSE;
                }

                @Override
                public int getCurrentSpeedId() {
                    return PlaybackControlGlue.PLAYBACK_SPEED_PAUSED;
                }

                @Override
                public int getCurrentPosition() {
                    return 50;
                }
            };
            glue.setHost(new PlaybackSupportFragmentGlueHost(this));
        }
    }

    @Test
    public void setupWithControlGlue() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(ControlGlueFragment.class, ACTIVITY_LOAD_MS);
        final ControlGlueFragment fragment = (ControlGlueFragment)
                activity.getTestFragment();

        assertTrue(fragment.getAdapter().size() == 1);

        View playRow = fragment.getVerticalGridView().getChildAt(0);
        assertTrue(playRow.hasFocus());
        assertEquals(playRow.getResources().getDimensionPixelSize(
                androidx.leanback.R.dimen.lb_playback_controls_padding_bottom),
                fragment.getVerticalGridView().getHeight() - playRow.getBottom());
    }

    public static class AutohidePlaybackSupportFragment extends PlaybackSupportFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            PlaybackControlsRow row = new PlaybackControlsRow();
            SparseArrayObjectAdapter primaryAdapter = new SparseArrayObjectAdapter(
                    new ControlButtonPresenterSelector());
            row.setPrimaryActionsAdapter(primaryAdapter);
            row.setSecondaryActionsAdapter(null);
            setPlaybackRow(row);
            setPlaybackRowPresenter(new PlaybackControlsRowPresenter());
        }
    }

    @LargeTest
    @Test
    public void autohideAfterPlaying() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(AutohidePlaybackSupportFragment.class, ACTIVITY_LOAD_MS);
        final AutohidePlaybackSupportFragment fragment = (AutohidePlaybackSupportFragment)
                activity.getTestFragment();

        assertTrue(fragment.getAdapter().size() == 1);
        fragment.mAutohideTimerAfterPlayingInMs = 100;
        fragment.mAutohideTimerAfterTickleInMs = 0;

        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.setControlsOverlayAutoHideEnabled(true);
            }
        });
        waitForSlideOut(fragment);
        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.setControlsOverlayAutoHideEnabled(false);
            }
        });
        waitForSlideIn(fragment);

    }

    @Test
    public void noAutohideWhenControlsOverlayAutoHideDisabled() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(AutohidePlaybackSupportFragment.class, ACTIVITY_LOAD_MS);
        final AutohidePlaybackSupportFragment fragment = (AutohidePlaybackSupportFragment)
                activity.getTestFragment();

        assertEquals(1, fragment.getAdapter().size());
        // by default it's enabled when fragment is resumed
        assertTrue(fragment.isControlsOverlayAutoHideEnabled());
        fragment.mAutohideTimerAfterPlayingInMs = 100;
        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.setControlsOverlayAutoHideEnabled(false);
            }
        });
        assertNoSlideOut(fragment);
    }

    @Test
    public void noAutohideAfterTickle() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(AutohidePlaybackSupportFragment.class, ACTIVITY_LOAD_MS);
        final AutohidePlaybackSupportFragment fragment = (AutohidePlaybackSupportFragment)
                activity.getTestFragment();

        assertTrue(fragment.getAdapter().size() == 1);
        fragment.mAutohideTimerAfterPlayingInMs = 100;
        // When mAutohideTimerAfterTickleInMs is 0, no autohide after tickle
        fragment.mAutohideTimerAfterTickleInMs = 0;

        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.tickle();
            }
        });
        assertNoSlideOut(fragment);
    }

    @Test
    public void autohideAfterTickle() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(AutohidePlaybackSupportFragment.class, ACTIVITY_LOAD_MS);
        final AutohidePlaybackSupportFragment fragment = (AutohidePlaybackSupportFragment)
                activity.getTestFragment();

        assertTrue(fragment.getAdapter().size() == 1);
        fragment.mAutohideTimerAfterPlayingInMs = 100;
        // When mAutohideTimerAfterTickleInMs > 0, autohide after tickle
        fragment.mAutohideTimerAfterTickleInMs = 100;

        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.tickle();
            }
        });
        waitForSlideOut(fragment);
    }

    public static class PlayerAdapterSample extends PlayerAdapter {
        boolean mPlaying;

        @Override
        public boolean isPrepared() {
            return true;
        }

        @Override
        public boolean isPlaying() {
            return mPlaying;
        }

        @Override
        public void play() {
            mPlaying = true;
            if (super.getCallback() != null) {
                getCallback().onPlayStateChanged(this);
            }
        }

        @Override
        public void pause() {
            mPlaying = false;
            if (super.getCallback() != null) {
                getCallback().onPlayStateChanged(this);
            }
        }
    }

    public static class ControlsOverlayAutoHideDisabledFragment extends PlaybackSupportFragment {
        PlaybackTransportControlGlue<PlayerAdapterSample> mGlue;
        PlaybackGlueHost mHost = new PlaybackSupportFragmentGlueHost(this);
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mGlue = new PlaybackTransportControlGlue<>(getActivity(), new PlayerAdapterSample());
            mGlue.setHost(mHost);
            mGlue.setControlsOverlayAutoHideEnabled(false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            hideControlsOverlay(false);
        }
    }

    @Test
    public void testControlsOverlayAutoHideDisabled() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(ControlsOverlayAutoHideDisabledFragment.class,
                        ACTIVITY_LOAD_MS);
        final ControlsOverlayAutoHideDisabledFragment fragment =
                (ControlsOverlayAutoHideDisabledFragment) activity.getTestFragment();

        // Sanity check that onViewCreated has made the controls invisible
        assertFalse(fragment.mControlVisible);
        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.mGlue.play();
            }
        });
        // Play/pause should not show controls when setControlsOverlayAutoHideEnabled(false).
        assertFalse(fragment.mControlVisible);
        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.mGlue.pause();
            }
        });
        assertFalse(fragment.mControlVisible);
    }

    public static class KeyEventTickleDisabledFragment extends PlaybackSupportFragment {
        PlaybackTransportControlGlue<PlayerAdapterSample> mGlue;
        PlaybackGlueHost mHost = new PlaybackSupportFragmentGlueHost(this);
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mGlue = new PlaybackTransportControlGlue<>(getActivity(), new PlayerAdapterSample());
            mGlue.setHost(mHost);
            setShowOrHideControlsOverlayOnUserInteraction(false);
        }
    }

    @Test
    public void testKeyEventTickleDisabled() throws Throwable {
        SingleSupportFragmentTestActivity activity =
                launchAndWaitActivity(KeyEventTickleDisabledFragment.class,
                        ACTIVITY_LOAD_MS);
        final KeyEventTickleDisabledFragment fragment =
                (KeyEventTickleDisabledFragment) activity.getTestFragment();

        activityTestRule.runOnUiThread(new Runnable() {
            public void run() {
                fragment.mGlue.play();
            }
        });
        // Initially visible
        assertTrue(fragment.mControlVisible);
        // Then auto slide out after playing
        waitForSlideOut(fragment);
        // KeyEvent should not show controls when setShowOrHideControlsOverlayOnUserInteraction
        // (false) is called
        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        assertFalse(fragment.mControlVisible);
    }

    private static void waitForSlideOut(final PlaybackSupportFragment fragment) {
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return fragment.getVerticalGridView().findViewHolderForAdapterPosition(0)
                        .itemView.getTranslationY() > MINIMAL_FADEOUT_TRANSLATION;
            }
        });
    }

    private static void waitForSlideIn(final PlaybackSupportFragment fragment) {
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return fragment.getVerticalGridView().findViewHolderForAdapterPosition(0)
                        .itemView.getTranslationY() < DELTA;
            }
        });
    }

    private static void assertNoSlideOut(final PlaybackSupportFragment fragment) {
        try {
            PollingCheck.waitFor(WAIT_FOR_NO_EVENT_MS, new PollingCheck.PollingCheckCondition() {
                @Override
                public boolean canProceed() {
                    return fragment.getVerticalGridView().findViewHolderForAdapterPosition(0)
                            .itemView.getTranslationY() > DELTA;
                }
            });
        } catch (AssertionError error) {
            // if timeout, then we are good
            return;
        }
        fail("unexpected faded out");
    }
}
