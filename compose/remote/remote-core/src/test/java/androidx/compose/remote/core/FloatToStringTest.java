/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.core;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.utilities.StringUtils;

import org.junit.Test;

public class FloatToStringTest {
    @Test
    public void testSimpleConversions() {
        String str;
        str = StringUtils.floatToString(0.0f, 1, 2, (char) 0, (char) 0);
        assertEquals("\"0.\"", "\"" + str + "\"");
        str = StringUtils.floatToString(0.0f, 1, 2, (char) 0, ' ');
        assertEquals("\"0.  \"", "\"" + str + "\"");
        str = StringUtils.floatToString(0.0f, 1, 2, (char) 0, '0');
        assertEquals("\"0.00\"", "\"" + str + "\"");

        str = StringUtils.floatToString(1.0f, 2, 2, (char) 0, '0');
        assertEquals("\"1.00\"", "\"" + str + "\"");
        str = StringUtils.floatToString(1.0f, 2, 2, (char) ' ', '0');
        assertEquals("\" 1.00\"", "\"" + str + "\"");
        str = StringUtils.floatToString(1.0f, 2, 2, (char) '0', '0');
        assertEquals("\"01.00\"", "\"" + str + "\"");

        str = StringUtils.floatToString(0.124f, 1, 2, (char) 0, (char) 0);
        assertEquals("\"0.12\"", "\"" + str + "\"");
        str = StringUtils.floatToString(0.125f, 1, 2, (char) 0, ' ');
        assertEquals("\"0.13\"", "\"" + str + "\"");
        str = StringUtils.floatToString(0.106f, 1, 2, (char) 0, '0');
        assertEquals("\"0.11\"", "\"" + str + "\"");
    }

    /** */
    @Test
    public void testNegitiveStringConversions() {
        String str;
        str = StringUtils.floatToString(-2.02f, 1, 2, (char) 0, '0');
        assertEquals("\"-2.02\"", "\"" + str + "\"");
        str = StringUtils.floatToString(0.00f, 1, 3, (char) 0, '0');
        assertEquals("\"0.000\"", "\"" + str + "\"");
        str = StringUtils.floatToString(-0.02f, 1, 3, (char) 0, '0');
        assertEquals("\"-0.020\"", "\"" + str + "\"");

        str = StringUtils.floatToString(-0.02f, 1, 3, (char) 0, '0');
        assertEquals("\"-0.020\"", "\"" + str + "\"");
    }
}
