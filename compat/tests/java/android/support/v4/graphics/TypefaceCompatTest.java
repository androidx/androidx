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

package android.support.v4.graphics;

import static android.content.res.AssetManager.ACCESS_BUFFER;

import static org.junit.Assert.assertNotNull;

import android.graphics.Typeface;
import android.os.ParcelFileDescriptor;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.BaseInstrumentationTestCase;
import android.support.v4.app.TestSupportActivity;
import android.support.v4.graphics.fonts.FontResult;
import android.util.Base64;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for {@link TypefaceCompatBaseImpl}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TypefaceCompatTest extends BaseInstrumentationTestCase<TestSupportActivity> {
    private static final String TEST_FONT_FILE = "fonts/samplefont1.ttf";
    private static final String CACHE_FILE = "cachedfont.ttf";
    private static final String PROVIDER = "com.test.fontprovider.authority";
    private static final String QUERY_CACHED = "query_cached";
    private static final String QUERY = "query";
    private static final String PACKAGE = "com.test.fontprovider.package";
    private static final byte[] BYTE_ARRAY =
            Base64.decode("e04fd020ea3a6910a2d808002b30", Base64.DEFAULT);
    private static final List<List<byte[]>> CERTS = Arrays.asList(Arrays.asList(BYTE_ARRAY));

    private TypefaceCompatBaseImpl mCompat;

    public TypefaceCompatTest() {
        super(TestSupportActivity.class);
    }

    @Before
    public void setup() {
        mCompat = new TypefaceCompatBaseImpl(mActivityTestRule.getActivity());
        TypefaceCompatBaseImpl.putInCache(PROVIDER, QUERY_CACHED, Typeface.MONOSPACE);
    }

    private File loadFont() {
        File cacheFile = new File(mActivityTestRule.getActivity().getCacheDir(), CACHE_FILE);
        try {
            copyToCacheFile(TEST_FONT_FILE, cacheFile);
            return cacheFile;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void copyToCacheFile(final String assetPath, final File cacheFile)
            throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            is = mActivityTestRule.getActivity().getAssets().open(assetPath, ACCESS_BUFFER);
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

    // TODO(nona): Remove once EmojiCompat stop using Typeface.createTypeface.
    @Test
    public void testCreateTypeface() throws IOException, InterruptedException {
        File file = loadFont();
        ParcelFileDescriptor pfd =
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        try {
            FontResult result = new FontResult(pfd, 0, null, 400, false /* italic */);
            Typeface typeface = mCompat.createTypeface(Arrays.asList(result));

            assertNotNull(typeface);
        } finally {
            if (file != null) {
                file.delete();
            }
            if (pfd != null) {
                pfd.close();
            }
        }
    }
}
