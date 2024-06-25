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

package androidx.pdf.viewer;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.pdf.models.Dimensions;
import androidx.pdf.util.BitmapRecycler;
import androidx.pdf.util.MockDrawable;
import androidx.pdf.widget.MosaicView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/** Unit tests for {@link PageMosaicView}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class PageMosaicViewTest {
    @Mock
    private MosaicView.BitmapSource mMockBitmapSource;

    @Mock
    private BitmapRecycler mMockBitmapRecycler;

    @Before
    public void setup() {

    }

    @Test
    public void newPageMosaicView_pageTextIsNull() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler);

        assertThat(pageMosaicView.getPageText() == null).isTrue();
    }

    @Test
    public void setOverlay_nonNull_addsNewOverlayForSearchKey() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler);

        pageMosaicView.setOverlay(new MockDrawable());
        assertThat(pageMosaicView.hasOverlay()).isTrue();
    }

    @Test
    public void setOverlay_nullOverlay_removesOverlayForSearchKey() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler);

        pageMosaicView.setOverlay(new MockDrawable());
        assertThat(pageMosaicView.hasOverlay()).isTrue();

        pageMosaicView.setOverlay(null);
        assertThat(pageMosaicView.hasOverlay()).isFalse();
    }

    @Test
    public void setPageText_nonNullPageText_setsPageTextAndContentDescription() {
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler) {
            @Override
            @NonNull
            protected String buildContentDescription(@Nullable String pageText, int pageNum) {
                return (pageText != null) ? pageText : "dummyString";
            }
        };

        String pageText = "dummyPageText";
        pageMosaicView.setPageText(pageText);
        assertThat(pageMosaicView.getPageText()).isEqualTo(pageText);
        assertThat(pageMosaicView.getContentDescription()).isEqualTo(pageText);
    }

    @Test
    public void setPageText_nullPageText_nullPageTextAndNonNullContentDescription() {
        String dummyContentDescription = "dummyContentDescription";
        Dimensions dimensions = new Dimensions(800, 800);
        PageMosaicView pageMosaicView = new PageMosaicView(
                ApplicationProvider.getApplicationContext(), /* pageNum= */ 0, dimensions,
                mMockBitmapSource, mMockBitmapRecycler) {
            @Override
            @NonNull
            protected String buildContentDescription(@Nullable String pageText, int pageNum) {
                return (pageText != null) ? pageText : dummyContentDescription;
            }
        };

        pageMosaicView.setPageText(null);
        assertThat(pageMosaicView.getPageText() == null).isTrue();
        assertThat(pageMosaicView.getContentDescription()).isEqualTo(dummyContentDescription);
    }
}
