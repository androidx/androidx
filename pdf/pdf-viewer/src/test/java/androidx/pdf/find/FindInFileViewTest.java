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

import android.net.Uri;

import androidx.pdf.viewer.PaginatedView;
import androidx.pdf.viewer.PaginationModel;
import androidx.pdf.viewer.loader.PdfLoader;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import junit.framework.TestCase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.robolectric.RobolectricTestRunner;

/**
 * Unit tests for {@link FindInFileView}
 */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class FindInFileViewTest extends TestCase {
    @Mock
    private PdfLoader mPdfLoader;
    @Mock
    private Uri mUri;
    @Mock
    private PaginatedView mPaginatedView;
    @Mock
    private PaginationModel mPaginationModel;
    private FindInFileView mFindInFileView;

    @Before
    public void setUp() throws Exception {
        mFindInFileView = new FindInFileView(ApplicationProvider.getApplicationContext());
        mFindInFileView.setPdfLoader(mPdfLoader);
        mFindInFileView.setPaginatedView(mPaginatedView);
        mFindInFileView.setFileUri(mUri);
    }

    @Test
    public void testSetFindInFileView_visibilityTrue() {
        mFindInFileView.setFindInFileView(true);
        assertThat(mFindInFileView.getVisibility()).isEqualTo(VISIBLE);
    }

    @Test
    public void testSetFindInFileView_visibilityFalse() {
        mFindInFileView.setFindInFileView(false);
        assertThat(mFindInFileView.getVisibility()).isEqualTo(GONE);
    }
}
