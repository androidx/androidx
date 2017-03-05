// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from PlaybackFragmentTest.java.  DO NOT MODIFY. */

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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.media.PlaybackControlGlue;
import android.support.v17.leanback.media.PlaybackGlue;
import android.support.v17.leanback.testutils.PollingCheck;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v17.leanback.widget.SparseArrayObjectAdapter;
import android.view.KeyEvent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class PlaybackSupportFragmentTest extends SingleSupportFragmentTestBase {

    private static final String TAG = "PlaybackSupportFragmentTest";
    private static final long TRANSITION_LENGTH = 1000;

    @Test
    public void testDetachCalledWhenDestroyFragment() throws Throwable {
        launchAndWaitActivity(PlaybackTestSupportFragment.class, 1000);
        final PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) mActivity.getTestFragment();
        PlaybackGlue glue = fragment.getGlue();
        activityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mActivity.finish();
            }
        });
        PollingCheck.waitFor(new PollingCheck.PollingCheckCondition() {
            @Override
            public boolean canProceed() {
                return fragment.mDestroyCalled;
            }
        });
        assertNull(glue.getHost());
    }

    @Test
    public void testSelectedListener() throws Throwable {
        launchAndWaitActivity(PlaybackTestSupportFragment.class, 1000);
        PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) mActivity.getTestFragment();

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
        Thread.sleep(TRANSITION_LENGTH);
        verify(selectedListener, times(1)).onItemSelected(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The selected action should be rewind", rewind, itemCaptor.getValue());

        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        Thread.sleep(TRANSITION_LENGTH);
        verify(selectedListener, times(2)).onItemSelected(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The selected action should be thumbsUp", thumbsUp, itemCaptor.getValue());

        // Now navigate down to a ListRow item.
        ListRow listRow0 = (ListRow) fragment.getAdapter().get(1);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
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
        launchAndWaitActivity(PlaybackTestSupportFragment.class, 1000);
        PlaybackTestSupportFragment fragment = (PlaybackTestSupportFragment) mActivity.getTestFragment();

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
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(1)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The clicked action should be playPause", playPause, itemCaptor.getValue());

        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(1)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(2)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The clicked action should be rewind", rewind, itemCaptor.getValue());

        sendKeys(KeyEvent.KEYCODE_DPAD_LEFT);
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(2)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(3)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same controls row should be passed to the listener", controlsRow,
                rowCaptor.getValue());
        assertSame("The clicked action should be thumbsUp", thumbsUp, itemCaptor.getValue());

        // Now navigate down to a ListRow item.
        ListRow listRow0 = (ListRow) fragment.getAdapter().get(1);

        sendKeys(KeyEvent.KEYCODE_DPAD_DOWN);
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(3)).onItemClicked(any(Presenter.ViewHolder.class),
                any(Object.class), any(RowPresenter.ViewHolder.class), any(Row.class));
        sendKeys(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        verify(clickedListener, times(4)).onItemClicked(itemVHCaptor.capture(),
                itemCaptor.capture(), rowVHCaptor.capture(), rowCaptor.capture());
        assertSame("Same list row should be passed to the listener", listRow0,
                rowCaptor.getValue());
        boolean listRowItemPassed = (itemCaptor.getValue() == listRow0.getAdapter().get(0)
                || itemCaptor.getValue() == listRow0.getAdapter().get(1));
        assertTrue("None of the items in the first ListRow are passed to the click listener.",
                listRowItemPassed);
    }

}
