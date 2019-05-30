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

package androidx.core.content.res;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Xml;

import androidx.core.test.R;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

@SmallTest
public class TypedArrayUtilsTest {
    // Resources ID generated in the latest R.java for framework.
    static final int[] STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY = {
            android.R.attr.name, android.R.attr.tint, android.R.attr.height,
            android.R.attr.width, android.R.attr.alpha, android.R.attr.autoMirrored,
            android.R.attr.tintMode, android.R.attr.viewportWidth, android.R.attr.viewportHeight
    };

    static final int STYLEABLE_VECTOR_DRAWABLE_TINT = 1;

    private Context mContext;
    private Resources mResources;

    @Before
    public void setup() {
        mContext = ApplicationProvider.getApplicationContext();
        mResources = mContext.getResources();
    }

    private ColorStateList getTintFromXml(int resId) throws XmlPullParserException, IOException {
        Resources.Theme theme = null;
        Resources res = mResources;

        XmlPullParser parser = res.getXml(resId);
        AttributeSet attrs = Xml.asAttributeSet(parser);

        // Seek to the <vector> element.
        int type;
        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT) {
            // Empty loop
        }
        assertEquals(XmlPullParser.START_TAG, type);
        assertEquals("vector", parser.getName());

        TypedArray a = TypedArrayUtils.obtainAttributes(res, theme, attrs,
                STYLEABLE_VECTOR_DRAWABLE_TYPE_ARRAY);

        // Load an XML parser for something that references a CSL XML resource. Make sure we can
        // resolve a CSL object out of the reference.
        return TypedArrayUtils.getNamedColorStateList(a, parser, theme, "tint",
                STYLEABLE_VECTOR_DRAWABLE_TINT);
    }

    @Test
    public void testGetNamedColorStateList() throws Exception {
        ColorStateList tint = getTintFromXml(R.drawable.heart_tint_csl);
        assertNotNull(tint);
        assertTrue(tint.isStateful());
    }

    @Test
    public void testGetNamedColorStateList_Upconvert() throws Exception {
        ColorStateList tint = getTintFromXml(R.drawable.heart_tint);
        assertNotNull(tint);
        assertFalse(tint.isStateful());
    }
}
