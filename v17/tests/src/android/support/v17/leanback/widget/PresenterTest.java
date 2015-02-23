/*
 * Copyright (C) 2015 The Android Open Source Project
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
package android.support.v17.leanback.widget;

import android.graphics.Bitmap;
import android.support.v17.leanback.app.HeadersFragment;
import android.support.v17.leanback.R;
import android.test.AndroidTestCase;
import android.widget.FrameLayout;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class PresenterTest extends AndroidTestCase {

    public void testZoomFactors() throws Throwable {
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL);
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM);
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_LARGE);
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_XSMALL);
        try {
            new ListRowPresenter(100);
            fail("Should have thrown exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    private void testHeaderPresenter(RowHeaderPresenter p) {
        int expectedVisibility;
        Presenter.ViewHolder vh = p.onCreateViewHolder(new FrameLayout(getContext()));
        p.onBindViewHolder(vh, null);
        expectedVisibility = p.isNullItemVisibilityGone() ? View.GONE : View.VISIBLE;
        assertTrue("Header visibility",
                vh.view.getVisibility() == expectedVisibility);
        p.onBindViewHolder(vh, new Row(null));
        assertTrue("Header visibility",
                vh.view.getVisibility() == expectedVisibility);
        p.onBindViewHolder(vh, new Row(new HeaderItem("")));
        assertTrue("Header visibility",
                vh.view.getVisibility() == View.VISIBLE);
    }

    public void testHeaderPresenter() throws Throwable {
        HeadersFragment hf = new HeadersFragment();
        PresenterSelector ps = hf.getPresenterSelector();
        Presenter p = ps.getPresenter(new Object());
        assertTrue("Row header instance",
                p instanceof RowHeaderPresenter);
        assertFalse("isNullItemVisibilityGone",
                ((RowHeaderPresenter) p).isNullItemVisibilityGone());
        testHeaderPresenter((RowHeaderPresenter) p);

        ListRowPresenter lrp = new ListRowPresenter();
        assertTrue("Row header instance",
                lrp.getHeaderPresenter() instanceof RowHeaderPresenter);
        RowHeaderPresenter rhp = (RowHeaderPresenter) lrp.getHeaderPresenter();
        assertTrue("isNullItemVisibilityGone",
                rhp.isNullItemVisibilityGone());
        testHeaderPresenter(rhp);
    }

    public void testPlaybackControlsRowPresenter() {
        Presenter detailsPresenter = new AbstractDetailsDescriptionPresenter() {
            @Override
            protected void onBindDescription(ViewHolder vh, Object item) {
                vh.getTitle().setText("The quick brown fox jumped over the lazy dog");
                vh.getSubtitle().setText("Subtitle");
            }
        };
        PlaybackControlsRowPresenter controlsRowPresenter = new PlaybackControlsRowPresenter(
                detailsPresenter);
        PlaybackControlsRowPresenter.ViewHolder vh = (PlaybackControlsRowPresenter.ViewHolder)
                controlsRowPresenter.onCreateViewHolder(new FrameLayout(getContext()));

        Object item = new Object();
        PlaybackControlsRow controlsRow = new PlaybackControlsRow(item);

        controlsRowPresenter.onBindRowViewHolder(vh, controlsRow);
        assertEquals("Controls card right panel layout height",
                vh.view.findViewById(R.id.controls_card_right_panel).getLayoutParams().height,
                LayoutParams.WRAP_CONTENT);
        assertEquals("Description dock layout height",
                vh.view.findViewById(R.id.description_dock).getLayoutParams().height,
                LayoutParams.WRAP_CONTENT);
        controlsRowPresenter.onUnbindRowViewHolder(vh);

        controlsRow.setImageBitmap(
                getContext(), Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888));
        controlsRowPresenter.onBindRowViewHolder(vh, controlsRow);
        AssertHelper.assertGreaterThan("Controls card right panel layout height",
                vh.view.findViewById(R.id.controls_card_right_panel).getLayoutParams().height, 0);
        assertEquals("Description dock layout height",
                vh.view.findViewById(R.id.description_dock).getLayoutParams().height, 0);
        controlsRowPresenter.onUnbindRowViewHolder(vh);
    }
}
