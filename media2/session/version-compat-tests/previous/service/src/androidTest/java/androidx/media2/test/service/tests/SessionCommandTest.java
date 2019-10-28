/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2.test.service.tests;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Build;

import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tests {@link SessionCommand} and {@link SessionCommandGroup}.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN)
@RunWith(AndroidJUnit4.class)
@SmallTest
public class SessionCommandTest {
    // Prefix for all command codes
    private static final String PREFIX_COMMAND_CODE = "COMMAND_CODE_";

    private static final List<String> PREFIX_COMMAND_CODES = new ArrayList<>();
    static {
        PREFIX_COMMAND_CODES.add("COMMAND_CODE_PLAYER_");
        PREFIX_COMMAND_CODES.add("COMMAND_CODE_VOLUME_");
        PREFIX_COMMAND_CODES.add("COMMAND_CODE_SESSION_");
        PREFIX_COMMAND_CODES.add("COMMAND_CODE_LIBRARY_");
    }

    /**
     * Test possible typos in naming
     */
    @Test
    public void testCodes_name() {
        List<Field> fields = getSessionCommandsFields("");
        for (int i = 0; i < fields.size(); i++) {
            String name = fields.get(i).getName();

            boolean matches = false;
            if (name.startsWith("COMMAND_VERSION_") || name.equals("COMMAND_CODE_CUSTOM")) {
                matches = true;
            }
            if (!matches) {
                for (int j = 0; j < PREFIX_COMMAND_CODES.size(); j++) {
                    if (name.startsWith(PREFIX_COMMAND_CODES.get(j))) {
                        matches = true;
                        break;
                    }
                }
            }
            assertTrue("Unexpected constant " + name, matches);
        }
    }

    /**
     * Tests possible code duplications in values
     */
    @Test
    public void testCodes_valueDuplication() throws IllegalAccessException {
        List<Field> fields = getSessionCommandsFields(PREFIX_COMMAND_CODE);
        Set<Integer> values = new HashSet<>();
        for (int i = 0; i < fields.size(); i++) {
            Integer value = fields.get(i).getInt(null);
            assertTrue(values.add(value));
        }
    }

    /**
     * Tests whether codes are continuous
     */
    @Test
    @Ignore
    public void testCodes_valueContinuous() throws IllegalAccessException {
        for (int i = 0; i < PREFIX_COMMAND_CODES.size(); i++) {
            List<Field> fields = getSessionCommandsFields(PREFIX_COMMAND_CODES.get(i));
            List<Integer> values = new ArrayList<>();
            for (int j = 0; j < fields.size(); j++) {
                values.add(fields.get(j).getInt(null));
            }
            Collections.sort(values);
            for (int j = 1; j < values.size(); j++) {
                assertEquals(
                        "Command code isn't continuous. Missing " + (values.get(j - 1) + 1)
                                + " in " + PREFIX_COMMAND_CODES.get(i),
                        ((int) values.get(j - 1)) + 1, (int) values.get(j));
            }
        }
    }

    private static List<Field> getSessionCommandsFields(String prefix) {
        final List<Field> list = new ArrayList<>();
        final Field[] fields = SessionCommand.class.getFields();
        if (fields != null) {
            for (int i = 0; i < fields.length; i++) {
                if (isPublicStaticFinalInt(fields[i])
                        && fields[i].getName().startsWith(prefix)) {
                    list.add(fields[i]);
                }
            }
        }
        return list;
    }

    private static boolean isPublicStaticFinalInt(Field field) {
        if (field.getType() != int.class) {
            return false;
        }
        int modifier = field.getModifiers();
        return Modifier.isPublic(modifier) && Modifier.isStatic(modifier)
                && Modifier.isFinal(modifier);
    }
}
