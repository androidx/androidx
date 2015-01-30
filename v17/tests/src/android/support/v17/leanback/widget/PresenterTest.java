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

import android.support.v17.leanback.app.HeadersFragment;
import android.test.AndroidTestCase;
import android.widget.FrameLayout;
import android.view.View;

import junit.framework.Assert;

public class PresenterTest extends AndroidTestCase {

    public void testZoomFactors() throws Throwable {
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_SMALL);
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_MEDIUM);
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_LARGE);
        new ListRowPresenter(FocusHighlight.ZOOM_FACTOR_XSMALL);
        try {
            new ListRowPresenter(100);
            Assert.fail("Should have thrown exception");
        } catch (IllegalArgumentException exception) {
        }
    }

    private void testHeaderPresenter(RowHeaderPresenter p) {
        int expectedVisibility;
        Presenter.ViewHolder vh = p.onCreateViewHolder(new FrameLayout(getContext()));
        p.onBindViewHolder(vh, null);
        expectedVisibility = p.isNullItemVisibilityGone() ? View.GONE : View.VISIBLE;
        Assert.assertTrue(vh.view.getVisibility() == expectedVisibility);
        p.onBindViewHolder(vh, new Row(null));
        Assert.assertTrue(vh.view.getVisibility() == expectedVisibility);
        p.onBindViewHolder(vh, new Row(new HeaderItem("")));
        Assert.assertTrue(vh.view.getVisibility() == View.VISIBLE);
    }

    public void testHeaderPresenter() throws Throwable {
        HeadersFragment hf = new HeadersFragment();
        PresenterSelector ps = hf.getPresenterSelector();
        Presenter p = ps.getPresenter(new Object());
        Assert.assertTrue(p instanceof RowHeaderPresenter);
        Assert.assertFalse(((RowHeaderPresenter) p).isNullItemVisibilityGone());
        testHeaderPresenter((RowHeaderPresenter) p);

        ListRowPresenter lrp = new ListRowPresenter();
        Assert.assertTrue(lrp.getHeaderPresenter() instanceof RowHeaderPresenter);
        RowHeaderPresenter rhp = (RowHeaderPresenter) lrp.getHeaderPresenter();
        Assert.assertTrue(rhp.isNullItemVisibilityGone());
        testHeaderPresenter(rhp);
    }
}
