/*
 * Copyright (C) 2016 The Android Open Source Project
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
package androidx.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParallaxIntTest {

    Parallax<Parallax.IntProperty> mSource;
    int mScreenMax;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double) expected, (double) actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
        mSource = new Parallax<Parallax.IntProperty>() {

            @Override
            public float getMaxValue() {
                return mScreenMax;
            }

            @Override
            public IntProperty createProperty(String name, int index) {
                return new IntProperty(name, index);
            }
        };
    }

    @Test
    public void testVariable() {
        mScreenMax = 1080;
        Parallax.IntProperty var1 = mSource.addProperty("var1");
        var1.setValue(mSource, 54);
        assertEquals((int) 54, var1.getValue(mSource));
        assertEquals(var1.getName(), "var1");
        var1.set(mSource, (int) 2000);
        assertEquals((int) 2000, var1.get(mSource).intValue());
    }

    @Test
    public void testFixedKeyValue() {
        mScreenMax = 1080;
        Parallax.IntProperty var1 = mSource.addProperty("var1");

        Parallax.IntPropertyMarkerValue keyValue = (Parallax.IntPropertyMarkerValue)
                var1.atAbsolute(1000);
        assertSame(keyValue.getProperty(), var1);
        assertEquals((int) 1000, keyValue.getMarkerValue(mSource));
    }

    @Test
    public void testFractionOfKeyValue() {
        mScreenMax = 1080;
        Parallax.IntProperty var1 = mSource.addProperty("var1");

        Parallax.IntPropertyMarkerValue keyValue = (Parallax.IntPropertyMarkerValue)
                var1.at(0, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertEquals((int) 540, keyValue.getMarkerValue(mSource));
    }

    @Test
    public void testFixedKeyValueWithFraction() {
        mScreenMax = 1080;
        Parallax.IntProperty var1 = mSource.addProperty("var1");

        Parallax.IntPropertyMarkerValue keyValue = (Parallax.IntPropertyMarkerValue)
                var1.at(-100, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertEquals((int) 440, keyValue.getMarkerValue(mSource));

        Parallax.IntPropertyMarkerValue keyValue2 = (Parallax.IntPropertyMarkerValue)
                var1.at(100, 0.5f);
        assertSame(keyValue2.getProperty(), var1);
        assertEquals((int) 640, keyValue2.getMarkerValue(mSource));
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyIntPropertys_wrongOrder() {
        Parallax.IntProperty var1 = mSource.addProperty("var1");
        Parallax.IntProperty var2 = mSource.addProperty("var2");

        var1.setValue(mSource, (int) 500);
        var2.setValue(mSource, (int) 499);

        mSource.verifyIntProperties();
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyIntPropertysWrong_combination() {
        Parallax.IntProperty var1 = mSource.addProperty("var1");
        Parallax.IntProperty var2 = mSource.addProperty("var2");

        var1.setValue(mSource, Parallax.IntProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, Parallax.IntProperty.UNKNOWN_AFTER);

        mSource.verifyIntProperties();
    }

    @Test
    public void testVerifyIntPropertys_success() {
        Parallax.IntProperty var1 = mSource.addProperty("var1");
        Parallax.IntProperty var2 = mSource.addProperty("var2");

        var1.setValue(mSource, (int) 499);
        var2.setValue(mSource, (int) 500);

        mSource.verifyIntProperties();

        var1.setValue(mSource, Parallax.IntProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, (int) 500);

        mSource.verifyIntProperties();

        var1.setValue(mSource, (int) 499);
        var2.setValue(mSource, Parallax.IntProperty.UNKNOWN_AFTER);

        mSource.verifyIntProperties();
    }
}
