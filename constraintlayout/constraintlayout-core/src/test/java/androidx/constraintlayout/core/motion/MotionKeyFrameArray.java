/*
 * Copyright (C) 2021 The Android Open Source Project
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
package androidx.constraintlayout.core.motion;

import static org.junit.Assert.assertEquals;

import androidx.constraintlayout.core.motion.utils.KeyFrameArray;

import org.junit.Test;

import java.util.Random;

public class MotionKeyFrameArray {
    @Test
    public void arcTest1() {

        KeyFrameArray.CustomArray array = new KeyFrameArray.CustomArray();
        Random random = new Random();
        for (int i = 0; i < 32; i++) {
            assertEquals(i, array.size());
            array.append(i, null);
        }
        array.dump();
        for (int i = 0; i < array.size(); i++) {
            int k = array.keyAt(i);
            Object val = array.valueAt(i);
            assertEquals(null, val);
        }
        array.clear();
        for (int i = 0; i < 32; i++) {
            int k = random.nextInt(100);
            System.out.println(k);
            array.append(k, new CustomAttribute("foo", CustomAttribute.AttributeType.INT_TYPE));
            array.dump();
        }

    }
}
