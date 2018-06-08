/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.leanback.media;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackControlsRowPresenter;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.RowPresenter;

import org.junit.Test;
import org.mockito.Mockito;

@LargeTest
public class PlaybackControlGlueTest {

    public static class PlaybackControlGlueImpl extends PlaybackControlGlue {

        public PlaybackControlGlueImpl(Context context) {
            super(context, new int[] {PLAYBACK_SPEED_FAST_L0, PLAYBACK_SPEED_FAST_L1});
        }

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
            return null;
        }

        @Override
        public CharSequence getMediaSubtitle() {
            return null;
        }

        @Override
        public int getMediaDuration() {
            return 0;
        }

        @Override
        public Drawable getMediaArt() {
            return null;
        }

        @Override
        public long getSupportedActions() {
            return 0;
        }

        @Override
        public int getCurrentSpeedId() {
            return 0;
        }

        @Override
        public int getCurrentPosition() {
            return 0;
        }
    }

    Context mContext;
    PlaybackControlGlue mGlue;

    @Test
    public void usingDefaultRowAndPresenter() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = Mockito.spy(new PlaybackControlGlueImpl(mContext));
            }
        });
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        mGlue.setHost(host);
        Mockito.verify(mGlue, times(1)).onAttachedToHost(host);
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertTrue(host.mPlaybackRowPresenter instanceof PlaybackControlsRowPresenter);
        assertTrue(host.mRow instanceof PlaybackControlsRow);

    }
    @Test
    public void customRowPresenter() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = Mockito.spy(new PlaybackControlGlueImpl(mContext));
            }
        });
        PlaybackRowPresenter presenter = new PlaybackRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                return new RowPresenter.ViewHolder(new LinearLayout(parent.getContext()));
            }
        };
        mGlue.setPlaybackRowPresenter(presenter);
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        mGlue.setHost(host);
        Mockito.verify(mGlue, times(1)).onAttachedToHost(host);
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertSame(host.mPlaybackRowPresenter, presenter);
        assertTrue(host.mRow instanceof PlaybackControlsRow);

    }

    @Test
    public void customControlsRow() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = Mockito.spy(new PlaybackControlGlueImpl(mContext));
            }
        });
        PlaybackControlsRow row = new PlaybackControlsRow(mContext);
        mGlue.setControlsRow(row);
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        mGlue.setHost(host);
        Mockito.verify(mGlue, times(1)).onAttachedToHost(host);
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertTrue(host.mPlaybackRowPresenter instanceof PlaybackControlsRowPresenter);
        assertSame(host.mRow, row);

    }

    @Test
    public void customRowAndPresenter() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = Mockito.spy(new PlaybackControlGlueImpl(mContext));
            }
        });
        PlaybackControlsRow row = new PlaybackControlsRow(mContext);
        mGlue.setControlsRow(row);
        PlaybackRowPresenter presenter = new PlaybackRowPresenter() {
            @Override
            protected RowPresenter.ViewHolder createRowViewHolder(ViewGroup parent) {
                return new RowPresenter.ViewHolder(new LinearLayout(parent.getContext()));
            }
        };
        mGlue.setPlaybackRowPresenter(presenter);
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        mGlue.setHost(host);
        Mockito.verify(mGlue, times(1)).onAttachedToHost(host);
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertSame(host.mPlaybackRowPresenter, presenter);
        assertSame(host.mRow, row);

    }
}
