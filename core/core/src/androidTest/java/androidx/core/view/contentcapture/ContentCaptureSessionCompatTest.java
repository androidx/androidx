/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.view.contentcapture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.support.v4.BaseInstrumentationTestCase;
import android.view.View;
import android.view.ViewStructure;
import android.view.autofill.AutofillId;
import android.view.contentcapture.ContentCaptureSession;

import androidx.core.view.ViewCompatActivity;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
@SmallTest
@SdkSuppress(minSdkVersion = 29)
public class ContentCaptureSessionCompatTest extends
        BaseInstrumentationTestCase<ViewCompatActivity> {

    private View mView;

    public ContentCaptureSessionCompatTest() {
        super(ViewCompatActivity.class);
    }

    @Before
    public void setUp() {
        final Activity activity = mActivityTestRule.getActivity();
        mView = activity.findViewById(androidx.core.test.R.id.view);
    }

    @Test
    public void testToContentCaptureSession_returnsContentCaptureSession() {
        ContentCaptureSession mockContentCaptureSession = mock(ContentCaptureSession.class);
        ContentCaptureSessionCompat contentCaptureSessionCompat =
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        mockContentCaptureSession, mView);

        ContentCaptureSession result = contentCaptureSessionCompat.toContentCaptureSession();

        assertEquals(mockContentCaptureSession, result);
    }

    @Test
    public void testNewAutofillId_returnsAutofillIdAboveSDK29() {
        ContentCaptureSession mockContentCaptureSession = mock(ContentCaptureSession.class);
        AutofillId mockAutofillId = mock(AutofillId.class);
        when(mockContentCaptureSession.newAutofillId(any(), anyLong())).thenReturn(mockAutofillId);
        ContentCaptureSessionCompat contentCaptureSessionCompat =
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        mockContentCaptureSession, mView);

        assertEquals(mockAutofillId, contentCaptureSessionCompat.newAutofillId(1L));
    }

    @Test
    public void testNewVirtualViewStructure_returnsViewStructureAboveSDK29() {
        ContentCaptureSession mockContentCaptureSession = mock(ContentCaptureSession.class);
        AutofillId mockAutofillId = mock(AutofillId.class);
        ContentCaptureSessionCompat contentCaptureSessionCompat =
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        mockContentCaptureSession, mView);

        Object result = contentCaptureSessionCompat.newVirtualViewStructure(mockAutofillId, 1L);

        assertNotNull(result);
    }

    @Test
    @SdkSuppress(minSdkVersion = 29, maxSdkVersion = 33)
    public void testNotifyViewsAppeared_throwsNPEBetweenSDK29And33() {
        ContentCaptureSession mockContentCaptureSession = mock(ContentCaptureSession.class);
        ViewStructure mockViewStructure = mock(ViewStructure.class);
        List<ViewStructure> viewStructures = new ArrayList<>();
        viewStructures.add(mockViewStructure);
        ContentCaptureSessionCompat contentCaptureSessionCompat =
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        mockContentCaptureSession, mView);

        // Some final methods in the mock object throw NPE.
        assertThrows(NullPointerException.class,
                () -> contentCaptureSessionCompat.notifyViewsAppeared(viewStructures));
    }

    @Test
    public void testNotifyViewsDisappeared_throwsNPEAboveSDK29() {
        ContentCaptureSession mockContentCaptureSession = mock(ContentCaptureSession.class);
        long[] ids = {1L};
        ContentCaptureSessionCompat contentCaptureSessionCompat =
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        mockContentCaptureSession, mView);

        // Some final methods in the mock object throw NPE.
        assertThrows(NullPointerException.class,
                () -> contentCaptureSessionCompat.notifyViewsDisappeared(ids));
    }

    @Test
    public void testNotifyViewTextChanged_throwsNPEAboveSDK29() {
        ContentCaptureSession mockContentCaptureSession = mock(ContentCaptureSession.class);
        ContentCaptureSessionCompat contentCaptureSessionCompat =
                ContentCaptureSessionCompat.toContentCaptureSessionCompat(
                        mockContentCaptureSession, mView);
        AutofillId mockAutofillId = mock(AutofillId.class);

        // Some final methods in the mock object throw NPE.
        assertThrows(NullPointerException.class,
                () -> contentCaptureSessionCompat.notifyViewTextChanged(mockAutofillId, "test"));
    }
}
