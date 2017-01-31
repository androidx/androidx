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
public class ParallaxIntSourceTest {

    ParallaxSource.IntSource source;
    int screenMax;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double)expected, (double)actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
        source = new ParallaxSource.IntSource<ParallaxSource.IntProperty>() {

            public void setListener(ParallaxSource.Listener listener) {
            }

            public int getMaxParentVisibleSize() {
                return screenMax;
            }

            @Override
            public IntProperty createProperty(String name, int index) {
                return new IntProperty(name, index);
            }
        };
    }

    @Test
    public void testVariable() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");
        var1.setIntValue(source, 54);
        assertEquals((int)54, var1.getIntValue(source));
        assertEquals(var1.getName(), "var1");
        var1.set(source, (int)2000);
        assertEquals((int)2000, var1.get(source).intValue());
    }

    @Test
    public void testFixedKeyValue() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");

        ParallaxSource.IntPropertyKeyValue keyValue = var1.atAbsolute(1000);
        assertSame(keyValue.getProperty(), var1);
        assertEquals((int)1000, keyValue.getKeyValue(source));
    }

    @Test
    public void testFractionOfKeyValue() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");

        ParallaxSource.IntPropertyKeyValue keyValue = var1.at(0, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertEquals((int)540, keyValue.getKeyValue(source));
    }

    @Test
    public void testFixedKeyValueWithFraction() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");

        ParallaxSource.IntPropertyKeyValue keyValue = var1.at(-100, 0.5f);
        assertSame(keyValue.getProperty(), var1);
        assertEquals((int)440, keyValue.getKeyValue(source));

        ParallaxSource.IntPropertyKeyValue keyValue2 = var1.at(100, 0.5f);
        assertSame(keyValue2.getProperty(), var1);
        assertEquals((int)640, keyValue2.getKeyValue(source));
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyIntPropertys_wrongOrder() {
        ParallaxSource.IntProperty var1 = source.addProperty("var1");
        ParallaxSource.IntProperty var2 = source.addProperty("var2");;

        var1.setIntValue(source, (int)500);
        var2.setIntValue(source, (int)499);

        source.verifyProperties();
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyIntPropertysWrong_combination() {
        ParallaxSource.IntProperty var1 = source.addProperty("var1");
        ParallaxSource.IntProperty var2 = source.addProperty("var2");

        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);

        source.verifyProperties();
    }

    @Test
    public void testVerifyIntPropertys_success() {
        ParallaxSource.IntProperty var1 = source.addProperty("var1");
        ParallaxSource.IntProperty var2 = source.addProperty("var2");

        var1.setIntValue(source, (int)499);
        var2.setIntValue(source, (int)500);

        source.verifyProperties();

        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, (int)500);

        source.verifyProperties();

        var1.setIntValue(source, (int)499);
        var2.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);

        source.verifyProperties();
    }
}
