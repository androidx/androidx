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
import static org.junit.Assert.assertSame;
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
public class ParallaxIntSourceTest {

    List<ParallaxSource.IntVariable> variables;
    ParallaxSource.IntVariable screenMax;
    ParallaxSource<ParallaxSource.IntVariable> source;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double)expected, (double)actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
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
    }

    @Test
    public void testVariable() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        var1.setIntValue(54);
        assertEquals((int)54, var1.getIntValue());
        var1.setName("testname123");
        assertEquals(var1.getName(), "testname123");
        var1.setIntValue(2000);
        assertEquals((int)2000, var1.getIntValue());
    }

    @Test
    public void testFixedKeyValue() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);

        ParallaxSource.IntVariableKeyValue keyValue = var1.at(1000);
        assertSame(keyValue.getVariable(), var1);
        assertEquals((int)1000, keyValue.getIntValue());
    }

    @Test
    public void testFractionOfKeyValue() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);

        ParallaxSource.IntVariableKeyValue keyValue = var1.at(0, 0.5f);
        assertSame(keyValue.getVariable(), var1);
        assertEquals((int)540, keyValue.getIntValue());
    }

    @Test
    public void testFixedKeyValueWithFraction() {
        screenMax.setIntValue(1080);
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);

        ParallaxSource.IntVariableKeyValue keyValue = var1.at(-100, 0.5f);
        assertSame(keyValue.getVariable(), var1);
        assertEquals((int)440, keyValue.getIntValue());

        ParallaxSource.IntVariableKeyValue keyValue2 = var1.at(100, 0.5f);
        assertSame(keyValue2.getVariable(), var1);
        assertEquals((int)640, keyValue2.getIntValue());
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyIntVariables_wrongOrder() {
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        ParallaxSource.IntVariable var2 = new ParallaxSource.IntVariable(source);
        variables.add(var2);

        var1.setIntValue((int)500);
        var2.setIntValue((int)499);

        ParallaxSource.verifyIntVariables(variables);
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyIntVariablesWrong_combination() {
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        ParallaxSource.IntVariable var2 = new ParallaxSource.IntVariable(source);
        variables.add(var2);

        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);

        ParallaxSource.verifyIntVariables(variables);
    }

    @Test
    public void testVerifyIntVariables_success() {
        ParallaxSource.IntVariable var1 = new ParallaxSource.IntVariable(source);
        variables.add(var1);
        ParallaxSource.IntVariable var2 = new ParallaxSource.IntVariable(source);
        variables.add(var2);

        var1.setIntValue((int)499);
        var2.setIntValue((int)500);

        ParallaxSource.verifyIntVariables(variables);

        var1.setIntValue(ParallaxSource.IntVariable.UNKNOWN_BEFORE);
        var2.setIntValue((int)500);

        ParallaxSource.verifyIntVariables(variables);

        var1.setIntValue((int)499);
        var2.setIntValue(ParallaxSource.IntVariable.UNKNOWN_AFTER);

        ParallaxSource.verifyIntVariables(variables);
    }
}
