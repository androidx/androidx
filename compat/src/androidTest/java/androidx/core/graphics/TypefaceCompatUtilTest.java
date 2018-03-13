/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.core.graphics;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;

import androidx.annotation.RequiresApi;
import androidx.core.provider.MockFontProvider;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TypefaceCompatUtilTest {

    public Context mContext;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }

    @Test
    @RequiresApi(19)
    public void testMmapNullPfd() {
        if (Build.VERSION.SDK_INT < 19) {
            // The API tested here requires SDK level 19.
            return;
        }
        final Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT)
                .authority(MockFontProvider.AUTHORITY).build();
        final Uri fileUri = ContentUris.withAppendedId(uri, MockFontProvider.INVALID_FONT_FILE_ID);
        // Should not crash.
        TypefaceCompatUtil.mmap(mContext, null, fileUri);
    }
}
