// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from PlaybackControlGlueTest.java.  DO NOT MODIFY. */

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

package android.support.v17.leanback.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.KeyEvent;
import android.view.View;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class PlaybackControlSupportGlueTest {


    static class PlayControlGlueImpl extends PlaybackControlSupportGlue {
        int mSpeedId = PLAYBACK_SPEED_PAUSED;
        // number of times onRowChanged callback is called
        int mOnRowChangedCallCount = 0;

        PlayControlGlueImpl(Context context, int[] seekSpeeds) {
            super(context, seekSpeeds);
        }

        PlayControlGlueImpl(Context context, int[] ffSpeeds, int[] rwSpeeds) {
            super(context, ffSpeeds, rwSpeeds);
        }

        PlayControlGlueImpl(Context context, PlaybackOverlaySupportFragment fragment,
                                   int[] seekSpeeds) {
            super(context, fragment, seekSpeeds);
        }

        @Override
        public boolean hasValidMedia() {
            return true;
        }

        @Override
        public boolean isMediaPlaying() {
            return mSpeedId == PLAYBACK_SPEED_NORMAL;
        }

        @Override
        public CharSequence getMediaTitle() {
            return "DUMP TITLE";
        }

        @Override
        public CharSequence getMediaSubtitle() {
            return "DUMP SUBTITLE";
        }

        @Override
        public int getMediaDuration() {
            return 50000;
        }

        @Override
        public Drawable getMediaArt() {
            return null;
        }

        @Override
        public long getSupportedActions() {
            return ACTION_REWIND | ACTION_FAST_FORWARD | ACTION_PLAY_PAUSE;
        }

        @Override
        public int getCurrentSpeedId() {
            return mSpeedId;
        }

        @Override
        public int getCurrentPosition() {
            return 5000;
        }

        @Override
        protected void startPlayback(int speed) {
            mSpeedId = speed;
        }

        @Override
        protected void pausePlayback() {
            mSpeedId = PLAYBACK_SPEED_PAUSED;
        }

        @Override
        protected void skipToNext() {
        }

        @Override
        protected void skipToPrevious() {
        }

        @Override
        protected void onRowChanged(PlaybackControlsRow row) {
            mOnRowChangedCallCount++;
        }

        public void notifyMetaDataChanged() {
            onMetadataChanged();
            onStateChanged();
        }

        public int getOnRowChangedCallCount() {
            return mOnRowChangedCallCount;
        }
    }

    Context context;
    PlaybackControlSupportGlue glue;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    glue = new PlayControlGlueImpl(context, new int[]{
                            PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0,
                            PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L1,
                            PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2
                    });
                }
            });
        } catch (Throwable throwable) {
            Assert.fail(throwable.getMessage());
        }
    }

    @Test
    public void testFastForwardToMaxThenReset() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_REWIND);

        assertFalse(glue.isMediaPlaying());
        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // click multiple times to reach PLAYBACK_SPEED_FAST_L2
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        assertEquals(1, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L1, glue.getCurrentSpeedId());
        assertEquals(2, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(3, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(3, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // press playPause again put it back to play
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
    }

    @Test
    public void testFastRewindToMaxThenReset() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_REWIND);

        assertFalse(glue.isMediaPlaying());
        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // click multiple times to reach PLAYBACK_SPEED_FAST_L2
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(1, rewind.getIndex());
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L1, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(2, rewind.getIndex());
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(3, rewind.getIndex());
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(3, rewind.getIndex());

        // press playPause again put it back to play
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
    }

    @Test
    public void testFastForwardAbortKeyCodes() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_REWIND);

        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // Testing keycodes that will not abort seek
        final int[] noAbortSeekKeyCodes = new int[] {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER
        };
        for (int i = 0; i < noAbortSeekKeyCodes.length; i++) {
            glue.onActionClicked(fastForward);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(1, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, noAbortSeekKeyCodes[i]);
            glue.onKey(null, noAbortSeekKeyCodes[i], kv);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            glue.onActionClicked(playPause);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        }

        // Testing abortSeekKeyCodes
        final int[] abortSeekKeyCodes = new int[] {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE
        };
        for (int i = 0; i < abortSeekKeyCodes.length; i++) {
            glue.onActionClicked(fastForward);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(1, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, abortSeekKeyCodes[i]);
            glue.onKey(null, abortSeekKeyCodes[i], kv);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
        }
    }

    @Test
    public void testRewindAbortKeyCodes() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_REWIND);

        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // Testing keycodes that will not abort seek
        final int[] noAbortSeekKeyCodes = new int[] {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER
        };
        for (int i = 0; i < noAbortSeekKeyCodes.length; i++) {
            glue.onActionClicked(rewind);
            assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(1, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, noAbortSeekKeyCodes[i]);
            glue.onKey(null, noAbortSeekKeyCodes[i], kv);
            assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            glue.onActionClicked(playPause);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        }

        // Testing abortSeekKeyCodes
        final int[] abortSeekKeyCodes = new int[] {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_ESCAPE
        };
        for (int i = 0; i < abortSeekKeyCodes.length; i++) {
            glue.onActionClicked(rewind);
            assertEquals(-PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(1, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, abortSeekKeyCodes[i]);
            glue.onKey(null, abortSeekKeyCodes[i], kv);
            assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
        }
    }

    @Test
    public void testMediaPauseButtonOnFF() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);

        glue.onActionClicked(playPause);
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PAUSE, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PAUSE));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPauseButtonOnPlay() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);

        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PAUSE, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PAUSE));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPauseButtonOnPause() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);

        glue.onActionClicked(playPause);
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PAUSE, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PAUSE));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPlayButtonOnFF() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);

        glue.onActionClicked(playPause);
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPlayButtonOnPlay() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);

        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPlayButtonOnPause() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);

        glue.onActionClicked(playPause);
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPlayPauseButtonOnFF() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_FAST_FORWARD);

        glue.onActionClicked(playPause);
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPlayPauseButtonOnPlay() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);

        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
    }

    @Test
    public void testMediaPlayPauseButtonOnPause() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);

        glue.onActionClicked(playPause);
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());
        glue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, new KeyEvent(KeyEvent.ACTION_DOWN,
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
    }

    @Test
    public void testOnItemClickedListener() {
        PlaybackControlsRow row = new PlaybackControlsRow();
        final PlaybackOverlaySupportFragment[] fragmentResult = new PlaybackOverlaySupportFragment[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragmentResult[0] = new PlaybackOverlaySupportFragment();
            }
        });
        PlaybackOverlaySupportFragment fragment = fragmentResult[0];
        glue.setHost(new PlaybackControlSupportGlue.PlaybackSupportGlueHostOld(fragment));
        glue.setControlsRow(row);
        SparseArrayObjectAdapter adapter = (SparseArrayObjectAdapter)
                row.getPrimaryActionsAdapter();
        PlaybackControlsRow.MultiAction playPause = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlSupportGlue.ACTION_PLAY_PAUSE);
        OnItemViewClickedListener listener = Mockito.mock(OnItemViewClickedListener.class);
        glue.setOnItemViewClickedListener(listener);

        // create fake row ViewHolder and fade item ViewHolder
        View rowView = new View(context);
        View view = new View(context);
        PlaybackRowPresenter.ViewHolder rowVh = new PlaybackRowPresenter.ViewHolder(rowView);
        Presenter.ViewHolder vh = new Presenter.ViewHolder(view);

        // Initially media is paused
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_PAUSED, glue.getCurrentSpeedId());

        // simulate a click inside PlaybackOverlaySupportFragment's PlaybackRow.
        fragment.getOnItemViewClickedListener().onItemClicked(vh, playPause, rowVh, row);
        verify(listener, times(0)).onItemClicked(vh, playPause, rowVh, row);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());

        // simulate a click on object other than PlaybackRow.
        Object regularItem = new Object();
        Row regularRow = new Row();
        RowPresenter.ViewHolder regularRowViewHolder = new RowPresenter.ViewHolder(rowView);
        Presenter.ViewHolder regularViewHOlder = new Presenter.ViewHolder(view);
        fragment.getOnItemViewClickedListener().onItemClicked(regularViewHOlder, regularItem,
                regularRowViewHolder, regularRow);
        verify(listener, times(1)).onItemClicked(regularViewHOlder, regularItem,
                regularRowViewHolder, regularRow);
        assertEquals(PlaybackControlSupportGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
    }

    @Test
    public void testOnRowChangedCallback() throws Exception {
        final PlaybackOverlaySupportFragment[] fragmentResult = new
                PlaybackOverlaySupportFragment[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragmentResult[0] = new PlaybackOverlaySupportFragment();
            }
        });
        PlaybackOverlaySupportFragment fragment = fragmentResult[0];
        PlayControlGlueImpl playbackGlue = new PlayControlGlueImpl(context, fragment,
                new int[]{
                        PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0,
                        PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L1,
                        PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2
                });

        // before any controls row is created the count is zero
        assertEquals(playbackGlue.getOnRowChangedCallCount(), 0);
        playbackGlue.createControlsRowAndPresenter();
        // after a controls row is created, onRowChanged() call back is called once
        assertEquals(playbackGlue.getOnRowChangedCallCount(), 1);
        assertEquals(3, playbackGlue.getControlsRow().getPrimaryActionsAdapter().size());
        playbackGlue.notifyMetaDataChanged();
        // onMetaDataChanged() calls updateRowMetadata which ends up calling
        // notifyPlaybackRowChanged on the old host and finally onRowChanged on the glue.
        assertEquals(playbackGlue.getOnRowChangedCallCount(), 2);
        assertEquals(3, playbackGlue.getControlsRow().getPrimaryActionsAdapter().size());
    }


    @Test
    public void testWithoutValidMedia() throws Exception {
        final PlaybackOverlaySupportFragment[] fragmentResult = new
                PlaybackOverlaySupportFragment[1];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fragmentResult[0] = new PlaybackOverlaySupportFragment();
            }
        });
        final boolean[] hasValidMedia = new boolean[] {false};
        PlaybackOverlaySupportFragment fragment = fragmentResult[0];
        PlayControlGlueImpl playbackGlue = new PlayControlGlueImpl(context, fragment,
                new int[]{
                        PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L0,
                        PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L1,
                        PlaybackControlSupportGlue.PLAYBACK_SPEED_FAST_L2
                }) {
            @Override
            public boolean hasValidMedia() {
                return hasValidMedia[0];
            }
        };

        // before any controls row is created the count is zero
        assertEquals(playbackGlue.getOnRowChangedCallCount(), 0);
        playbackGlue.createControlsRowAndPresenter();
        // after a controls row is created, onRowChanged() call back is called once
        assertEquals(playbackGlue.getOnRowChangedCallCount(), 1);
        // enven hasValidMedia() is false, we should still have three buttons.
        assertEquals(3, playbackGlue.getControlsRow().getPrimaryActionsAdapter().size());

        hasValidMedia[0] = true;
        playbackGlue.notifyMetaDataChanged();
        // onMetaDataChanged() calls updateRowMetadata which ends up calling
        // notifyPlaybackRowChanged on the old host and finally onRowChanged on the glue.
        assertEquals(playbackGlue.getOnRowChangedCallCount(), 2);
        assertEquals(3, playbackGlue.getControlsRow().getPrimaryActionsAdapter().size());
    }

}
