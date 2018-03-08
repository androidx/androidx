// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from ParallaxIntTest.java.  DO NOT MODIFY. */

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
public class ParallaxFloatTest {

    Parallax<Parallax.FloatProperty> mSource;
    int mScreenMax;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double) expected, (double) actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
        mSource = new Parallax<Parallax.FloatProperty>() {

            @Override
            public float getMaxValue() {
                return mScreenMax;
            }

            @Override
            public FloatProperty createProperty(String name, int index) {
                return new FloatProperty(name, index);
            }
        };
    }

    @Test
    public void testVariable() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");
        var1.setValue(mSource, 54);
        assertFloatEquals((float) 54, var1.getValue(mSource));
        assertEquals(var1.getName(), "var1");
        var1.set(mSource, (float) 2000);
        assertFloatEquals((float) 2000, var1.get(mSource).floatValue());
    }

    @Test
    public void testFixedKeyValue() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");

        Parallax.FloatPropertyMarkerValue keyValue = (Parallax.FloatPropertyMarkerValue)
                var1.atAbsolute(1000);
        assertSame(keyValue.getProperty(), var1);
        assertFloatEquals((float) 1000, keyValue.getMarkerValue(mSource));
    }

    @Test
    public void testFractionOfKeyValue() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");

        Parallax.FloatPropertyMarkerValue keyValue = (Parallax.FloatPropertyMarkerValue)
                var1.at(0, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertFloatEquals((float) 540, keyValue.getMarkerValue(mSource));
    }

    @Test
    public void testFixedKeyValueWithFraction() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");

        Parallax.FloatPropertyMarkerValue keyValue = (Parallax.FloatPropertyMarkerValue)
                var1.at(-100, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertFloatEquals((float) 440, keyValue.getMarkerValue(mSource));

        Parallax.FloatPropertyMarkerValue keyValue2 = (Parallax.FloatPropertyMarkerValue)
                var1.at(100, 0.5f);
        assertSame(keyValue2.getProperty(), var1);
        assertFloatEquals((float) 640, keyValue2.getMarkerValue(mSource));
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyFloatPropertys_wrongOrder() {
        Parallax.FloatProperty var1 = mSource.addProperty("var1");
        Parallax.FloatProperty var2 = mSource.addProperty("var2");

        var1.setValue(mSource, (float) 500);
        var2.setValue(mSource, (float) 499);

        mSource.verifyFloatProperties();
    }

    @Test(expected = IllegalStateException.class)
    public void testVerifyFloatPropertysWrong_combination() {
        Parallax.FloatProperty var1 = mSource.addProperty("var1");
        Parallax.FloatProperty var2 = mSource.addProperty("var2");

        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);

        mSource.verifyFloatProperties();
    }

    @Test
    public void testVerifyFloatPropertys_success() {
        Parallax.FloatProperty var1 = mSource.addProperty("var1");
        Parallax.FloatProperty var2 = mSource.addProperty("var2");

        var1.setValue(mSource, (float) 499);
        var2.setValue(mSource, (float) 500);

        mSource.verifyFloatProperties();

        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, (float) 500);

        mSource.verifyFloatProperties();

        var1.setValue(mSource, (float) 499);
        var2.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);

        mSource.verifyFloatProperties();
    }
}
