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
public class ParallaxFloatSourceTest {

    List<ParallaxSource.FloatVariable> variables;
    ParallaxSource.FloatVariable screenMax;
    ParallaxSource<ParallaxSource.FloatVariable> source;

    static void assertFloatEquals(float expected, float actual) {
        org.junit.Assert.assertEquals((double)expected, (double)actual, 0.0001d);
    }

    @Before
    public void setUp() throws Exception {
        source = new ParallaxSource<ParallaxSource.FloatVariable>() {
            public List<ParallaxSource.FloatVariable> getVariables() {
                return variables;
            }

            public void setListener(ParallaxSource.Listener listener) {
            }

            public ParallaxSource.FloatVariable getMaxParentVisibleSize() {
                return screenMax;
            }
        };
        variables = new ArrayList<ParallaxSource.FloatVariable>();
        screenMax = new ParallaxSource.FloatVariable(source);
    }

    @Test
    public void testVariable() {
        screenMax.setFloatValue(1080);
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);
        var1.setFloatValue(54);
        assertFloatEquals((float)54, var1.getFloatValue());
        var1.setName("testname123");
        assertEquals(var1.getName(), "testname123");
        var1.setFloatValue(2000);
        assertFloatEquals((float)2000, var1.getFloatValue());
    }

    @Test
    public void testFixedKeyValue() {
        screenMax.setFloatValue(1080);
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);

        ParallaxSource.FloatVariableKeyValue keyValue = var1.at(1000);
        assertSame(keyValue.getVariable(), var1);
        assertFloatEquals((float)1000, keyValue.getFloatValue());
    }

    @Test
    public void testFractionOfKeyValue() {
        screenMax.setFloatValue(1080);
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);

        ParallaxSource.FloatVariableKeyValue keyValue = var1.at(0, 0.5f);
        assertSame(keyValue.getVariable(), var1);
        assertFloatEquals((float)540, keyValue.getFloatValue());
    }

    @Test
    public void testFixedKeyValueWithFraction() {
        screenMax.setFloatValue(1080);
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);

        ParallaxSource.FloatVariableKeyValue keyValue = var1.at(-100, 0.5f);
        assertSame(keyValue.getVariable(), var1);
        assertFloatEquals((float)440, keyValue.getFloatValue());

        ParallaxSource.FloatVariableKeyValue keyValue2 = var1.at(100, 0.5f);
        assertSame(keyValue2.getVariable(), var1);
        assertFloatEquals((float)640, keyValue2.getFloatValue());
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyFloatVariables_wrongOrder() {
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);
        ParallaxSource.FloatVariable var2 = new ParallaxSource.FloatVariable(source);
        variables.add(var2);

        var1.setFloatValue((int)500);
        var2.setFloatValue((int)499);

        ParallaxSource.verifyFloatVariables(variables);
    }

    @Test(expected=IllegalStateException.class)
    public void testVerifyFloatVariablesWrong_combination() {
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);
        ParallaxSource.FloatVariable var2 = new ParallaxSource.FloatVariable(source);
        variables.add(var2);

        var1.setFloatValue(ParallaxSource.FloatVariable.UNKNOWN_BEFORE);
        var2.setFloatValue(ParallaxSource.FloatVariable.UNKNOWN_AFTER);

        ParallaxSource.verifyFloatVariables(variables);
    }

    @Test
    public void testVerifyFloatVariables_success() {
        ParallaxSource.FloatVariable var1 = new ParallaxSource.FloatVariable(source);
        variables.add(var1);
        ParallaxSource.FloatVariable var2 = new ParallaxSource.FloatVariable(source);
        variables.add(var2);

        var1.setFloatValue((int)499);
        var2.setFloatValue((int)500);

        ParallaxSource.verifyFloatVariables(variables);

        var1.setFloatValue(ParallaxSource.FloatVariable.UNKNOWN_BEFORE);
        var2.setFloatValue((int)500);

        ParallaxSource.verifyFloatVariables(variables);

        var1.setFloatValue((int)499);
        var2.setFloatValue(ParallaxSource.FloatVariable.UNKNOWN_AFTER);

        ParallaxSource.verifyFloatVariables(variables);
    }
}
