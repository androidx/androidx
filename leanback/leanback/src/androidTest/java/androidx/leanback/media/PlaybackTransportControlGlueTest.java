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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.leanback.widget.PlaybackControlsRow;
import androidx.leanback.widget.PlaybackRowPresenter;
import androidx.leanback.widget.PlaybackTransportRowPresenter;
import androidx.leanback.widget.RowPresenter;
import androidx.test.filters.MediumTest;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.mockito.Mockito;

@MediumTest
public class PlaybackTransportControlGlueTest {

    public static class PlayerAdapterSample extends PlayerAdapter {
        @Override
        public void play() {
        }

        @Override
        public void pause() {
        }
    }

    @SuppressWarnings("unchecked")
    public static class PlaybackTransportControlGlueImpl
            extends PlaybackTransportControlGlue {
        public PlaybackTransportControlGlueImpl(Context context) {
            super(context, new PlayerAdapterSample());
        }

        public PlaybackTransportControlGlueImpl(Context context, PlayerAdapter impl) {
            super(context, impl);
        }
    }

    Context mContext;
    PlaybackTransportControlGlueImpl mGlue;
    PlaybackTransportRowPresenter.ViewHolder mViewHolder;
    PlayerAdapter mAdapter;

    void setupWithMockAdapterAndViewHolder() {
        mContext = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                androidx.leanback.R.style.Theme_Leanback);

        mAdapter = Mockito.mock(PlayerAdapter.class);
        when(mAdapter.isPrepared()).thenReturn(true);
        when(mAdapter.getCurrentPosition()).thenReturn(123L);
        when(mAdapter.getDuration()).thenReturn(20000L);
        when(mAdapter.getBufferedPosition()).thenReturn(321L);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = new PlaybackTransportControlGlueImpl(mContext, mAdapter);
                PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();
                mGlue.setHost(host);

                PlaybackTransportRowPresenter presenter = (PlaybackTransportRowPresenter)
                        mGlue.getPlaybackRowPresenter();
                FrameLayout parent = new FrameLayout(mContext);
                mViewHolder = (PlaybackTransportRowPresenter.ViewHolder)
                        presenter.onCreateViewHolder(parent);
                presenter.onBindViewHolder(mViewHolder, mGlue.getControlsRow());
            }
        });
    }

    void playMockAdapter() {
        mGlue.play();
        Mockito.verify(mAdapter, times(1)).play();
        when(mAdapter.isPlaying()).thenReturn(true);
        mAdapter.getCallback().onPlayStateChanged(mAdapter);
    }

    @Test
    public void usingDefaultRowAndPresenter() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = new PlaybackTransportControlGlueImpl(mContext);
            }
        });
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        mGlue.setHost(host);
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertTrue(host.mPlaybackRowPresenter instanceof PlaybackTransportRowPresenter);
        assertTrue(host.mRow instanceof PlaybackControlsRow);

    }
    @Test
    public void customRowPresenter() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = new PlaybackTransportControlGlueImpl(mContext);
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
                mGlue = new PlaybackTransportControlGlueImpl(mContext);
            }
        });
        PlaybackControlsRow row = new PlaybackControlsRow(mContext);
        mGlue.setControlsRow(row);
        PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();

        mGlue.setHost(host);
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertTrue(host.mPlaybackRowPresenter instanceof PlaybackTransportRowPresenter);
        assertSame(host.mRow, row);

    }

    @Test
    public void customRowAndPresenter() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = new PlaybackTransportControlGlueImpl(mContext);
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
        assertSame(mGlue, host.mGlue);
        assertSame(host, mGlue.getHost());
        assertSame(host.mPlaybackRowPresenter, presenter);
        assertSame(host.mRow, row);

    }

    @Test
    public void playerAdapterTest() {
        setupWithMockAdapterAndViewHolder();

        mGlue.play();
        Mockito.verify(mAdapter, times(1)).play();
        mGlue.pause();
        Mockito.verify(mAdapter, times(1)).pause();
        mGlue.seekTo(123L);
        Mockito.verify(mAdapter, times(1)).seekTo(123L);
        assertEquals(123L, mGlue.getCurrentPosition());
        assertEquals(20000L, mGlue.getDuration());
        assertEquals(321L, mGlue.getBufferedPosition());

        assertSame(mGlue.mAdapterCallback, mAdapter.getCallback());

        when(mAdapter.getCurrentPosition()).thenReturn(124L);
        mAdapter.getCallback().onCurrentPositionChanged(mAdapter);
        assertEquals(124L, mGlue.getControlsRow().getCurrentPosition());

        when(mAdapter.getBufferedPosition()).thenReturn(333L);
        mAdapter.getCallback().onBufferedPositionChanged(mAdapter);
        assertEquals(333L, mGlue.getControlsRow().getBufferedPosition());

        when(mAdapter.getDuration()).thenReturn((long) (Integer.MAX_VALUE) * 2);
        mAdapter.getCallback().onDurationChanged(mAdapter);
        assertEquals((long) (Integer.MAX_VALUE) * 2, mGlue.getControlsRow().getDuration());

    }

    @Test
    public void savePlayerAdapterEventBeforeAttachToHost() {
        mContext = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                androidx.leanback.R.style.Theme_Leanback);

        final PlayerAdapter impl = Mockito.mock(PlayerAdapter.class);
        when(impl.isPrepared()).thenReturn(true);
        when(impl.getCurrentPosition()).thenReturn(123L);
        when(impl.getDuration()).thenReturn(20000L);
        when(impl.getBufferedPosition()).thenReturn(321L);
        final PlaybackGlueHost.PlayerCallback hostCallback = Mockito.mock(
                PlaybackGlueHost.PlayerCallback.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = new PlaybackTransportControlGlueImpl(mContext, impl);
                // fire events before attach to host.
                impl.getCallback().onBufferingStateChanged(impl, true);
                impl.getCallback().onVideoSizeChanged(impl, 200, 150);
                impl.getCallback().onError(impl, 12, "abc");
                PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();
                host.setPlayerCallback(hostCallback);
                mGlue.setHost(host);
            }
        });

        // when attach to host, should pass the buffering state, video size and last error message
        // to the host.
        Mockito.verify(hostCallback, times(1)).onBufferingStateChanged(true);
        Mockito.verify(hostCallback, times(1)).onVideoSizeChanged(200, 150);
        Mockito.verify(hostCallback, times(1)).onError(12, "abc");
        Mockito.reset(hostCallback);

        final PlaybackGlueHost.PlayerCallback hostCallback2 = Mockito.mock(
                PlaybackGlueHost.PlayerCallback.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                PlaybackGlueHostImpl host = new PlaybackGlueHostImpl();
                host.setPlayerCallback(hostCallback2);
                mGlue.setHost(host);
            }
        });

        // when detach from host, should have host stop buffering.
        Mockito.verify(hostCallback, times(1)).onBufferingStateChanged(false);
        Mockito.verify(hostCallback, times(0)).onVideoSizeChanged(anyInt(), anyInt());
        Mockito.verify(hostCallback, times(0)).onError(anyInt(), anyString());

        // attach to a different host, buffering state and video size should be saved, one time
        // error state is not saved.
        Mockito.verify(hostCallback2, times(1)).onBufferingStateChanged(true);
        Mockito.verify(hostCallback2, times(1)).onVideoSizeChanged(200, 150);
        Mockito.verify(hostCallback2, times(0)).onError(anyInt(), anyString());
    }

    @Test
    public void playStateReceivePlayPause() {
        setupWithMockAdapterAndViewHolder();
        playMockAdapter();

        mGlue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        Mockito.verify(mAdapter, times(1)).pause();
    }

    @Test
    public void playStateReceivePause() {
        setupWithMockAdapterAndViewHolder();
        playMockAdapter();

        mGlue.onKey(null, KeyEvent.KEYCODE_MEDIA_PAUSE,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
        Mockito.verify(mAdapter, times(1)).pause();
    }

    @Test
    public void playStateReceivePlay() {
        setupWithMockAdapterAndViewHolder();
        playMockAdapter();

        mGlue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
        Mockito.verify(mAdapter, never()).pause();
    }

    @Test
    public void pauseStateReceivePlayPause() {
        setupWithMockAdapterAndViewHolder();

        mGlue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE));
        Mockito.verify(mAdapter, times(1)).play();
    }

    @Test
    public void pauseStateReceivePause() {
        setupWithMockAdapterAndViewHolder();

        mGlue.onKey(null, KeyEvent.KEYCODE_MEDIA_PAUSE,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
        Mockito.verify(mAdapter, never()).play();
    }

    @Test
    public void pauseStateReceivePlay() {
        setupWithMockAdapterAndViewHolder();

        mGlue.onKey(null, KeyEvent.KEYCODE_MEDIA_PLAY,
                new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
        Mockito.verify(mAdapter, times(1)).play();
    }
}
