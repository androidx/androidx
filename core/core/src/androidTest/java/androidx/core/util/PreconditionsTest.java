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

package androidx.core.util;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class PreconditionsTest {

    @Test
    public void testCheckFlagsArgument() throws Exception {
        final int allowedFlags = (1 << 0) | (1 << 1) | (1 << 2);

        int flags = 0;
        assertThat(Preconditions.checkFlagsArgument(flags, allowedFlags)).isEqualTo(flags);

        flags = (1 << 1) | (1 << 2);
        assertThat(Preconditions.checkFlagsArgument(flags, allowedFlags)).isEqualTo(flags);

        flags = (1 << 3);
        try {
            Preconditions.checkFlagsArgument(flags, allowedFlags);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) { }
    }
}
