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

import static org.junit.Assert.assertEquals;

import android.content.res.AssetManager;

import androidx.core.util.Pair;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

@SmallTest
public class FontFileUtilTest {

    @Test
    public void testRegularFonts() throws IOException {
        AssetManager am = InstrumentationRegistry.getTargetContext().getAssets();
        for (Pair<Integer, Boolean> style : FontTestUtil.getAllStyles()) {
            InputStream is = null;
            try {
                int weight = style.first.intValue();
                boolean italic = style.second.booleanValue();
                String path = FontTestUtil.getFontPathFromStyle(weight, italic);

                is = am.open(path);
                int packed = FontFileUtil.analyzeStyle(is, 0, null);
                assertEquals(path, weight, FontFileUtil.unpackWeight(packed));
                assertEquals(path, italic, FontFileUtil.unpackItalic(packed));
            } finally {
                is.close();
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testTtcFont() throws IOException {
        AssetManager am = InstrumentationRegistry.getTargetContext().getAssets();
        for (Pair<Integer, Boolean> style : FontTestUtil.getAllStyles()) {
            InputStream is = null;
            try {
                int weight = style.first.intValue();
                boolean italic = style.second.booleanValue();
                String path = FontTestUtil.getTtcFontFileInAsset();
                int ttcIndex = FontTestUtil.getTtcIndexFromStyle(weight, italic);

                is = am.open(path);
                int packed = FontFileUtil.analyzeStyle(is, ttcIndex, null);
                assertEquals(path + "#" + ttcIndex, weight, FontFileUtil.unpackWeight(packed));
                assertEquals(path + "#" + ttcIndex, italic, FontFileUtil.unpackItalic(packed));
            } finally {
                is.close();
            }
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = 26)
    public void testVariationFont() throws IOException {
        AssetManager am = InstrumentationRegistry.getTargetContext().getAssets();
        for (Pair<Integer, Boolean> style : FontTestUtil.getAllStyles()) {
            InputStream is = null;
            try {
                int weight = style.first.intValue();
                boolean italic = style.second.booleanValue();
                String path = FontTestUtil.getVFFontInAsset();
                String axes = FontTestUtil.getVarSettingsFromStyle(weight, italic);

                is = am.open(path);
                int packed = FontFileUtil.analyzeStyle(is, 0, axes);
                assertEquals(path + "#" + axes, weight, FontFileUtil.unpackWeight(packed));
                assertEquals(path + "#" + axes, italic, FontFileUtil.unpackItalic(packed));
            } finally {
                is.close();
            }
        }
    }
}
