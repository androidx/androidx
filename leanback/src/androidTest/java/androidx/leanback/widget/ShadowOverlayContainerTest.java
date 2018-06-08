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
package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ShadowOverlayContainerTest {
    private Context mContext;

    @Before
    public void setup() throws Exception {
        mContext = InstrumentationRegistry.getTargetContext();
    }

    @Test
    public void testWrapContent() {
        FrameLayout frameLayout = new FrameLayout(mContext);
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT));
        textView.setText("abc");
        ShadowOverlayContainer container = new ShadowOverlayContainer(mContext);
        container.initialize(true, true, true);
        container.wrap(textView);
        frameLayout.addView(container);
        frameLayout.measure(MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 500, 500);
        assertTrue(textView.getWidth() > 0);
        assertTrue(textView.getWidth() < 500);
        assertTrue(textView.getHeight() > 0);
        assertTrue(textView.getHeight() < 500);
        assertEquals(container.getWidth(), textView.getWidth());
        assertEquals(container.getHeight(), textView.getHeight());

        // change layout size of textView after wrap()
        textView.setLayoutParams(new FrameLayout.LayoutParams(123, 123));
        frameLayout.measure(MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 500, 500);
        assertTrue(textView.getWidth() == 123);
        assertTrue(textView.getHeight() == 123);
        assertEquals(container.getWidth(), textView.getWidth());
        assertEquals(container.getHeight(), textView.getHeight());
    }

    @Test
    public void testFixedSize() {
        FrameLayout frameLayout = new FrameLayout(mContext);
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new FrameLayout.LayoutParams(200, LayoutParams.WRAP_CONTENT));
        textView.setText("abc");
        ShadowOverlayContainer container = new ShadowOverlayContainer(mContext);
        container.initialize(true, true, true);
        container.wrap(textView);
        frameLayout.addView(container);
        frameLayout.measure(MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 500, 500);
        assertTrue(textView.getWidth() == 200);
        assertTrue(textView.getHeight() > 0);
        assertTrue(textView.getHeight() < 500);
        assertEquals(container.getWidth(), textView.getWidth());
        assertEquals(container.getHeight(), textView.getHeight());

        // change layout size of textView after wrap()
        textView.setLayoutParams(new FrameLayout.LayoutParams(123, 123));
        frameLayout.measure(MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 500, 500);
        assertTrue(textView.getWidth() == 123);
        assertTrue(textView.getHeight() == 123);
        assertEquals(container.getWidth(), textView.getWidth());
        assertEquals(container.getHeight(), textView.getHeight());
    }

    @Test
    public void testMatchParent() {
        FrameLayout frameLayout = new FrameLayout(mContext);
        TextView textView = new TextView(mContext);
        textView.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT));
        textView.setText("abc");
        ShadowOverlayContainer container = new ShadowOverlayContainer(mContext);
        container.initialize(true, true, true);
        container.wrap(textView);
        frameLayout.addView(container);
        frameLayout.measure(MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(500, MeasureSpec.EXACTLY));
        frameLayout.layout(0, 0, 500, 500);
        assertTrue(textView.getWidth() == 500);
        assertTrue(textView.getHeight() > 0);
        assertTrue(textView.getHeight() < 500);
        assertEquals(container.getWidth(), textView.getWidth());
        assertEquals(container.getHeight(), textView.getHeight());
    }
}
