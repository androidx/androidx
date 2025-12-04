/*
 * Copyright 2025 The Android Open Source Project
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

import androidx.compose.remote.core.operations.utilities.touch.CommandParameters;

import org.junit.Test;

public class CommandParametersTest {
    private static final byte COLOR = 1;
    private static final byte FONT_SIZE = 2;
    private static final byte FONT_STYLE = 3;
    private static final byte FONT_WEIGHT = 4;
    private static final byte FONT_FAMILY = 5;
    private static final byte STR_DATA = 6;
    private static final byte INT_DATA = 7;
    private static final byte FLOAT_DATA = 8;

    @Test
    public void basic() {
        CommandParameters param =
                new CommandParameters(
                        CommandParameters.param("textId", COLOR, CommandParameters.P_INT),
                        CommandParameters.param("fontSize", FONT_SIZE, CommandParameters.P_FLOAT),
                        CommandParameters.param("fontStyle", FONT_STYLE, CommandParameters.P_BYTE),
                        CommandParameters.param("fontWeight", FONT_WEIGHT,
                                CommandParameters.P_SHORT),
                        CommandParameters.param("fontFamily", FONT_FAMILY,
                                CommandParameters.P_BOOLEAN),
                        CommandParameters.param("strData", STR_DATA, CommandParameters.PA_STRING),
                        CommandParameters.param("intData", INT_DATA, CommandParameters.PA_INT),
                        CommandParameters.param("floatData", FLOAT_DATA, CommandParameters.PA_FLOAT)
                );
        WireBuffer buffer = new WireBuffer();
        buffer.writeShort(8);
        param.write(buffer, COLOR, 1);
        param.write(buffer, FONT_SIZE, 2.2f);
        param.write(buffer, FONT_STYLE, (byte) 3);
        param.write(buffer, FONT_WEIGHT, (short) 4);
        param.write(buffer, FONT_FAMILY, true);
        param.write(buffer, STR_DATA, "Hello World");
        param.write(buffer, INT_DATA, new int[]{1, 2, 3});
        param.write(buffer, FLOAT_DATA, new float[]{1.0f, 2.0f, 3.0f});
        buffer.setIndex(0);
        final int[] data = new int[1];
        int len =  buffer.readShort();
        for (int i = 0; i < len; i++) {
            param.read(buffer, new CommandParameters.Callback() {
                @Override
                public void value(int id, int value) {
                    assert id == COLOR;
                    assert value == 1;
                    data[0]++;
                }

                @Override
                public void value(int id, float value) {
                    assert id == FONT_SIZE;
                    assert value == 2.2f;
                    data[0]++;
                }

                @Override
                public void value(int id, short value) {
                    assert id == FONT_WEIGHT;
                    assert value == 4;
                    data[0]++;
                }

                @Override
                public void value(int id, byte value) {
                    assert id == FONT_STYLE;
                    assert value == 3;
                    data[0]++;
                }

                @Override
                public void value(int id, boolean value) {
                    assert id == FONT_FAMILY;
                    assert value;
                    data[0]++;
                }

                @Override
                public void value(int id, String value) {
                    assert id == STR_DATA;
                    assert value.equals("Hello World");
                    data[0]++;
                }

                @Override
                public void value(int id, int[] value) {
                    assert id == INT_DATA;
                    assert value.length == 3;
                    assert value[0] == 1;
                    assert value[1] == 2;
                    assert value[2] == 3;
                    data[0]++;
                }

                @Override
                public void value(int id, float[] value) {
                    assert id == FLOAT_DATA;
                    assert value.length == 3;
                    assert value[0] == 1.0f;
                    assert value[1] == 2.0f;
                    assert value[2] == 3.0f;
                    data[0]++;
                }
            });
        }
        assert data[0] == 8;
    }
}
