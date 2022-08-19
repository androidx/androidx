/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.graphics.opengl.egl;

import static org.junit.Assert.assertEquals;

import android.opengl.EGL14;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class EGLConfigAttributesJavaTest {

    @Test
    public void testEglConfigAttribute() {
        EGLConfigAttributes config = new EGLConfigAttributes.Builder()
                .setAttribute(1, 2)
                .setAttribute(3, 4)
                .setAttribute(5, 6)
                .build();
        int[] attrs = config.toArray();
        assertEquals(7, attrs.length);
        assertEquals(Integer.valueOf(2), findValueForKey(attrs, 1));
        assertEquals(Integer.valueOf(4), findValueForKey(attrs, 3));
        assertEquals(Integer.valueOf(6), findValueForKey(attrs, 5));
        assertEquals(EGL14.EGL_NONE, attrs[6]);
    }

    /**
     * Helper method that does a linear search of the key in an integer array and returns
     * the corresponding value for the key.
     * This assumes the array is structured in an alternating format of key/value pairs and ends
     * with the value of EGL_NONE
     * @param attrs Array of ints representing alternating key value pairs, ending with EGL_NONE
     * @param key Key to search for the corresponding value of
     * @return Value of the specified key or null if it was not found
     */
    private Integer findValueForKey(int[] attrs, int key) {
        for (int i = 0; i < attrs.length; i++) {
            if (attrs[i] == EGL14.EGL_NONE) {
                break;
            }
            if (attrs[i] == key) {
                if (i < attrs.length - 1) {
                    return attrs[i + 1];
                } else {
                    return null;
                }
            }
        }
        return null;
    }
}
