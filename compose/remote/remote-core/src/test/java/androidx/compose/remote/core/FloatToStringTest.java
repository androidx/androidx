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

import static androidx.compose.remote.core.operations.utilities.StringUtils.GROUPING_BY3;
import static androidx.compose.remote.core.operations.utilities.StringUtils.GROUPING_BY32;
import static androidx.compose.remote.core.operations.utilities.StringUtils.GROUPING_BY4;
import static androidx.compose.remote.core.operations.utilities.StringUtils.GROUPING_NONE;
import static androidx.compose.remote.core.operations.utilities.StringUtils.NEGATIVE_PARENTHESES;
import static androidx.compose.remote.core.operations.utilities.StringUtils.NO_OPTIONS;
import static androidx.compose.remote.core.operations.utilities.StringUtils.PAD_NONE;
import static androidx.compose.remote.core.operations.utilities.StringUtils.PAD_SPACE;
import static androidx.compose.remote.core.operations.utilities.StringUtils.PAD_ZERO;
import static androidx.compose.remote.core.operations.utilities.StringUtils.ROUNDING;
import static androidx.compose.remote.core.operations.utilities.StringUtils.SEPARATOR_COMMA_PERIOD;
import static androidx.compose.remote.core.operations.utilities.StringUtils.SEPARATOR_PERIOD_COMMA;
import static androidx.compose.remote.core.operations.utilities.StringUtils.SEPARATOR_SPACE_COMMA;
import static androidx.compose.remote.core.operations.utilities.StringUtils.SEPARATOR_UNDER_PERIOD;

import static org.junit.Assert.assertEquals;

import androidx.compose.remote.core.operations.utilities.StringUtils;

import org.junit.Test;

import java.math.BigDecimal;

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

    /**
     *
     */
    @Test
    public void testNegativeStringConversions() {
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


    @Test
    public void testOptionFlags() {

        assertEquals("1,234.56",
                StringUtils.floatToString(1234.56f, 8, 2,
                        PAD_NONE, ' ', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("-1,234.56",
                StringUtils.floatToString(-1234.56f, 8, 2,
                        PAD_NONE, ' ', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("0.0 ",
                StringUtils.floatToString(0f, 1, 2,
                        PAD_NONE, PAD_SPACE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));

        assertEquals("7.1200",
                StringUtils.floatToString(7.12f, 1, 4, ' ',
                        '0', SEPARATOR_COMMA_PERIOD, GROUPING_BY3, NO_OPTIONS));
        assertEquals("7.12",
                StringUtils.floatToString(7.12f, 1, 4, ' ',
                        PAD_NONE, SEPARATOR_COMMA_PERIOD, GROUPING_BY3, NO_OPTIONS));
        assertEquals("7.1200",
                StringUtils.floatToString(7.12f, 1, 4, ' ',
                        PAD_ZERO, SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));


        assertEquals("     123.45",
                StringUtils.floatToString(123.45f, 8, 2,
                        ' ', '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("00000123.45",
                StringUtils.floatToString(123.45f, 8, 2,
                        PAD_ZERO, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("123.45",
                StringUtils.floatToString(123.45f, 8, 2,
                        PAD_NONE, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        // assertEquals("-0000123.45" ,
        // StringUtils.floatToString(-123.45f, 8, 2,  PAD_ZERO, '0',
        // SEPARATOR_PERIOD_COMMA, GROUPING_BY3,false));


        int options = GROUPING_BY3;
        assertEquals("1.234.567,90",
                StringUtils.floatToString(1234567.89f, 9, 2,
                        PAD_NONE, '0', SEPARATOR_PERIOD_COMMA,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("1 234 567,90",
                StringUtils.floatToString(1234567.89f, 9, 2,
                        PAD_NONE, '0', SEPARATOR_SPACE_COMMA,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("1_234_567.90",
                StringUtils.floatToString(1234567.89f, 9, 2,
                        PAD_NONE, '0', SEPARATOR_UNDER_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));


        //  numbers exceeding the floating point precision
        assertEquals("1234567936.0 ",
                StringUtils.floatToString(1234567890.12f, 20, 2,
                        PAD_NONE, PAD_SPACE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, NO_OPTIONS));
        assertEquals("1,234,567,936.0",
                StringUtils.floatToString(1234567890.12f, 20, 1,
                        PAD_NONE, PAD_ZERO, SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("12,3456,7936.00",
                StringUtils.floatToString(1234567890.12f, 20, 2,
                        PAD_NONE, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY4, NO_OPTIONS));
        assertEquals("1,23,45,67,936.00",
                StringUtils.floatToString(1234567890.12f, 20, 2,
                        PAD_NONE, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY32, NO_OPTIONS));

        assertEquals("-890.12",
                StringUtils.floatToString(-890.12f, 5, 2,
                        PAD_NONE, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NO_OPTIONS));
        assertEquals("(890.12)",
                StringUtils.floatToString(-890.12f, 20, 2,
                        PAD_NONE, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NEGATIVE_PARENTHESES));
        assertEquals("(       890.12)",
                StringUtils.floatToString(-890.12f, 10, 2,
                        PAD_SPACE, '0', SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NEGATIVE_PARENTHESES));
        System.out.println("------------------------------------------");

        System.out.println("7. Edge Cases (Rounding)");
        assertEquals("10.0",
                StringUtils.floatToString(9.991f, 10, 1,
                        PAD_NONE, PAD_ZERO, SEPARATOR_COMMA_PERIOD, GROUPING_NONE, ROUNDING));
        assertEquals("9.99",
                StringUtils.floatToString(9.991f, 10, 2,
                        PAD_NONE, PAD_ZERO, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, NO_OPTIONS));
        assertEquals("10.0",
                StringUtils.floatToString(9.999f, 10, 2,
                        PAD_NONE, PAD_NONE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, ROUNDING));
        assertEquals("10",
                StringUtils.floatToString(9.999f, 10, 0,
                        PAD_NONE, PAD_NONE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, ROUNDING));
        assertEquals("10.0 ",
                StringUtils.floatToString(9.999f, 10, 2,
                        PAD_NONE, PAD_SPACE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, ROUNDING));
        assertEquals("123.0",
                StringUtils.floatToString(123f, 10, 1,
                        PAD_NONE, PAD_ZERO, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, NO_OPTIONS));
        assertEquals("123.0",
                StringUtils.floatToString(123f, 10, 1,
                        PAD_NONE, PAD_ZERO, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, NO_OPTIONS));
        System.out.println("------------------------------------------");

        System.out.println("8. Complex Combination Example");
        float myValue = -12345.678f;

        System.out.println("   Value: " + myValue);
        System.out.println("   Options: PAD_PRE_SPACE | PAD_AFTER_ZERO | "
                + "SEPARATOR_COMMA_PERIOD | GROUPING_BY3 | NEGATIVE_PARENTHESES");
        assertEquals("(         12,345.6780)",
                StringUtils.floatToString(myValue, 15, 4,
                        PAD_SPACE, PAD_ZERO, SEPARATOR_COMMA_PERIOD,
                        GROUPING_BY3, NEGATIVE_PARENTHESES));
        assertEquals("(         12.345,6780)",
                StringUtils.floatToString(myValue, 15, 4,
                        PAD_SPACE, PAD_ZERO, SEPARATOR_PERIOD_COMMA,
                        GROUPING_BY3, NEGATIVE_PARENTHESES));
        assertEquals("(         12_345.6780)",
                StringUtils.floatToString(myValue, 15, 4,
                        PAD_SPACE, PAD_ZERO, SEPARATOR_UNDER_PERIOD,
                        GROUPING_BY3, NEGATIVE_PARENTHESES));
        assertEquals("(         12 345,6780)",
                StringUtils.floatToString(myValue, 15, 4,
                        PAD_SPACE, PAD_ZERO, SEPARATOR_SPACE_COMMA,
                        GROUPING_BY3, NEGATIVE_PARENTHESES));
        System.out.println("------------------------------------------");

    }


    @Test
    public void largerFractional() {
        System.out.println(1.123456789f);
        BigDecimal bigDecimal = new BigDecimal(1.123456789f);
        System.out.println(bigDecimal);
        assertEquals("1.12345685",
                StringUtils.floatToString(1.123456789f, 2, 18,
                        PAD_NONE, PAD_NONE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, ROUNDING));
        System.out.println(11.123456789f);

        assertEquals("11.123457",
                StringUtils.floatToString(11.123456789f, 2, 18,
                        PAD_NONE, PAD_NONE, SEPARATOR_COMMA_PERIOD,
                        GROUPING_NONE, ROUNDING));
    }

    @Test
    public void testOne() {

        assertEquals("10,0 ",
                StringUtils.floatToString(9.999f, 10, 2,
                        PAD_NONE, PAD_SPACE, SEPARATOR_PERIOD_COMMA,
                        GROUPING_NONE, ROUNDING));

    }


}
