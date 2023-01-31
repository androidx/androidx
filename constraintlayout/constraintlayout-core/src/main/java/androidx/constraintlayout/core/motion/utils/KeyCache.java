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

package androidx.constraintlayout.core.motion.utils;

import java.util.Arrays;
import java.util.HashMap;

/**
 * Used by KeyTimeCycles (and any future time dependent behaviour) to cache its current parameters
 * to maintain consistency across requestLayout type rebuilds.
 */
public class KeyCache {

    HashMap<Object, HashMap<String, float[]>> mMap = new HashMap<>();

    // @TODO: add description
    public void setFloatValue(Object view, String type, int element, float value) {
        if (!mMap.containsKey(view)) {
            HashMap<String, float[]> array = new HashMap<>();
            float[] vArray = new float[element + 1];
            vArray[element] = value;
            array.put(type, vArray);
            mMap.put(view, array);
        } else {
            HashMap<String, float[]> array = mMap.get(view);
            if (array == null) {
                array = new HashMap<>();
            }

            if (!array.containsKey(type)) {
                float[] vArray = new float[element + 1];
                vArray[element] = value;
                array.put(type, vArray);
                mMap.put(view, array);
            } else {
                float[] vArray = array.get(type);
                if (vArray == null) {
                    vArray = new float[0];
                }
                if (vArray.length <= element) {
                    vArray = Arrays.copyOf(vArray, element + 1);
                }
                vArray[element] = value;
                array.put(type, vArray);
            }
        }
    }

    // @TODO: add description
    public float getFloatValue(Object view, String type, int element) {
        if (!mMap.containsKey(view)) {
            return Float.NaN;
        } else {
            HashMap<String, float[]> array = mMap.get(view);
            if (array == null || !array.containsKey(type)) {
                return Float.NaN;
            }
            float[] vArray = array.get(type);
            if (vArray == null) {
                return Float.NaN;
            }
            if (vArray.length > element) {
                return vArray[element];
            }
            return Float.NaN;
        }
    }
}
