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

package androidx.pdf.data;

import static com.google.common.truth.Truth.assertThat;

import android.net.Uri;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Tests for {@link FileOpenable}.
 */
@RunWith(RobolectricTestRunner.class)
//TODO: Remove minsdk check after sdk extension 13 release
@Config(minSdk = Build.VERSION_CODES.VANILLA_ICE_CREAM)
public class FileOpenableTest {

    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String BASE_ASSET_PATH = "src/test/assets";

    @Test
    public void testFileUriOpenable() throws FileNotFoundException {
        File file = getTestFile("pdf/AcroJS.pdf");
        Uri uri = Uri.fromFile(file);
        FileOpenable original = new FileOpenable(uri);
        Openable clone = writeAndReadFromParcel(original, FileOpenable.CREATOR);
        assertThat(clone).isNotNull();
        assertThat(clone.getContentType()).isEqualTo(original.getContentType());
        assertThat(clone.length()).isEqualTo(original.length());
    }

    @Test
    public void testFileOpenable() throws FileNotFoundException {
        File file = getTestFile("pdf/AcroJS.pdf");
        FileOpenable original = new FileOpenable(file, PDF_MIME_TYPE);
        FileOpenable clone = writeAndReadFromParcel(original, FileOpenable.CREATOR);
        assertThat(clone).isNotNull();
        assertThat(clone.getContentType()).isEqualTo(original.getContentType());
        assertThat(clone.length()).isEqualTo(original.length());
    }

    private static <T extends Parcelable> T writeAndReadFromParcel(T openable, Creator<T> creator) {
        Parcel parcel = Parcel.obtain();
        openable.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        T clone = creator.createFromParcel(parcel);
        return clone;
    }

    private File getTestFile(String name) {
        return new File(BASE_ASSET_PATH, name);
    }
}

