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

package androidx.pdf.find;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;

import android.os.Build;

import androidx.pdf.viewer.PaginatedView;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Unit tests for {@link FindInFileView}
 */
@SmallTest
@RunWith(RobolectricTestRunner.class)
// TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class FindInFileViewTest extends TestCase {
    @Mock
    private PdfLoader mPdfLoader;
    @Mock
    private PaginatedView mPaginatedView;
    @Mock
    private FloatingActionButton mAnnotationButton;
    private FindInFileView mFindInFileView;
    private AutoCloseable mOpenMocks;

    @Before
    public void setUp() throws Exception {
        mOpenMocks = MockitoAnnotations.openMocks(this);
        mFindInFileView = new FindInFileView(ApplicationProvider.getApplicationContext());
        mFindInFileView.setPdfLoader(mPdfLoader);
        mFindInFileView.setPaginatedView(mPaginatedView);
        mFindInFileView.setAnnotationButton(mAnnotationButton);
    }

    @After
    public void tearDown() throws Exception {
        mOpenMocks.close();
    }

    @Test
    public void testSetFindInFileView_visibilityTrue() {
        doNothing().when(mAnnotationButton).setVisibility(anyInt());
        mFindInFileView.setFindInFileView(true);
        assertThat(mFindInFileView.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void testSetFindInFileView_visibilityFalse() {
        mFindInFileView.setFindInFileView(false);
        assertThat(mFindInFileView.getVisibility()).isEqualTo(GONE);
    }
}
