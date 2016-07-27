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

import android.support.test.runner.AndroidJUnitRunner;
import android.test.suitebuilder.annotation.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.support.test.runner.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ParallaxIntEffectTest {

    List<ParallaxSource.IntVariable> variables;
    ParallaxSource.IntVariable screenMax;
    ParallaxSource<ParallaxSource.IntVariable> source;
    ParallaxEffect.IntEffect effect;
    @Mock ParallaxTarget target;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        source = new ParallaxSource<ParallaxSource.IntVariable>() {
            public List<ParallaxSource.IntVariable> getVariables() {
                return variables;
            }

            public void setListener(ParallaxSource.Listener listener) {
            }

            public ParallaxSource.IntVariable getMaxParentVisibleSize() {
                return screenMax;
            }
        };
        variables = new ArrayList<ParallaxSource.IntVariable>();
        screenMax = new ParallaxSource.IntVariable(source);
        effect = new ParallaxEffect.IntEffect();
    }

    @Test
    public void testOneVariable() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        var1.setName("detail_banner_topEdge");

        effect.setVariableRanges(var1.at(540), var1.at(0));
        effect.target(target);

        // start
        var1.setIntValue(540);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // 25% complete
        var1.setIntValue(405);
        effect.performMapping(source);
        verify(target, times(1)).update(0.25f);
        Mockito.reset(target);

        // middle
        var1.setIntValue(270);
        effect.performMapping(source);
        verify(target, times(1)).update(.5f);
        Mockito.reset(target);

        // 75% complete
        var1.setIntValue(135);
        effect.performMapping(source);
        verify(target, times(1)).update(0.75f);
        Mockito.reset(target);

        // end
        var1.setIntValue(0);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // after end
        var1.setIntValue(-1000);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // before start
        var1.setIntValue(1000);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_before
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_after
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyKeyValueOfSameVariableInDesendantOrder() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        var1.setName("detail_banner_topEdge");

        effect.setVariableRanges(var1.at(540), var1.at(550));
        effect.target(target);
        var1.setIntValue(0);
        effect.performMapping(source);
    }

    @Test
    public void testTwoVariable() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        var1.setName("row1_top");
        ParallaxSource.IntVariable var2 = new ParallaxSource.IntVariable(source);
        variables.add(var2);
        var2.setName("row2_top");

        effect.setVariableRanges(var1.at(540), var2.at(540));
        effect.target(target);

        // start
        var1.setIntValue(540);
        var2.setIntValue(840);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // middle
        var1.setIntValue(390);
        var2.setIntValue(690);
        effect.performMapping(source);
        verify(target, times(1)).update(.5f);
        Mockito.reset(target);

        // end
        var1.setIntValue(240);
        var2.setIntValue(540);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // after end
        var1.setIntValue(200);
        var2.setIntValue(500);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // before start
        var1.setIntValue(1000);
        var2.setIntValue(1300);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_before
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_before
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue(-1000);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_after
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);
        var2.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_after
        var1.setIntValue(1000);
        var2.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0f);
        Mockito.reset(target);

        // unknown_before and less
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue(500);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_before and hit second
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue(540);
        effect.performMapping(source);
        verify(target, times(1)).update(1f);
        Mockito.reset(target);

        // unknown_before with estimation
        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue(1080);
        effect.performMapping(source);
        verify(target, times(1)).update(0.5f);
        Mockito.reset(target);

        // unknown_after with estimation
        var1.setIntValue(0);
        var2.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);
        effect.performMapping(source);
        verify(target, times(1)).update(0.5f);
        Mockito.reset(target);
    }

}
