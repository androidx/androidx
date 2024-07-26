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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;

import androidx.pdf.models.Dimensions;
import androidx.pdf.util.ObservableValue;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.pdf.widget.ZoomView;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@SuppressWarnings("unchecked")
@SmallTest
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class PageViewFactoryTest {
    private final PdfLoader mMockPdfLoader = mock(PdfLoader.class);

    private final PaginatedView mMockPaginatedView = mock(PaginatedView.class);

    private final ZoomView mMockZoomView = mock(ZoomView.class);

    private final SingleTapHandler mMockSingleTapHandler = mock(SingleTapHandler.class);

    @Before
    public void setup() {
        when(mMockZoomView.zoomScroll()).thenReturn(mock(ObservableValue.class));
    }

    @Test
    public void getOrCreatePageView_accessibilityDisabled_newAndSetupPageViewMosaicView() {
        // Arrange
        int dummyPageElevation = 100;
        int dummyWidth = 800;
        int dummyHeight = 800;
        Dimensions mockDimensions = new Dimensions(dummyWidth, dummyHeight);
        ArgumentCaptor<PageViewFactory.PageView> pageViewArgumentCaptor = ArgumentCaptor.forClass(
                PageViewFactory.PageView.class);
        PageViewFactory mockPageViewFactory = new MockPageViewAccessbilityDisabledFactory(
                ApplicationProvider.getApplicationContext(), mMockPdfLoader, mMockPaginatedView,
                mMockZoomView, mMockSingleTapHandler
        );

        // Act
        mockPageViewFactory.getOrCreatePageView(0, dummyPageElevation, mockDimensions);

        // Assert
        verify(mMockPaginatedView).addView(pageViewArgumentCaptor.capture());
        assertThat(pageViewArgumentCaptor.getValue().getClass().getSimpleName()).isEqualTo(
                PageMosaicView.class.getSimpleName());
        View view = pageViewArgumentCaptor.getValue().asView();

        assertThat(view.getBackground().getClass().getSimpleName()).isEqualTo(
                ColorDrawable.class.getSimpleName());
        assertThat(((ColorDrawable) view.getBackground()).getColor()).isEqualTo(Color.WHITE);
        assertThat(view.getElevation()).isEqualTo(dummyPageElevation);
        assertThat(((PageMosaicView) pageViewArgumentCaptor.getValue()).getBounds()).isEqualTo(
                new Rect(0, 0, dummyWidth, dummyHeight));
    }

    @Test
    public void getOrCreatePageView_accessibilityEnabled_newAccessibilityAndSetupPageViewWrapper() {
        // Arrange
        int dummyPageElevation = 100;
        int dummyWidth = 800;
        int dummyHeight = 800;
        Dimensions mockDimensions = new Dimensions(dummyWidth, dummyHeight);
        ArgumentCaptor<PageViewFactory.PageView> pageViewArgumentCaptor = ArgumentCaptor.forClass(
                PageViewFactory.PageView.class);
        PageViewFactory mockPageViewFactory = new MockPageViewAccessbilityEnabledFactory(
                ApplicationProvider.getApplicationContext(), mMockPdfLoader, mMockPaginatedView,
                mMockZoomView, mMockSingleTapHandler
        );

        // Act
        mockPageViewFactory.getOrCreatePageView(0, dummyPageElevation, mockDimensions);

        // Assert
        verify(mMockPaginatedView).addView(pageViewArgumentCaptor.capture());
        assertThat(pageViewArgumentCaptor.getValue().getClass().getSimpleName()).isEqualTo(
                AccessibilityPageWrapper.class.getSimpleName());

        PageMosaicView pageMosaicView =
                pageViewArgumentCaptor.getValue().getPageView();
        assertThat(pageMosaicView.getBackground().getClass().getSimpleName()).isEqualTo(
                ColorDrawable.class.getSimpleName());
        assertThat(((ColorDrawable) pageMosaicView.getBackground()).getColor()).isEqualTo(
                Color.WHITE);
        assertThat(pageMosaicView.getElevation()).isEqualTo(dummyPageElevation);
        assertThat(pageMosaicView.getBounds()).isEqualTo(
                new Rect(0, 0, dummyWidth, dummyHeight));
    }
}
