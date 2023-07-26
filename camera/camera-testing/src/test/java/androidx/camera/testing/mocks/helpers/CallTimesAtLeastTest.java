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

package androidx.camera.testing.mocks.helpers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.camera.testing.impl.mocks.helpers.CallTimesAtLeast;

import org.junit.Test;
import org.robolectric.annotation.Config;

@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
public class CallTimesAtLeastTest {
    private final CallTimesAtLeast mCallTimes = new CallTimesAtLeast(5);

    @Test
    public void actualCallCountMatchesExactly_isSatisfiedReturnsTrue() {
        assertTrue(mCallTimes.isSatisfied(5));
    }

    @Test
    public void actualCallCountIsLess_isSatisfiedReturnsFalse() {
        assertFalse(mCallTimes.isSatisfied(2));
    }

    @Test
    public void actualCallCountIsMore_isSatisfiedReturnsTrue() {
        assertTrue(mCallTimes.isSatisfied(8));
    }

}
