/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.core.os;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Process;

import androidx.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public class ProcessCompatTest {

    @Test
    public void testIsApplicationUid() {
        assertTrue("Test process is an application",
                ProcessCompat.isApplicationUid(Process.myUid()));
        assertFalse("Test process is not an application",
                ProcessCompat.isApplicationUid(1000));
    }

    @Test
    public void testIsIsolatedUid() {
        assertFalse("Test process is not an isolated uid",
                ProcessCompat.isIsolatedUid(Process.myUid()));
        assertTrue("Isolated uid in range",
                ProcessCompat.isIsolatedUid(99000));
        assertFalse("System uid is not an isolated uid",
                ProcessCompat.isIsolatedUid(1000));
    }

    @Test
    public void testIsSdkSandboxUid() {
        assertFalse("Test process is not an sdk sandbox uid",
                ProcessCompat.isSdkSandboxUid(Process.myUid()));
        assertFalse("System uid is not an sdk sandbox uid",
                ProcessCompat.isSdkSandboxUid(1000));
    }
}
