/*
 * Copyright (C) 2020 The Android Open Source Project
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
package androidx.constraintlayout.core;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class SolverVariableValuesTest {

    @Test
    public void testOperations() {

        Cache mCache = new Cache();
        SolverVariable variable5 = new SolverVariable("v5", SolverVariable.Type.SLACK);
        SolverVariable variable1 = new SolverVariable("v1", SolverVariable.Type.SLACK);
        SolverVariable variable3 = new SolverVariable("v3", SolverVariable.Type.SLACK);
        SolverVariable variable7 = new SolverVariable("v7", SolverVariable.Type.SLACK);
        SolverVariable variable11 = new SolverVariable("v11", SolverVariable.Type.SLACK);
        SolverVariable variable12 = new SolverVariable("v12", SolverVariable.Type.SLACK);

        variable5.id = 5;
        variable1.id = 1;
        variable3.id = 3;
        variable7.id = 7;
        variable11.id = 11;
        variable12.id = 12;
        mCache.mIndexedVariables[variable5.id] = variable5;
        mCache.mIndexedVariables[variable1.id] = variable1;
        mCache.mIndexedVariables[variable3.id] = variable3;
        mCache.mIndexedVariables[variable7.id] = variable7;
        mCache.mIndexedVariables[variable11.id] = variable11;
        mCache.mIndexedVariables[variable12.id] = variable12;

        SolverVariableValues values = new SolverVariableValues(null, mCache);
        values.put(variable5, 1f);
        System.out.println(values);
        values.put(variable1, -1f);
        System.out.println(values);
        values.put(variable3, -1f);
        System.out.println(values);
        values.put(variable7, 1f);
        System.out.println(values);
        values.put(variable11, 1f);
        System.out.println(values);
        values.put(variable12, -1f);
        System.out.println(values);
        values.remove(variable1, true);
        System.out.println(values);
        values.remove(variable3, true);
        System.out.println(values);
        values.remove(variable7, true);
        System.out.println(values);
        values.add(variable5, 1f, true);
        System.out.println(values);

        int currentSize = values.getCurrentSize();
        for (int i = 0; i < currentSize; i++) {
            SolverVariable variable = values.getVariable(i);
        }
    }

    @Test
    public void testBasic() {

        Cache mCache = new Cache();
        SolverVariable variable1 = new SolverVariable("A", SolverVariable.Type.SLACK);
        SolverVariable variable2 = new SolverVariable("B", SolverVariable.Type.SLACK);
        SolverVariable variable3 = new SolverVariable("C", SolverVariable.Type.SLACK);
        variable1.id = 0;
        variable2.id = 1;
        variable3.id = 2;
        mCache.mIndexedVariables[variable1.id] = variable1;
        mCache.mIndexedVariables[variable2.id] = variable2;
        mCache.mIndexedVariables[variable3.id] = variable3;
        SolverVariableValues values = new SolverVariableValues(null, mCache);

        variable1.id = 10;
        variable2.id = 100;
        variable3.id = 1000;

        values.put(variable1, 1);
        values.put(variable2, 2);
        values.put(variable3, 3);

        float v1 = values.get(variable1);
        float v2 = values.get(variable2);
        float v3 = values.get(variable3);
        assertEquals(v1, 1f, 0f);
        assertEquals(v2, 2f, 0f);
        assertEquals(v3, 3f, 0f);
    }

    @Test
    public void testBasic2() {
        Cache mCache = new Cache();
        SolverVariableValues values = new SolverVariableValues(null, mCache);
        SolverVariable variable1 = new SolverVariable("A", SolverVariable.Type.SLACK);
        SolverVariable variable2 = new SolverVariable("B", SolverVariable.Type.SLACK);
        SolverVariable variable3 = new SolverVariable("C", SolverVariable.Type.SLACK);

        variable1.id = 32;
        variable2.id = 32 * 2;
        variable3.id = 32 * 3;

        values.put(variable1, 1);
        values.put(variable2, 2);
        values.put(variable3, 3);

        float v1 = values.get(variable1);
        float v2 = values.get(variable2);
        float v3 = values.get(variable3);
        assertEquals(v1, 1f, 0f);
        assertEquals(v2, 2f, 0f);
        assertEquals(v3, 3f, 0f);
    }

    @Test
    public void testBasic3() {
        Cache mCache = new Cache();
        SolverVariableValues values = new SolverVariableValues(null, mCache);
        ArrayList<SolverVariable> variables = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            SolverVariable variable = new SolverVariable("A" + i, SolverVariable.Type.SLACK);
            variable.id = i * 32;
            values.put(variable, i);
            variables.add(variable);
        }
        int i = 0;
        for (SolverVariable variable : variables) {
            float value = i;
            assertEquals(value, values.get(variable), 0f);
            i++;
        }
//        System.out.println("array size: count: " + values.count
//          + " keys: " + values.keys.length + " values: " + values.values.length);
//        values.maxDepth();
    }

    @Test
    public void testBasic4() {
        Cache mCache = new Cache();
        SolverVariableValues values = new SolverVariableValues(null, mCache);
        ArrayList<SolverVariable> variables = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            SolverVariable variable = new SolverVariable("A" + i, SolverVariable.Type.SLACK);
            variable.id = i;
            values.put(variable, i);
            variables.add(variable);
        }
        int i = 0;
        for (SolverVariable variable : variables) {
            float value = i;
            assertEquals(value, values.get(variable), 0f);
            i++;
        }
//        System.out.println("array size: count: " + values.count
//          + " keys: " + values.keys.length + " values: " + values.values.length);
//        values.maxDepth();
    }

    @Test
    public void testBasic5() {
        Cache mCache = new Cache();
        SolverVariableValues values = new SolverVariableValues(null, mCache);
        ArrayList<SolverVariable> variables = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            SolverVariable variable = new SolverVariable("A" + i, SolverVariable.Type.SLACK);
            variable.id = i;
            values.put(variable, i);
            variables.add(variable);
        }
        int i = 0;
        for (SolverVariable variable : variables) {
            if (i % 2 == 0) {
                values.remove(variable, false);
            }
            i++;
        }
        i = 0;
        for (SolverVariable variable : variables) {
            float value = i;
            if (i % 2 != 0) {
                assertEquals(value, values.get(variable), 0f);
            }
            i++;
        }
//        System.out.println("array size: count: " + values.count
//          + " keys: " + values.keys.length + " values: " + values.values.length);
//        values.maxDepth();
    }

    @Test
    public void testBasic6() {
        Cache mCache = new Cache();
        SolverVariableValues values = new SolverVariableValues(null, mCache);
        ArrayList<SolverVariable> variables = new ArrayList<>();
        HashMap<SolverVariable, Float> results = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            SolverVariable variable = new SolverVariable("A" + i, SolverVariable.Type.SLACK);
            variable.id = i;
            values.put(variable, i);
            results.put(variable, (float) i);
            variables.add(variable);
        }
        ArrayList<SolverVariable> toRemove = new ArrayList<>();
        Random random = new Random(1234);
        for (SolverVariable variable : variables) {
            if (random.nextFloat() > 0.3f) {
                toRemove.add(variable);
            }
        }
        variables.removeAll(toRemove);
        for (int i = 0; i < 100; i++) {
            SolverVariable variable = new SolverVariable("B" + i, SolverVariable.Type.SLACK);
            variable.id = 100 + i;
            values.put(variable, i);
            results.put(variable, (float) i);
            variables.add(variable);
        }
        for (SolverVariable variable : variables) {
            float value = results.get(variable);
            assertEquals(value, values.get(variable), 0f);
        }
//        System.out.println("array size: count: " + values.count
//          + " keys: " + values.keys.length + " values: " + values.values.length);
//        values.maxDepth();
    }
}
