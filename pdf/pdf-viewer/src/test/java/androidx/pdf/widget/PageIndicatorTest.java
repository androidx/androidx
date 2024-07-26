/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.widget;


import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.os.Build;
import android.widget.TextView;

import androidx.pdf.data.Range;
import androidx.pdf.util.Accessibility;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link PageIndicator}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class PageIndicatorTest {
    @Mock
    Accessibility mAccessibility;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private final TextView mPageNumberView = new TextView(mContext);
    private PageIndicator mPageIndicator;

    private AutoCloseable mOpenMocks;

    @Before
    public void setUp() {
        mOpenMocks = MockitoAnnotations.openMocks(this);
        when(mAccessibility.isAccessibilityEnabled(isA(Context.class))).thenReturn(true);

        mPageIndicator = new PageIndicator(mContext, mPageNumberView, mAccessibility);
        mPageIndicator.setNumPages(10);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }


    @Test
    public void testSetRangeAndZoom_whenFirstCall_returnsFalse() {
        assertThat(mPageIndicator.setRangeAndZoom(new Range(0, 1), 1.5f, false)).isFalse();
    }

    @Test
    public void testSetRangeAndZoom_whenRangeIsTheSame_returnsFalse() {
        mPageIndicator.setRangeAndZoom(new Range(0, 1), 1.5f, false);
        assertThat(mPageIndicator.setRangeAndZoom(new Range(0, 1), 2.5f, true)).isFalse();
    }

    @Test
    public void testSetRangeAndZoom_whenRangeIsTheDifferent_returnsTrue() {
        mPageIndicator.setRangeAndZoom(new Range(0, 1), 1.5f, false);
        assertThat(mPageIndicator.setRangeAndZoom(new Range(0, 3), 1.5f, false)).isTrue();
    }

    @Test
    public void testAnnouncePageChanges() {
        mPageIndicator.setRangeAndZoom(new Range(0, 1), 1.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 1), 1.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(1, 1), 1.5f, false);
        verify(mAccessibility).announce(mContext, mPageNumberView, "page 2 of 10");

        mPageIndicator.setRangeAndZoom(new Range(1, 1), 1.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(1, 1), 1.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(1, 2), 1.5f, false);
        verify(mAccessibility).announce(mContext, mPageNumberView, "pages 2 to 3 of 10");

        mPageIndicator.setRangeAndZoom(new Range(1, 2), 1.5f, true);
    }

    @Test
    public void testAnnounceZoomChanges() {
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 1.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 2.0f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 2.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 3.0f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 3.5f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 4.0f, false);
        mPageIndicator.setRangeAndZoom(new Range(0, 0), 4.0f, true);
        verify(mAccessibility).announce(mContext, mPageNumberView,
                String.format("%s\n%s", "page 1 of 10", "zoom 400 percent"));
    }
}
