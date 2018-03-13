/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.tvprovider.media.tv;

import static android.support.test.InstrumentationRegistry.getContext;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.SystemClock;
import android.support.test.filters.MediumTest;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;

import androidx.tvprovider.test.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@Suppress // Test is failing b/70905391
@MediumTest
@RunWith(AndroidJUnit4.class)
public class ChannelLogoUtilsTest {
    private static final String FAKE_INPUT_ID = "ChannelLogoUtils.test";

    private ContentResolver mContentResolver;
    private Uri mChannelUri;
    private long mChannelId;

    @Before
    public void setUp() throws Exception {
        mContentResolver = getContext().getContentResolver();
        ContentValues contentValues = new Channel.Builder()
                .setInputId(FAKE_INPUT_ID)
                .setType(TvContractCompat.Channels.TYPE_OTHER).build().toContentValues();
        mChannelUri = mContentResolver.insert(TvContract.Channels.CONTENT_URI, contentValues);
        mChannelId = ContentUris.parseId(mChannelUri);
    }

    @After
    public void tearDown() throws Exception {
        mContentResolver.delete(mChannelUri, null, null);
    }

    @Test
    public void testStoreChannelLogo_fromBitmap() {
        assertNull(ChannelLogoUtils.loadChannelLogo(getContext(), mChannelId));
        Bitmap logo = BitmapFactory.decodeResource(getContext().getResources(),
                R.drawable.test_icon);
        assertNotNull(logo);
        assertTrue(ChannelLogoUtils.storeChannelLogo(getContext(), mChannelId, logo));
        // Workaround: the file status is not consistent between openInputStream/openOutputStream,
        // wait 10 secs to make sure that the logo file is written into the disk.
        SystemClock.sleep(10000);
        assertNotNull(ChannelLogoUtils.loadChannelLogo(getContext(), mChannelId));
    }

    @Test
    public void testStoreChannelLogo_fromResUri() {
        assertNull(ChannelLogoUtils.loadChannelLogo(getContext(), mChannelId));
        int resId = R.drawable.test_icon;
        Resources res = getContext().getResources();
        Uri logoUri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(res.getResourcePackageName(resId))
                .appendPath(res.getResourceTypeName(resId))
                .appendPath(res.getResourceEntryName(resId))
                .build();
        assertTrue(ChannelLogoUtils.storeChannelLogo(getContext(), mChannelId, logoUri));
        // Workaround: the file status is not consistent between openInputStream/openOutputStream,
        // wait 10 secs to make sure that the logo file is written into the disk.
        SystemClock.sleep(10000);
        assertNotNull(ChannelLogoUtils.loadChannelLogo(getContext(), mChannelId));
    }
}
