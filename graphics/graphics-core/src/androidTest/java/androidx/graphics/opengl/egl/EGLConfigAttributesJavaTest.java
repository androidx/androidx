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
        assertEquals(1, attrs[0]);
        assertEquals(2, attrs[1]);
        assertEquals(3, attrs[2]);
        assertEquals(4, attrs[3]);
        assertEquals(5, attrs[4]);
        assertEquals(6, attrs[5]);
        assertEquals(EGL14.EGL_NONE, attrs[6]);
    }
}
