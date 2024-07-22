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

package androidx.pdf.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

import java.io.File;

@SmallTest
@RunWith(RobolectricTestRunner.class)
public class AnnotationUtilsTest {
    @Test
    public void getAnnotationIntent_nonNullUri_returnsAnnotateActionIntent() {
        String fileName = "file:://dummyfile.pdf";
        Uri uri = Uri.fromFile(new File(fileName));
        Intent annotateIntent = AnnotationUtils.getAnnotationIntent(uri);

        assertThat(annotateIntent.getAction()).isEqualTo(AnnotationUtils.ACTION_ANNOTATE_PDF);
        assertThat(annotateIntent.getCategories()).hasSize(1);
        assertThat(annotateIntent.getCategories()).contains(Intent.CATEGORY_DEFAULT);
        assertThat(annotateIntent.getFlags()).isEqualTo(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        assertThat(annotateIntent.getData()).isEqualTo(uri);
        assertThat(annotateIntent.getType()).isEqualTo(AnnotationUtils.PDF_MIME_TYPE);
    }

    @Test
    public void getAnnotationIntent_nullUri_throwsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> AnnotationUtils.getAnnotationIntent(null));
    }

    @Test
    public void resolveAnnotationIntent_nonNullUri_returnsTrue() {
        String fileName = "file:://dummyfile.pdf";
        Uri uri = Uri.fromFile(new File(fileName));

        Context mockContext = Mockito.mock(Context.class);
        PackageManager mockPackageManager = Mockito.mock(PackageManager.class);
        ResolveInfo mockResolveInfo = Mockito.mock(ResolveInfo.class);
        ActivityInfo mockActivityInfo = new ActivityInfo();
        ApplicationInfo mockApplicationInfo = new ApplicationInfo();
        mockApplicationInfo.packageName = "dummyPackage";
        mockActivityInfo.applicationInfo = mockApplicationInfo;
        mockActivityInfo.name = "dummyName";
        mockResolveInfo.activityInfo = mockActivityInfo;

        when(mockPackageManager.resolveActivity(any(Intent.class),
                eq(PackageManager.MATCH_DEFAULT_ONLY))).thenReturn(
                mockResolveInfo);
        when(mockContext.getPackageManager()).thenReturn(mockPackageManager);

        boolean result = AnnotationUtils.resolveAnnotationIntent(mockContext, uri);
        assertTrue(result);
    }

    @Test
    public void resolveAnnotationIntent_nullContext_returnsNullPointerException() {
        assertThrows(NullPointerException.class,
                () -> AnnotationUtils.resolveAnnotationIntent(
                        ApplicationProvider.getApplicationContext(), null));
    }

    @Test
    public void resolveAnnotationIntent_nullUri_returnsNullPointerException() {
        String fileName = "file:://dummyfile.pdf";
        Uri uri = Uri.fromFile(new File(fileName));
        assertThrows(NullPointerException.class,
                () -> AnnotationUtils.resolveAnnotationIntent(
                        null, uri));
    }
}
