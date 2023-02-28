/*
 * Copyright (C) 2022 The Android Open Source Project
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

package androidx.constraintlayout.core.dsl;

import java.util.Arrays;

/**
 * This is the base Key for all the key (KeyCycle, KeyPosition, etc.) Objects
 */
public class Keys {

    protected String unpack(String[] str) {
        StringBuilder ret = new StringBuilder("[");
        for (int i = 0; i < str.length; i++) {

            ret.append((i == 0) ? "'" : ",'");

            ret.append(str[i]);
            ret.append("'");

        }
        ret.append("]");
        return ret.toString();
    }

    protected void append(StringBuilder builder, String name, int value) {
        if (value != Integer.MIN_VALUE) {
            builder.append(name);
            builder.append(":'").append(value).append("',\n");
        }
    }

    protected void append(StringBuilder builder, String name, String value) {
        if (value != null) {
            builder.append(name);
            builder.append(":'").append(value).append("',\n");
        }
    }

    protected void append(StringBuilder builder, String name, float value) {
        if (Float.isNaN(value)) {
            return;
        }
        builder.append(name);
        builder.append(":").append(value).append(",\n");

    }

    protected void append(StringBuilder builder, String name, String[] array) {
        if (array != null) {
            builder.append(name);
            builder.append(":").append(unpack(array)).append(",\n");
        }
    }

    protected void append(StringBuilder builder, String name, float[] array) {
        if (array != null) {
            builder.append(name);
            builder.append("percentWidth:").append(Arrays.toString(array)).append(",\n");
        }
    }

}
