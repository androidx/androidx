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

package androidx.leanback.widget;

import static androidx.leanback.widget.BaseCardView.LayoutParams.MATCH_PARENT;
import static androidx.leanback.widget.BaseCardView.LayoutParams.VIEW_TYPE_INFO;
import static androidx.leanback.widget.BaseCardView.LayoutParams.VIEW_TYPE_MAIN;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@SmallTest
@RunWith(JUnit4.class)
public class BaseCardViewTest {

    BaseCardView.LayoutParams createLayoutParams(int viewType, int width, int height) {
        BaseCardView.LayoutParams lp = new BaseCardView.LayoutParams(width, height);
        lp.viewType = viewType;
        return lp;
    }

    void mockInfoHeightAnimation(BaseCardView view, int width, int startHeight, int endHeight) {
        ((BaseCardView.InfoHeightAnimation) view.getAnimation()).mockStart();
        measureAndLayout(view, width, startHeight);
        ((BaseCardView.InfoHeightAnimation) view.getAnimation()).mockEnd();
        assertNull(view.getAnimation());
        measureAndLayout(view, width, endHeight);
    }

    void mockInfoAlphaAnimation(BaseCardView view, View infoView,
            float startAlpha, float endAlpha) {
        ((BaseCardView.InfoAlphaAnimation) view.getAnimation()).mockStart();
        assertEquals(startAlpha, infoView.getAlpha(), 0.01f);
        assertEquals(View.VISIBLE, infoView.getVisibility());
        ((BaseCardView.InfoAlphaAnimation) view.getAnimation()).mockEnd();
        assertNull(view.getAnimation());
        assertEquals(endAlpha, infoView.getAlpha(), 0.01f);
        assertEquals(endAlpha == 0f? View.GONE: View.VISIBLE, infoView.getVisibility());
    }

    void measureAndLayout(View view, int expectedWidth, int expectedHeight) {
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        assertEquals(expectedWidth, view.getMeasuredWidth());
        assertEquals(expectedHeight, view.getMeasuredHeight());
        view.layout(0, 0, expectedWidth, expectedHeight);
    }

    void verifyLayoutTimes(View.OnLayoutChangeListener listener, int timesCalled) {
        verify(listener, times(timesCalled)).onLayoutChange(any(View.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class),
                any(Integer.class), any(Integer.class), any(Integer.class), any(Integer.class));
    }

    @Test
    public void infoOver_InfoVisibleAlways() {
        BaseCardView cardView = new BaseCardView(InstrumentationRegistry.getContext());
        View main = new View(cardView.getContext());
        main.setLayoutParams(createLayoutParams(VIEW_TYPE_MAIN, 500, 500));
        cardView.addView(main);
        View info = new View(cardView.getContext());
        View.OnLayoutChangeListener onLayout = Mockito.mock(View.OnLayoutChangeListener.class);
        info.addOnLayoutChangeListener(onLayout);
        info.setLayoutParams(createLayoutParams(VIEW_TYPE_INFO, MATCH_PARENT, 200));
        cardView.addView(info);
        cardView.setCardType(BaseCardView.CARD_TYPE_INFO_OVER);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);

        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setActivated(true);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setActivated(false);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setSelected(true);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setSelected(false);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);
    }

    @Test
    public void infoUnder_InfoVisibleAlways() {
        BaseCardView cardView = new BaseCardView(InstrumentationRegistry.getContext());
        View main = new View(cardView.getContext());
        main.setLayoutParams(createLayoutParams(VIEW_TYPE_MAIN, 500, 500));
        cardView.addView(main);
        View info = new View(cardView.getContext());
        View.OnLayoutChangeListener onLayout = Mockito.mock(View.OnLayoutChangeListener.class);
        info.addOnLayoutChangeListener(onLayout);
        info.setLayoutParams(createLayoutParams(VIEW_TYPE_INFO, MATCH_PARENT, 200));
        cardView.addView(info);
        cardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ALWAYS);

        measureAndLayout(cardView, 500, 700);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setActivated(true);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 700);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setActivated(false);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 700);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setSelected(true);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 700);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setSelected(false);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());
        measureAndLayout(cardView, 500, 700);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);
    }

    @Test
    public void infoUnder_InfoVisibleActivated() {
        BaseCardView cardView = new BaseCardView(InstrumentationRegistry.getContext());
        View main = new View(cardView.getContext());
        main.setLayoutParams(createLayoutParams(VIEW_TYPE_MAIN, 500, 500));
        cardView.addView(main);
        View info = new View(cardView.getContext());
        View.OnLayoutChangeListener onLayout = Mockito.mock(View.OnLayoutChangeListener.class);
        info.addOnLayoutChangeListener(onLayout);
        info.setLayoutParams(createLayoutParams(VIEW_TYPE_INFO, MATCH_PARENT, 200));
        cardView.addView(info);
        cardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_ACTIVATED);

        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 0);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setActivated(true);
        assertTrue(cardView.isLayoutRequested());
        measureAndLayout(cardView, 500, 700);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        cardView.setActivated(false);
        assertTrue(cardView.isLayoutRequested());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        // changing selected does not affect size
        cardView.setSelected(true);
        assertFalse(cardView.isLayoutRequested());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);

        // changing selected does not affect size
        cardView.setSelected(false);
        assertFalse(cardView.isLayoutRequested());
        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 1);
        assertEquals(1f, info.getAlpha(), 0.01f);
    }

    @Test
    public void infoUnder_InfoVisibleSelected() {
        final BaseCardView cardView = new BaseCardView(InstrumentationRegistry.getContext());
        View main = new View(cardView.getContext());
        main.setLayoutParams(createLayoutParams(VIEW_TYPE_MAIN, 500, 500));
        cardView.addView(main);
        View info = new View(cardView.getContext());
        View.OnLayoutChangeListener onLayout = Mockito.mock(View.OnLayoutChangeListener.class);
        info.addOnLayoutChangeListener(onLayout);
        info.setLayoutParams(createLayoutParams(VIEW_TYPE_INFO, MATCH_PARENT, 200));
        cardView.addView(info);
        cardView.setCardType(BaseCardView.CARD_TYPE_INFO_UNDER);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_SELECTED);

        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 0);
        assertEquals(1f, info.getAlpha(), 0.01f);

        // changing activated does not affect size
        cardView.setActivated(true);
        measureAndLayout(cardView, 500, 500);
        assertNull(cardView.getAnimation());
        assertEquals(1f, info.getAlpha(), 0.01f);

        // start info height animation 500 -> 700
        cardView.setSelected(true);
        assertEquals(1f, info.getAlpha(), 0.01f);
        mockInfoHeightAnimation(cardView, 500 /*width*/, 500 /*startHeight*/, 700 /*endHeight*/);

        // changing activated does not affect size
        cardView.setActivated(false);
        assertEquals(1f, info.getAlpha(), 0.01f);
        measureAndLayout(cardView, 500, 700);
        assertNull(cardView.getAnimation());

        // start info height animation 700 -> 500
        cardView.setSelected(false);
        assertEquals(1f, info.getAlpha(), 0.01f);
        mockInfoHeightAnimation(cardView, 500 /*width*/, 700 /*startHeight*/, 500 /*endHeight*/);
    }

    @Test
    public void infoOver_InfoVisibleSelected() {
        final BaseCardView cardView = new BaseCardView(InstrumentationRegistry.getContext());
        View main = new View(cardView.getContext());
        main.setLayoutParams(createLayoutParams(VIEW_TYPE_MAIN, 500, 500));
        cardView.addView(main);
        View info = new View(cardView.getContext());
        View.OnLayoutChangeListener onLayout = Mockito.mock(View.OnLayoutChangeListener.class);
        info.addOnLayoutChangeListener(onLayout);
        info.setLayoutParams(createLayoutParams(VIEW_TYPE_INFO, MATCH_PARENT, 200));
        cardView.addView(info);
        cardView.setCardType(BaseCardView.CARD_TYPE_INFO_OVER);
        cardView.setInfoVisibility(BaseCardView.CARD_REGION_VISIBLE_SELECTED);

        measureAndLayout(cardView, 500, 500);
        verifyLayoutTimes(onLayout, 0);
        assertFalse(cardView.isSelected());
        assertFalse(cardView.isActivated());
        assertEquals(0f, info.getAlpha(), 0.01f);

        cardView.setActivated(true);
        assertFalse(cardView.isLayoutRequested());
        assertNull(cardView.getAnimation());

        assertEquals(info.getVisibility(), View.GONE);
        assertEquals(0f, info.getAlpha(), 0.01f);
        // start info animation alpha 0f -> 1f
        cardView.setSelected(true);
        // change visibility when start animation causing layout requested
        assertTrue(cardView.isLayoutRequested());
        measureAndLayout(cardView, 500, 500);
        mockInfoAlphaAnimation(cardView, info, 0f, 1f);
        assertEquals(1f, info.getAlpha(), 0.01f);

        // start info animation alpha 1f -> 0f
        cardView.setSelected(false);
        assertFalse(cardView.isLayoutRequested());
        mockInfoAlphaAnimation(cardView, info, 1f, 0f);
        assertEquals(0f, info.getAlpha(), 0.01f);
    }

}
