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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@SmallTest
@RunWith(AndroidJUnit4.class)

public class ControlBarTest {

    @Test
    public void defaultFocus() {
        Context context = InstrumentationRegistry.getTargetContext();
        final ControlBar bar = new ControlBar(context, null);
        final TextView v1 = new Button(context);
        bar.addView(v1, 100, 100);
        final TextView v2 = new Button(context);
        bar.addView(v2, 100, 100);
        final TextView v3 = new Button(context);
        bar.addView(v3, 100, 100);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        bar.requestFocus(View.FOCUS_DOWN);
                    }
                }
        );
        assertTrue(v2.hasFocus());
    }

    @Test
    public void persistFocus() {
        Context context = InstrumentationRegistry.getTargetContext();
        final LinearLayout rootView = new LinearLayout(context);
        final ControlBar bar = new ControlBar(context, null);
        rootView.addView(bar, 800, 100);
        final Button barSibling = new Button(context);
        rootView.addView(barSibling, 100, 100);
        final TextView v1 = new Button(context);
        bar.addView(v1, 100, 100);
        final TextView v2 = new Button(context);
        bar.addView(v2, 100, 100);
        final TextView v3 = new Button(context);
        bar.addView(v3, 100, 100);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        v3.requestFocus(View.FOCUS_DOWN);
                    }
                }
        );
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        barSibling.requestFocus();
                    }
                }
        );
        assertFalse(bar.hasFocus());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        bar.requestFocus(View.FOCUS_RIGHT);
                    }
                }
        );
        assertTrue(v3.hasFocus());
    }

    @Test
    public void getFocusables() {
        Context context = InstrumentationRegistry.getTargetContext();
        final LinearLayout rootView = new LinearLayout(context);
        final ControlBar bar = new ControlBar(context, null);
        rootView.addView(bar, 800, 100);
        final Button barSibling = new Button(context);
        rootView.addView(barSibling, 100, 100);
        final TextView v1 = new Button(context);
        bar.addView(v1, 100, 100);
        final TextView v2 = new Button(context);
        bar.addView(v2, 100, 100);
        final TextView v3 = new Button(context);
        bar.addView(v3, 100, 100);

        ArrayList<View> focusables = new ArrayList();
        bar.addFocusables(focusables, View.FOCUS_DOWN);
        assertEquals(1, focusables.size());
        assertSame(focusables.get(0), v2);
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_UP);
        assertEquals(1, focusables.size());
        assertSame(focusables.get(0), v2);
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_LEFT);
        assertEquals(3, focusables.size());
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_RIGHT);
        assertEquals(3, focusables.size());

        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        v3.requestFocus(View.FOCUS_DOWN);
                    }
                }
        );
        InstrumentationRegistry.getInstrumentation().runOnMainSync(
                new Runnable() {
                    @Override
                    public void run() {
                        barSibling.requestFocus();
                    }
                }
        );
        assertFalse(bar.hasFocus());
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_DOWN);
        assertEquals(1, focusables.size());
        assertSame(focusables.get(0), v3);
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_UP);
        assertEquals(1, focusables.size());
        assertSame(focusables.get(0), v3);
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_LEFT);
        assertEquals(3, focusables.size());
        focusables.clear();
        bar.addFocusables(focusables, View.FOCUS_RIGHT);
        assertEquals(3, focusables.size());

    }
}
