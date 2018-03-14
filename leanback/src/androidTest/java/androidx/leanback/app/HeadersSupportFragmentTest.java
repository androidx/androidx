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
import static org.junit.Assert.assertTrue;

import android.os.Bundle;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.FocusHighlightHelper;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ItemBridgeAdapter;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.VerticalGridView;

import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class HeadersSupportFragmentTest extends SingleSupportFragmentTestBase {

    static void loadData(ArrayObjectAdapter adapter, int numRows) {
        for (int i = 0; i < numRows; ++i) {
            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter();
            HeaderItem header = new HeaderItem(i, "Row " + i);
            adapter.add(new ListRow(header, listRowAdapter));
        }
    }

    public static class F_defaultScale extends HeadersSupportFragment {
        final ListRowPresenter mPresenter = new ListRowPresenter();

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(mPresenter);
            setAdapter(adapter);
            loadData(adapter, 10);
        }
    }

    @Test
    public void defaultScale() {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(F_defaultScale.class, 1000);

        final VerticalGridView gridView = ((HeadersSupportFragment) activity.getTestFragment())
                .getVerticalGridView();
        ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder)
                gridView.findViewHolderForAdapterPosition(0);
        assertTrue(vh.itemView.getScaleX() - 1.0f > 0.05f);
        assertTrue(vh.itemView.getScaleY() - 1.0f > 0.05f);
    }

    public static class F_disableScale extends HeadersSupportFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ListRowPresenter());
            setAdapter(adapter);
            loadData(adapter, 10);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            FocusHighlightHelper.setupHeaderItemFocusHighlight(getVerticalGridView(), false);
        }
    }

    @Test
    public void disableScale() {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(F_disableScale.class, 1000);

        final VerticalGridView gridView = ((HeadersSupportFragment) activity.getTestFragment())
                .getVerticalGridView();
        ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder)
                gridView.findViewHolderForAdapterPosition(0);
        assertEquals(vh.itemView.getScaleX(), 1f, 0.001f);
        assertEquals(vh.itemView.getScaleY(), 1f, 0.001f);
    }

    public static class F_disableScaleInConstructor extends HeadersSupportFragment {
        public F_disableScaleInConstructor() {
            FocusHighlightHelper.setupHeaderItemFocusHighlight(getBridgeAdapter(), false);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            final ArrayObjectAdapter adapter = new ArrayObjectAdapter(new ListRowPresenter());
            setAdapter(adapter);
            loadData(adapter, 10);
        }
    }

    @Test
    public void disableScaleInConstructor() {
        SingleSupportFragmentTestActivity activity = launchAndWaitActivity(
                F_disableScaleInConstructor.class, 1000);

        final VerticalGridView gridView = ((HeadersSupportFragment) activity.getTestFragment())
                .getVerticalGridView();
        ItemBridgeAdapter.ViewHolder vh = (ItemBridgeAdapter.ViewHolder)
                gridView.findViewHolderForAdapterPosition(0);
        assertEquals(vh.itemView.getScaleX(), 1f, 0.001f);
        assertEquals(vh.itemView.getScaleY(), 1f, 0.001f);
    }
}
