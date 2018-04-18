// CHECKSTYLE:OFF Generated code
/* This file is auto-generated from ParallaxIntEffectTest.java.  DO NOT MODIFY. */

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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.util.Property;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParallaxFloatEffectTest {

    Parallax<Parallax.FloatProperty> mSource;
    int mScreenMax;
    ParallaxEffect.FloatEffect mEffect;
    @Mock ParallaxTarget mTarget;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double) expected, (double) actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
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
        mEffect = new ParallaxEffect.FloatEffect();
    }

    @Test
    public void testOneVariable() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");

        mEffect.setPropertyRanges(var1.atAbsolute(540), var1.atAbsolute(0));
        mEffect.target(mTarget);

        // start
        var1.setValue(mSource, 540);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);

        // 25% complete
        var1.setValue(mSource, 405);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0.25f);
        Mockito.reset(mTarget);

        // middle
        var1.setValue(mSource, 270);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(.5f);
        Mockito.reset(mTarget);

        // 75% complete
        var1.setValue(mSource, 135);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0.75f);
        Mockito.reset(mTarget);

        // end
        var1.setValue(mSource, 0);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // after end
        var1.setValue(mSource, -1000);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // before start
        var1.setValue(mSource, 1000);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);

        // unknown_before
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // unknown_after
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyKeyValueOfSameVariableInDesendantOrder() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");

        mEffect.setPropertyRanges(var1.atAbsolute(540), var1.atAbsolute(550));
        mEffect.target(mTarget);
        var1.setValue(mSource, 0);
        mEffect.performMapping(mSource);
    }

    @Test
    public void testTwoVariable() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");
        Parallax.FloatProperty var2 = mSource.addProperty("var2");

        mEffect.setPropertyRanges(var1.atAbsolute(540), var2.atAbsolute(540));
        mEffect.target(mTarget);

        // start
        var1.setValue(mSource, 540);
        var2.setValue(mSource, 840);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);

        // middle
        var1.setValue(mSource, 390);
        var2.setValue(mSource, 690);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(.5f);
        Mockito.reset(mTarget);

        // end
        var1.setValue(mSource, 240);
        var2.setValue(mSource, 540);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // after end
        var1.setValue(mSource, 200);
        var2.setValue(mSource, 500);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // before start
        var1.setValue(mSource, 1000);
        var2.setValue(mSource, 1300);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);

        // unknown_before
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // unknown_before
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, -1000);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // unknown_after
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);
        var2.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);

        // unknown_after
        var1.setValue(mSource, 1000);
        var2.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0f);
        Mockito.reset(mTarget);

        // unknown_before and less
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, 500);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // unknown_before and hit second
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, 540);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(1f);
        Mockito.reset(mTarget);

        // unknown_before with estimation
        var1.setValue(mSource, Parallax.FloatProperty.UNKNOWN_BEFORE);
        var2.setValue(mSource, 1080);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0.5f);
        Mockito.reset(mTarget);

        // unknown_after with estimation
        var1.setValue(mSource, 0);
        var2.setValue(mSource, Parallax.FloatProperty.UNKNOWN_AFTER);
        mEffect.performMapping(mSource);
        verify(mTarget, times(1)).update(0.5f);
        Mockito.reset(mTarget);
    }

    @Test
    public void testDirectMapping() {
        mScreenMax = 1080;
        Parallax.FloatProperty var1 = mSource.addProperty("var1");

        mEffect.setPropertyRanges(var1.atAbsolute((float) 540.45), var1.atAbsolute((float) 0.22));
        Object object = new Object();
        final float[] properValue = new float[1];
        Property<Object, Float> property = new Property<Object, Float>(Float.class, "attr") {
            @Override
            public void set(Object object, Float value) {
                properValue[0] = value;
            }

            @Override
            public Float get(Object o) {
                return properValue[0];
            }
        };
        mTarget = new ParallaxTarget.DirectPropertyTarget<>(object, property);
        mEffect.target(mTarget);

        var1.setValue(mSource, (float) 540.45);
        mEffect.performMapping(mSource);
        assertFloatEquals((float) 540.45, properValue[0]);

        var1.setValue(mSource, (float) 405.85);
        mEffect.performMapping(mSource);
        assertFloatEquals((float) 405.85, properValue[0]);

        var1.setValue(mSource, 2000);
        mEffect.performMapping(mSource);
        assertFloatEquals((float) 540.45, properValue[0]);

        var1.setValue(mSource, (float) 0.22);
        mEffect.performMapping(mSource);
        assertFloatEquals((float) 0.22, properValue[0]);

        var1.setValue(mSource, (float) 0.12);
        mEffect.performMapping(mSource);
        assertFloatEquals((float) 0.22, properValue[0]);
    }
}
