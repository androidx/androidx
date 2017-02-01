// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from ParallaxIntSourceTest.java.  DO NOT MODIFY. */

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
package android.support.v17.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParallaxFloatSourceTest {

    ParallaxSource.FloatSource source;
    float screenMax;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double)expected, (double)actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
        source = new ParallaxSource.FloatSource<ParallaxSource.FloatProperty>() {

            public void setListener(ParallaxSource.Listener listener) {
            }

            public float getMaxParentVisibleSize() {
                return screenMax;
            }

            @Override
            public FloatProperty createProperty(String name, int index) {
                return new FloatProperty(name, index);
            }
        };
    }

    @Test
    public void testVariable() {
        screenMax = 1080;
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");
        var1.setFloatValue(source, 54);
        assertFloatEquals((float)54, var1.getFloatValue(source));
        assertEquals(var1.getName(), "var1");
        var1.set(source, (float)2000);
        assertFloatEquals((float)2000, var1.get(source).floatValue());
    }

    @Test
    public void testFixedKeyValue() {
        screenMax = 1080;
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");

        ParallaxSource.FloatPropertyKeyValue keyValue = var1.atAbsolute(1000);
        assertSame(keyValue.getProperty(), var1);
        assertFloatEquals((float)1000, keyValue.getKeyValue(source));
    }

    @Test
    public void testFractionOfKeyValue() {
        screenMax = 1080;
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");

        ParallaxSource.FloatPropertyKeyValue keyValue = var1.at(0, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertFloatEquals((float)540, keyValue.getKeyValue(source));
    }

    @Test
    public void testFixedKeyValueWithFraction() {
        screenMax = 1080;
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");

        ParallaxSource.FloatPropertyKeyValue keyValue = var1.at(-100, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertFloatEquals((float)440, keyValue.getKeyValue(source));

        ParallaxSource.FloatPropertyKeyValue keyValue2 = var1.at(100, 0.5f);
        assertSame(keyValue2.getProperty(), var1);
        assertFloatEquals((float)640, keyValue2.getKeyValue(source));
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyFloatPropertys_wrongOrder() {
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");
        ParallaxSource.FloatProperty var2 = source.addProperty("var2");;

        var1.setFloatValue(source, (float)500);
        var2.setFloatValue(source, (float)499);

        source.verifyProperties();
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyFloatPropertysWrong_combination() {
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");
        ParallaxSource.FloatProperty var2 = source.addProperty("var2");

        var1.setFloatValue(source, ParallaxSource.FloatProperty.UNKNOWN_BEFORE);
        var2.setFloatValue(source, ParallaxSource.FloatProperty.UNKNOWN_AFTER);

        source.verifyProperties();
    }

    @Test
    public void testVerifyFloatPropertys_success() {
        ParallaxSource.FloatProperty var1 = source.addProperty("var1");
        ParallaxSource.FloatProperty var2 = source.addProperty("var2");

        var1.setFloatValue(source, (float)499);
        var2.setFloatValue(source, (float)500);

        source.verifyProperties();

        var1.setFloatValue(source, ParallaxSource.FloatProperty.UNKNOWN_BEFORE);
        var2.setFloatValue(source, (float)500);

        source.verifyProperties();

        var1.setFloatValue(source, (float)499);
        var2.setFloatValue(source, ParallaxSource.FloatProperty.UNKNOWN_AFTER);

        source.verifyProperties();
    }
}
