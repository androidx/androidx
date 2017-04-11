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

package android.support.v4.graphics.fonts;

import static android.content.res.AssetManager.ACCESS_BUFFER;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.TestSupportActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Tests for {@link FontResult}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class FontResultTest {
    private static final int TTC_INDEX = 3;
    private static final String FONT_VARIATION_SETTINGS = "my_settings";
    private static final int BOLD_WEIGHT = 700;
    private static final String TEST_FONT_FILE = "samplefont.ttf";
    private Resources mResources;
    private Activity mActivity;
    private ParcelFileDescriptor mParcelFileDescriptor;

    @Rule
    public ActivityTestRule<TestSupportActivity> mActivityRule =
            new ActivityTestRule<>(TestSupportActivity.class);

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mResources = mActivity.getResources();
        mParcelFileDescriptor = loadFont();
    }

    @Test
    public void testWriteToParcel() throws IOException {
        // GIVEN a FontResult
        FontResult fontResult = new FontResult(mParcelFileDescriptor, TTC_INDEX,
                FONT_VARIATION_SETTINGS, BOLD_WEIGHT, true /* italic */);

        // WHEN we write it to a Parcel
        Parcel dest = Parcel.obtain();
        fontResult.writeToParcel(dest, 0);
        dest.setDataPosition(0);

        // THEN we create from that parcel and get the same values.
        FontResult result = FontResult.CREATOR.createFromParcel(dest);
        assertNotNull(result.getFileDescriptor());
        assertEquals(TTC_INDEX, result.getTtcIndex());
        assertEquals(FONT_VARIATION_SETTINGS, result.getFontVariationSettings());
        assertEquals(BOLD_WEIGHT, result.getWeight());
        assertTrue(result.getItalic());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullFileDescriptor() {
        new FontResult(null, TTC_INDEX, FONT_VARIATION_SETTINGS, BOLD_WEIGHT, false /* italic */);
    }

    @Test
    public void testConstructorWithNullFontVariationSettings() {
        // WHEN we create a result with a null fontVariationSettings
        FontResult fontResult = new FontResult(mParcelFileDescriptor, TTC_INDEX, null, BOLD_WEIGHT,
                false /* italic */);

        // THEN we expect no exception to be raised, and null to be stored as the value.
        assertNull(fontResult.getFontVariationSettings());
    }

    private ParcelFileDescriptor loadFont() {
        File cacheFile = null;
        try {
            cacheFile = new File(mActivity.getCacheDir(), TEST_FONT_FILE);
            cacheFile.getParentFile().mkdirs();
            copyToCacheFile("samplefont.ttf", cacheFile);
            return ParcelFileDescriptor.open(cacheFile, MODE_READ_ONLY);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (cacheFile != null) {
                cacheFile.delete();
            }
        }
        return null;
    }

    private void copyToCacheFile(final String assetPath, final File cacheFile)
            throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = mResources.getAssets().open(assetPath, ACCESS_BUFFER);
            fos = new FileOutputStream(cacheFile, false);
            byte[] buffer = new byte[1024];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                fos.write(buffer, 0, readLen);
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (fos != null) {
                fos.close();
            }
        }
    }
}
