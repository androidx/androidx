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

import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.graphics.drawable.Drawable;

import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.support.annotation.UiThread;

import android.test.suitebuilder.annotation.SmallTest;
import android.view.KeyEvent;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PlaybackControlGlueTest {


    static class PlayControlGlueImpl extends PlaybackControlGlue {
        int mSpeedId = PLAYBACK_SPEED_PAUSED;

        PlayControlGlueImpl(Context context, int[] seekSpeeds) {
            super(context, seekSpeeds);
        }

        PlayControlGlueImpl(Context context, int[] ffSpeeds, int[] rwSpeeds) {
            super(context, ffSpeeds, rwSpeeds);
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
        }
    }

    Context context;
    PlaybackControlGlue glue;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override
                public void run() {
                    glue = new PlayControlGlueImpl(context, new int[]{
                            PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0,
                            PlaybackControlGlue.PLAYBACK_SPEED_FAST_L1,
                            PlaybackControlGlue.PLAYBACK_SPEED_FAST_L2
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
                .lookup(PlaybackControlGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_REWIND);

        assertFalse(glue.isMediaPlaying());
        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // click multiple times to reach PLAYBACK_SPEED_FAST_L2
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        assertEquals(1, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L1, glue.getCurrentSpeedId());
        assertEquals(2, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(3, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());
        glue.onActionClicked(fastForward);
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(3, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // press playPause again put it back to play
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
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
                .lookup(PlaybackControlGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_REWIND);

        assertFalse(glue.isMediaPlaying());
        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // click multiple times to reach PLAYBACK_SPEED_FAST_L2
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(1, rewind.getIndex());
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L1, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(2, rewind.getIndex());
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(3, rewind.getIndex());
        glue.onActionClicked(rewind);
        assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L2, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(3, rewind.getIndex());

        // press playPause again put it back to play
        glue.onActionClicked(playPause);
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
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
                .lookup(PlaybackControlGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_REWIND);

        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // Testing keycodes that will not abort seek
        final int[] noAbortSeekKeyCodes = new int[] {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER
        };
        for (int i = 0; i < noAbortSeekKeyCodes.length; i++) {
            glue.onActionClicked(fastForward);
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(1, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, noAbortSeekKeyCodes[i]);
            glue.onKey(null, noAbortSeekKeyCodes[i], kv);
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            glue.onActionClicked(playPause);
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
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
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(1, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, abortSeekKeyCodes[i]);
            glue.onKey(null, abortSeekKeyCodes[i], kv);
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
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
                .lookup(PlaybackControlGlue.ACTION_PLAY_PAUSE);
        PlaybackControlsRow.MultiAction fastForward = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_FAST_FORWARD);
        PlaybackControlsRow.MultiAction rewind = (PlaybackControlsRow.MultiAction) adapter
                .lookup(PlaybackControlGlue.ACTION_REWIND);

        glue.onActionClicked(playPause);
        assertTrue(glue.isMediaPlaying());
        assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
        assertEquals(0, fastForward.getIndex());
        assertEquals(0, rewind.getIndex());

        // Testing keycodes that will not abort seek
        final int[] noAbortSeekKeyCodes = new int[] {
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER
        };
        for (int i = 0; i < noAbortSeekKeyCodes.length; i++) {
            glue.onActionClicked(rewind);
            assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(1, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, noAbortSeekKeyCodes[i]);
            glue.onKey(null, noAbortSeekKeyCodes[i], kv);
            assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            glue.onActionClicked(playPause);
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
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
            assertEquals(-PlaybackControlGlue.PLAYBACK_SPEED_FAST_L0, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(1, rewind.getIndex());
            KeyEvent kv = new KeyEvent(KeyEvent.ACTION_DOWN, abortSeekKeyCodes[i]);
            glue.onKey(null, abortSeekKeyCodes[i], kv);
            assertEquals(PlaybackControlGlue.PLAYBACK_SPEED_NORMAL, glue.getCurrentSpeedId());
            assertEquals(0, fastForward.getIndex());
            assertEquals(0, rewind.getIndex());
        }
    }
}
