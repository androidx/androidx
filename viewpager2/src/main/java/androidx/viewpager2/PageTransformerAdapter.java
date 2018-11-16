/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.viewpager2.widget.ViewPager2.OnPageChangeListener;
import androidx.viewpager2.widget.ViewPager2.PageTransformer;

import java.util.Locale;

/**
 * Translates {@link OnPageChangeListener} events to {@link PageTransformer} events.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
public class PageTransformerAdapter implements OnPageChangeListener {
    private final LinearLayoutManager mLayoutManager;

    private PageTransformer mPageTransformer;

    public PageTransformerAdapter(LinearLayoutManager layoutManager) {
        mLayoutManager = layoutManager;
    }

    /**
     * Sets the PageTransformer. The page transformer will be called for each attached page whenever
     * the scroll position is changed.
     *
     * @param transformer The PageTransformer
     */
    public void setPageTransformer(@Nullable PageTransformer transformer) {
        // TODO: add support for reverseDrawingOrder: b/112892792
        // TODO: add support for pageLayerType: b/112893074
        mPageTransformer = transformer;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        if (mPageTransformer == null) {
            return;
        }

        float transformOffset = -positionOffset;
        for (int i = 0; i < mLayoutManager.getChildCount(); i++) {
            View view = mLayoutManager.getChildAt(i);
            if (view == null) {
                throw new IllegalStateException(String.format(Locale.US,
                        "LayoutManager returned a null child at pos %d/%d while transforming pages",
                        i, mLayoutManager.getChildCount()));
            }
            int currPos = mLayoutManager.getPosition(view);
            float viewOffset = transformOffset + (currPos - position);
            mPageTransformer.transformPage(view, viewOffset);
        }
    }

    @Override
    public void onPageSelected(int position) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }
}
