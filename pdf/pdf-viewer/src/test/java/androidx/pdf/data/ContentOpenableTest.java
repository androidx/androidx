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
import android.os.Parcel;

import androidx.pdf.models.Dimensions;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link ContentOpenable}. */
@SmallTest
@RunWith(RobolectricTestRunner.class)
public class ContentOpenableTest {

    private ContentOpenable mContentOpenable;
    private final Uri mUri = Uri.parse("content://com.google.android.apps.test");
    private final String mType = "application/pdf";
    private final Dimensions mSize = new Dimensions(32, 43);

    @Test
    public void testParcellingWithOnlyUri() {
        mContentOpenable = new ContentOpenable(mUri);
        Parcel parcel = Parcel.obtain();
        mContentOpenable.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        ContentOpenable result = ContentOpenable.CREATOR.createFromParcel(parcel);
        assertThat(result.toString()).isEqualTo(mContentOpenable.toString());
    }

    @Test
    public void testParcellingWithUriAndType() {
        mContentOpenable = new ContentOpenable(mUri, mType);
        Parcel parcel = Parcel.obtain();
        mContentOpenable.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        ContentOpenable result = ContentOpenable.CREATOR.createFromParcel(parcel);
        assertThat(result.toString()).isEqualTo(mContentOpenable.toString());
    }

    @Test
    public void testParcellingWithUriAndSize() {
        mContentOpenable = new ContentOpenable(mUri, mSize);
        Parcel parcel = Parcel.obtain();
        mContentOpenable.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        ContentOpenable result = ContentOpenable.CREATOR.createFromParcel(parcel);
        assertThat(result.toString()).isEqualTo(mContentOpenable.toString());
    }
}
