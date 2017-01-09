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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParallaxIntEffectTest {

    ParallaxSource.IntSource source;
    int screenMax;
    ParallaxEffect.IntEffect effect;
    @Mock ParallaxTarget target;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
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
        effect = new ParallaxEffect.IntEffect();
    }

    @Test
    public void testOneVariable() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");

        effect.setPropertyRanges(var1.atAbsolute(540), var1.atAbsolute(0));
        effect.target(target);

        // start
        var1.setIntValue(source, 540);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // 25% complete
        var1.setIntValue(source, 405);
        effect.performMapping(source);
        verify(target, times(1)).update(0.25f);
        Mockito.reset(target);

        // middle
        var1.setIntValue(source, 270);
        effect.performMapping(source);
        verify(target, times(1)).update(.5f);
        Mockito.reset(target);

        // 75% complete
        var1.setIntValue(source, 135);
        effect.performMapping(source);
        verify(target, times(1)).update(0.75f);
        Mockito.reset(target);

        // end
        var1.setIntValue(source, 0);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // after end
        var1.setIntValue(source, -1000);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // before start
        var1.setIntValue(source, 1000);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_before
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_after
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyKeyValueOfSameVariableInDesendantOrder() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");

        effect.setPropertyRanges(var1.atAbsolute(540), var1.atAbsolute(550));
        effect.target(target);
        var1.setIntValue(source, 0);
        effect.performMapping(source);
    }

    @Test
    public void testTwoVariable() {
        screenMax = 1080;
        ParallaxSource.IntProperty var1 = source.addProperty("var1");
        ParallaxSource.IntProperty var2 = source.addProperty("var2");

        effect.setPropertyRanges(var1.atAbsolute(540), var2.atAbsolute(540));
        effect.target(target);

        // start
        var1.setIntValue(source, 540);
        var2.setIntValue(source, 840);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // middle
        var1.setIntValue(source, 390);
        var2.setIntValue(source, 690);
        effect.performMapping(source);
        verify(target, times(1)).update(.5f);
        Mockito.reset(target);

        // end
        var1.setIntValue(source, 240);
        var2.setIntValue(source, 540);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // after end
        var1.setIntValue(source, 200);
        var2.setIntValue(source, 500);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // before start
        var1.setIntValue(source, 1000);
        var2.setIntValue(source, 1300);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_before
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_before
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, -1000);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_after
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);
        var2.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_after
        var1.setIntValue(source, 1000);
        var2.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_before and less
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, 500);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_before and hit second
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, 540);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_before with estimation
        var1.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_BEFORE);
        var2.setIntValue(source, 1080);
        effect.performMapping(source);
        verify(target, times(1)).update(0.5f);
        Mockito.reset(target);

        // unknown_after with estimation
        var1.setIntValue(source, 0);
        var2.setIntValue(source, ParallaxSource.IntProperty.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0.5f);
        Mockito.reset(target);
    }

}
