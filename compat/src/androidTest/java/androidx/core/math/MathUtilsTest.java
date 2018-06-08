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

package androidx.core.math;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class MathUtilsTest {

    @Test
    public void testClamp() {
        // Int
        assertEquals(0, MathUtils.clamp(-4, 0, 7));
        assertEquals(3, MathUtils.clamp(3, -2, 7));
        assertEquals(0, MathUtils.clamp(0, 0, 7));
        assertEquals(7, MathUtils.clamp(7, 0, 7));
        assertEquals(7, MathUtils.clamp(8, -2, 7));

        // Double
        assertEquals(0.0, MathUtils.clamp(-0.4, 0.0, 7.0), 0.0);
        assertEquals(3.0, MathUtils.clamp(3.0, 0.0, 7.0), 0.0);
        assertEquals(0.1, MathUtils.clamp(0.1, 0.0, 7.0), 0.0);
        assertEquals(7.0, MathUtils.clamp(7.0, 0.0, 7.0), 0.0);
        assertEquals(-0.6, MathUtils.clamp(-0.7, -0.6, 7.0), 0.0);

        // Float
        assertEquals(0.0f, MathUtils.clamp(-0.4f, 0.0f, 7.0f), 0.0f);
        assertEquals(3.0f, MathUtils.clamp(3.0f, 0.0f, 7.0f), 0.0f);
        assertEquals(0.1f, MathUtils.clamp(0.1f, 0.0f, 7.0f), 0.0f);
        assertEquals(7.0f, MathUtils.clamp(7.0f, 0.0f, 7.0f), 0.0f);
        assertEquals(-0.6f, MathUtils.clamp(-0.7f, -0.6f, 7.0f), 0.0f);
    }
}
