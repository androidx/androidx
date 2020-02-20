/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.autofill;

import static org.junit.Assert.assertEquals;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for InlineSuggestionHostView
 */
@MediumTest
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29) // Needed only on 29 and above
public class InlineSuggestionHostViewTest {

    @Rule
    @NonNull
    public final ActivityTestRule<InlineContentActivity> mActivityTestRule =
            new ActivityTestRule<>(InlineContentActivity.class);

    @Test
    public void testInlinedSuggestionOffset() throws Exception {
        final ViewGroup suggestionsView = mActivityTestRule.getActivity()
                .findViewById(androidx.autofill.test.R.id.suggestions);

        // Create the suggestion view
        final CountDownLatch surfaceLatch = new CountDownLatch(1);
        final SurfaceView suggestionView = new SurfaceView(InstrumentationRegistry
                .getInstrumentation().getContext());
        suggestionView.setLayoutParams(new ViewGroup.LayoutParams(250, 50));
        suggestionView.setZOrderOnTop(true);
        suggestionView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                holder.setFormat(PixelFormat.RGBA_8888);
                final Canvas canvas = holder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                holder.unlockCanvasAndPost(canvas);
                surfaceLatch.countDown();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                    int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {}
        });

        // Add the suggestion view
        final ViewGroup suggestionHost = suggestionsView.findViewById(
                androidx.autofill.test.R.id.suggestion_host);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                suggestionHost.addView(suggestionView)
        );

        surfaceLatch.await(5, TimeUnit.SECONDS);

        // Make sure suggestion surface properly placed
        final Bitmap beforeScrollScreenshot = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().takeScreenshot();
        assertEquals(Color.WHITE, beforeScrollScreenshot.getColor(25, 25).toArgb());
        assertEquals(Color.BLACK, beforeScrollScreenshot.getColor(100, 25).toArgb());

        // Scroll the suggestion area
        final HorizontalScrollView scrollView = suggestionsView.findViewById(
                androidx.autofill.test.R.id.scroll_view);
        final CountDownLatch scrollLatch = new CountDownLatch(1);
        scrollView.getViewTreeObserver().addOnDrawListener(scrollLatch::countDown);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                scrollView.scrollBy(50, 0)
        );
        scrollLatch.await(5, TimeUnit.SECONDS);

        final Bitmap afterScrollScreenshot = InstrumentationRegistry.getInstrumentation()
                .getUiAutomation().takeScreenshot();

        // Make sure suggestion surface properly clipped
        assertEquals(Color.WHITE, afterScrollScreenshot.getColor(25, 25).toArgb());
    }
}
