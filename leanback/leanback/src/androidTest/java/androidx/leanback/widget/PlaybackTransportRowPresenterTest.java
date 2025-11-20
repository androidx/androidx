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

package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewParent;

import androidx.leanback.media.PlaybackTransportControlGlue;
import androidx.leanback.media.PlayerAdapter;
import androidx.leanback.widget.PlaybackSeekDataProvider.ResultCallback;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;

import org.jspecify.annotations.NonNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;

@MediumTest
public class PlaybackTransportRowPresenterTest {

    Context mContext;
    PlaybackTransportControlGlue<PlayerAdapter> mGlue;
    PlaybackGlueHostImplWithViewHolder mHost;
    PlayerAdapter mImpl;
    PlaybackTransportRowPresenter.ViewHolder mViewHolder;
    AbstractDetailsDescriptionPresenter.ViewHolder mDescriptionViewHolder;
    int mNumbThumbs;

    @Before
    public void setUp() {
        mContext = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                androidx.leanback.R.style.Theme_Leanback);
        mHost = new PlaybackGlueHostImplWithViewHolder(mContext);
        mImpl = Mockito.mock(PlayerAdapter.class);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue = new PlaybackTransportControlGlue<PlayerAdapter>(mContext, mImpl) {
                    @Override
                    protected void onCreatePrimaryActions(@NonNull ArrayObjectAdapter
                            primaryActionsAdapter) {
                        super.onCreatePrimaryActions(primaryActionsAdapter);
                        primaryActionsAdapter.add(
                                new PlaybackControlsRow.ClosedCaptioningAction(mContext));
                    }

                    @Override
                    protected void onCreateSecondaryActions(@NonNull ArrayObjectAdapter
                            secondaryActionsAdapter) {
                        secondaryActionsAdapter.add(
                                new PlaybackControlsRow.HighQualityAction(mContext));
                        secondaryActionsAdapter.add(
                                new PlaybackControlsRow.PictureInPictureAction(mContext));
                    }
                };
                mGlue.setHost(mHost);

            }
        });
        mViewHolder = (PlaybackTransportRowPresenter.ViewHolder) mHost.mViewHolder;
        mDescriptionViewHolder = (AbstractDetailsDescriptionPresenter.ViewHolder)
                mViewHolder.mDescriptionViewHolder;
        mNumbThumbs = mViewHolder.mThumbsBar.getChildCount();
        assertTrue((mNumbThumbs & 1) != 0);
    }

    void sendKeyUIThread(int keyCode) {
        sendKeyUIThread(keyCode, 1);
    }

    void sendKeyUIThread(final int keyCode, final int repeat) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mHost.sendKeyDownUp(keyCode, repeat);
            }
        });
    }

    void verifyGetThumbCalls(int firstHeroIndex, int lastHeroIndex,
            PlaybackSeekDataProvider provider, long[] positions) {
        int firstThumbIndex = Math.max(firstHeroIndex - (mNumbThumbs / 2), 0);
        int lastThumbIndex = Math.min(lastHeroIndex + (mNumbThumbs / 2), positions.length - 1);
        for (int i = firstThumbIndex; i <= lastThumbIndex; i++) {
            Mockito.verify(provider, times(1)).getThumbnail(eq(i), any(ResultCallback.class));
        }
        Mockito.verify(provider, times(0)).getThumbnail(
                eq(firstThumbIndex - 1), any(ResultCallback.class));
        Mockito.verify(provider, times(0)).getThumbnail(
                eq(firstThumbIndex - 2), any(ResultCallback.class));
        Mockito.verify(provider, times(0)).getThumbnail(
                eq(lastThumbIndex + 1), any(ResultCallback.class));
        Mockito.verify(provider, times(0)).getThumbnail(
                eq(lastThumbIndex + 2), any(ResultCallback.class));
    }

    void verifyAtHeroIndexWithDifferentPosition(long position, int heroIndex) {
        assertEquals(position, mGlue.getControlsRow().getCurrentPosition());
        assertEquals(mViewHolder.mThumbHeroIndex, heroIndex);
    }

    void verifyAtHeroIndex(long[] positions, int heroIndex) {
        verifyAtHeroIndex(positions, heroIndex, null);
    }

    void verifyAtHeroIndex(long[] positions, int heroIndex, Bitmap[] thumbs) {
        assertEquals(positions[heroIndex], mGlue.getControlsRow().getCurrentPosition());
        assertEquals(mViewHolder.mThumbHeroIndex, heroIndex);
        if (thumbs != null) {
            int start = Math.max(0, mViewHolder.mThumbHeroIndex - mNumbThumbs / 2);
            int end = Math.min(positions.length - 1, mViewHolder.mThumbHeroIndex + mNumbThumbs / 2);
            verifyThumbBitmaps(thumbs, start, end,
                    mViewHolder.mThumbsBar, start + mNumbThumbs / 2 - mViewHolder.mThumbHeroIndex,
                    end + mNumbThumbs / 2 - mViewHolder.mThumbHeroIndex);
        }
    }

    void verifyThumbBitmaps(Bitmap[] thumbs, int start, int end,
            ThumbsBar thumbsBar, int childStart, int childEnd) {
        assertEquals(end - start, childEnd - childStart);
        for (int i = start; i <= end; i++) {
            assertSame(thumbs[i], thumbsBar.getThumbBitmap(childStart + (i - start)));
        }
        for (int i = 0; i < childStart; i++) {
            assertNull(thumbsBar.getThumbBitmap(i));
        }
        for (int i = childEnd + 1; i < mNumbThumbs; i++) {
            assertNull(thumbsBar.getThumbBitmap(i));
        }
    }

    @Test
    public void progressUpdating() {
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(123L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);

        mGlue.play();
        Mockito.verify(mImpl, times(1)).play();
        mGlue.pause();
        Mockito.verify(mImpl, times(1)).pause();
        mGlue.seekTo(1231);
        Mockito.verify(mImpl, times(1)).seekTo(1231);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);
        assertEquals(123L, mGlue.getCurrentPosition());
        assertEquals(20000L, mGlue.getDuration());
        assertEquals(321L, mGlue.getBufferedPosition());
        assertEquals(123L, mViewHolder.mCurrentTimeInMs);
        assertEquals(20000L, mViewHolder.mTotalTimeInMs);
        assertEquals(321L, mViewHolder.mSecondaryProgressInMs);

        when(mImpl.getCurrentPosition()).thenReturn(124L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        assertEquals(124L, mGlue.getControlsRow().getCurrentPosition());
        assertEquals(124L, mViewHolder.mCurrentTimeInMs);
        when(mImpl.getBufferedPosition()).thenReturn(333L);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);
        assertEquals(333L, mGlue.getControlsRow().getBufferedPosition());
        assertEquals(333L, mViewHolder.mSecondaryProgressInMs);
        when(mImpl.getDuration()).thenReturn((long) (Integer.MAX_VALUE) * 2);
        mImpl.getCallback().onDurationChanged(mImpl);
        assertEquals((long) (Integer.MAX_VALUE) * 2, mGlue.getControlsRow().getDuration());
        assertEquals((long) (Integer.MAX_VALUE) * 2, mViewHolder.mTotalTimeInMs);
    }

    @Test
    public void mediaInfo() {
        final ColorDrawable art = new ColorDrawable();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mGlue.setTitle("xyz");
                mGlue.setSubtitle("zyx");
                mGlue.setArt(art);
            }
        });
        assertEquals("xyz", mDescriptionViewHolder.mTitle.getText());
        assertEquals("zyx", mDescriptionViewHolder.mSubtitle.getText());
        assertSame(art, mViewHolder.mImageView.getDrawable());
    }

    static boolean isDescendant(View view, View descendant) {
        while (descendant != view) {
            ViewParent p = descendant.getParent();
            if (!(p instanceof View)) {
                return false;
            }
            descendant = (View) p;
        }
        return true;
    }

    @Test
    public void navigateRightInPrimary() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mControlsVh.mControlBar.getChildAt(0).requestFocus();
            }
        });
        View view = mViewHolder.view.findFocus();
        assertTrue(isDescendant(mViewHolder.mControlsVh.mControlBar.getChildAt(0), view));
        assertTrue(isDescendant(mViewHolder.mControlsVh.mControlBar.getChildAt(1),
                view.focusSearch(View.FOCUS_RIGHT)));
    }

    @Test
    public void navigateRightInSecondary() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mSecondaryControlsVh.mControlBar.getChildAt(0).requestFocus();
            }
        });
        View view = mViewHolder.view.findFocus();
        assertTrue(isDescendant(mViewHolder.mSecondaryControlsVh.mControlBar.getChildAt(0), view));
        assertTrue(isDescendant(mViewHolder.mSecondaryControlsVh.mControlBar.getChildAt(1),
                view.focusSearch(View.FOCUS_RIGHT)));
    }

    @Test
    public void navigatePrimaryDownToProgress() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mControlsVh.mControlBar.getChildAt(0).requestFocus();
            }
        });
        View view = mViewHolder.view.findFocus();
        assertTrue(isDescendant(mViewHolder.mControlsVh.mControlBar.getChildAt(0), view));
        assertSame(mViewHolder.mProgressBar, view.focusSearch(View.FOCUS_DOWN));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void navigateProgressUpToPrimary() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mProgressBar.requestFocus();
            }
        });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mProgressBar.focusSearch(View.FOCUS_UP).requestFocus();
            }
        });
        View view = mViewHolder.view.findFocus();
        assertTrue(isDescendant(mViewHolder.mControlsVh.mControlBar.getChildAt(0), view));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void navigateProgressDownToSecondary() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mProgressBar.requestFocus();
            }
        });
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mProgressBar.focusSearch(View.FOCUS_DOWN).requestFocus();
            }
        });
        View view = mViewHolder.view.findFocus();
        assertTrue(isDescendant(mViewHolder.mSecondaryControlsVh.mControlBar.getChildAt(0), view));
    }

    @Test
    public void navigateSecondaryUpToProgress() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                mViewHolder.mSecondaryControlsVh.mControlBar.getChildAt(0).requestFocus();
            }
        });
        View view = mViewHolder.view.findFocus();
        assertTrue(isDescendant(mViewHolder.mSecondaryControlsVh.mControlBar.getChildAt(0), view));
        assertSame(mViewHolder.mProgressBar, view.focusSearch(View.FOCUS_UP));
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekAndConfirm() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(0L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();
        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 1);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 2);

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_CENTER);
        Mockito.verify(mImpl).seekTo(positions[2]);

        verifyGetThumbCalls(1, 2, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void playSeekToZero() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(0L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();
        mGlue.setSeekProvider(provider);

        // start play
        mGlue.play();
        verify(mImpl).play();

        // focus to seek bar
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        // using DPAD_RIGHT to initiate seeking
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 2);
        // press DPAD_CENTER to seek to new position and continue play
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(mImpl).seekTo(positions[2]);
        verify(mImpl).play();

        // press DPAD_LEFT seek to 0
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, 0);
        // press DPAD_CENTER to continue play from 0
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(mImpl).seekTo(0);
        verify(mImpl).play();
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void playSeekAndCancel() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(0L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();
        mGlue.setSeekProvider(provider);

        // start play
        mGlue.play();
        verify(mImpl).play();

        // focus to seek bar
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        // using DPAD_RIGHT to initiate seeking
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 2);
        // press DPAD_CENTER to seek to new position and continue play
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_CENTER);
        verify(mImpl).seekTo(positions[2]);
        verify(mImpl).play();

        // press DPAD_LEFT seek to 0
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, 0);
        // press BACK to cancel and continue play from position before seek
        sendKeyUIThread(KeyEvent.KEYCODE_BACK);
        verify(mImpl).seekTo(positions[2]);
        verify(mImpl).play();
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekHoldKeyDown() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(4489L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(4489L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();
        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        int insertPosition = -1 - Arrays.binarySearch(positions, 4489L);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT, 5);
        verifyAtHeroIndex(positions, insertPosition + 4);
        verifyGetThumbCalls(insertPosition, insertPosition + 4, provider, positions);

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT, 5);
        verifyAtHeroIndex(positions, insertPosition - 1);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekAndCancel() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(0L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();
        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 1);

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 2);

        sendKeyUIThread(KeyEvent.KEYCODE_BACK);
        Mockito.verify(mImpl, times(0)).seekTo(anyInt());
        verifyGetThumbCalls(1, 2, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekUpBetweenTwoKeyPosition() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();

        // initially select between 0 and 1
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn((positions[0] + positions[1]) / 2);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 1);
        verifyGetThumbCalls(1, 1, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekDownBetweenTwoKeyPosition() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();
        assertTrue(positions[0] == 0);

        // initially select between 0 and 1
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn((positions[0] + positions[1]) / 2);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, 0);
        verifyGetThumbCalls(0, 0, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekDownOutOfKeyPositions() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(1000L, 10000L, 101));
        final long[] positions = provider.getSeekPositions();
        assertTrue(positions[0] > 0);

        // initially select between 0 and 1
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn((positions[0] + positions[1]) / 2);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, 0);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndexWithDifferentPosition(0, 0);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndexWithDifferentPosition(0, 0);
        verifyGetThumbCalls(0, 0, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekDownAheadOfKeyPositions() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(1000L, 10000L, 101));
        final long[] positions = provider.getSeekPositions();
        assertTrue(positions[0] > 0);

        // initially select between 0 and 1
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(positions[0] / 2);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndexWithDifferentPosition(0, 0);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 0);
        verifyGetThumbCalls(0, 0, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekUpAheadOfKeyPositions() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(1000L, 10000L, 101));
        final long[] positions = provider.getSeekPositions();
        assertTrue(positions[0] > 0);

        // initially select between 0 and 1
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(positions[0] / 2);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 0);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndexWithDifferentPosition(0, 0);
        verifyGetThumbCalls(0, 0, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekUpOutOfKeyPositions() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();

        // initially select between nth-1 and nth
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn((positions[positions.length - 2]
                + positions[positions.length - 1]) / 2);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, positions.length - 1);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, positions.length - 2);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, positions.length - 1);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndexWithDifferentPosition(20000L, positions.length - 1);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndexWithDifferentPosition(20000L, positions.length - 1);
        verifyGetThumbCalls(positions.length - 2, positions.length - 1, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekUpAfterKeyPositions() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();

        // initially select after last item
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(positions[positions.length - 1] + 100);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndexWithDifferentPosition(20000L, positions.length - 1);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, positions.length - 1);
        verifyGetThumbCalls(positions.length - 1, positions.length - 1, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void seekDownAfterKeyPositions() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        PlaybackSeekProviderSample provider = Mockito.spy(
                new PlaybackSeekProviderSample(10000L, 101));
        final long[] positions = provider.getSeekPositions();

        // initially select after last item
        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(positions[positions.length - 1] + 100);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_LEFT);
        verifyAtHeroIndex(positions, positions.length - 1);
        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndexWithDifferentPosition(20000L, positions.length - 1);
        verifyGetThumbCalls(positions.length - 1, positions.length - 1, provider, positions);
    }

    @Test
    @SdkSuppress(maxSdkVersion = 33) // b/262909049: Failing on SDK 34
    public void thumbLoadedInCallback() {
        if (Build.VERSION.SDK_INT == 33 && !"REL".equals(Build.VERSION.CODENAME)) {
            return; // b/262909049: Do not run this test on pre-release Android U.
        }

        when(mImpl.isPrepared()).thenReturn(true);
        when(mImpl.getCurrentPosition()).thenReturn(0L);
        when(mImpl.getDuration()).thenReturn(20000L);
        when(mImpl.getBufferedPosition()).thenReturn(321L);
        mImpl.getCallback().onCurrentPositionChanged(mImpl);
        mImpl.getCallback().onDurationChanged(mImpl);
        mImpl.getCallback().onBufferedPositionChanged(mImpl);

        final Bitmap[] thumbs = new Bitmap[101];
        for (int i = 0; i < 101; i++) {
            thumbs[i] = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        }
        PlaybackSeekProviderSample provider = new PlaybackSeekProviderSample(10000L, 101) {
            @Override
            public void getThumbnail(int index, ResultCallback callback) {
                callback.onThumbnailLoaded(thumbs[index], index);
            }
        };
        final long[] positions = provider.getSeekPositions();
        mGlue.setSeekProvider(provider);
        mViewHolder.mProgressBar.requestFocus();
        assertTrue(mViewHolder.mProgressBar.hasFocus());

        sendKeyUIThread(KeyEvent.KEYCODE_DPAD_RIGHT);
        verifyAtHeroIndex(positions, 1, thumbs);
    }

}
