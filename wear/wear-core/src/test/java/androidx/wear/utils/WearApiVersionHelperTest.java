/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class WearApiVersionHelperTest {
    @Mock
    WearApiVersionHelper.AbstractApiVersion mMockApiVersion;

    @Rule
    public final MockitoRule mocks = MockitoJUnit.rule();

    @Before
    public void setUp() {
        mMockApiVersion = mock(WearApiVersionHelper.AbstractApiVersion.class);
        WearApiVersionHelper.sTestApiVersion = mMockApiVersion;
        when(mMockApiVersion.compareTo(any())).thenCallRealMethod();
    }

    @Test
    public void test_samePlatformLevelSameIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(33);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(3);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_samePlatformLevelHigherIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(33);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(5);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_samePlatformLevelLowerIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(33);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(1);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_higherPlatformLevelLowerIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(40);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_higherPlatformLevelHigherIncrement_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(40);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_lowerPlatformLevelSameIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(30);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(3);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_lowerPlatformLevelLowerIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(30);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(1);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }

    @Test
    public void test_lowerPlatformLevelHigherIncrement_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(29);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(5);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_TIRAMISU_3));
    }


    @Test
    public void test_invalidVersion_exception() {
        assertThrows(IllegalArgumentException.class,
                () -> WearApiVersionHelper.isApiVersionAtLeast(
                "marmalade"));
        assertThrows(IllegalArgumentException.class,
                () -> WearApiVersionHelper.isApiVersionAtLeast(
                "33-3"));
    }

    @Test
    public void test_VicIsAtLeastVic_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(35);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(1);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_VIC_1));
    }

    @Test
    public void test_BaklavaIsAtLeastVic_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(36);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(0);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_VIC_1));
    }

    @Test
    public void test_VicIsAtLeastBaklava_failure() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(35);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(1);

        assertFalse(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_BAKLAVA_0));
    }

    @Test
    public void test_BaklavaIsAtLeastBaklava_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(36);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(0);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_BAKLAVA_0));
    }

    @Test
    public void test_CinnamonBunIsAtLeastBaklava_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(37);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(0);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_BAKLAVA_0));
    }

    @Test
    public void test_CinnamonBunIsAtLeastCinnamonBun_success() {
        when(mMockApiVersion.getPlatformApiLevel()).thenReturn(37);
        when(mMockApiVersion.getIncrementalApiLevel()).thenReturn(0);

        assertTrue(WearApiVersionHelper.isApiVersionAtLeast(
                WearApiVersionHelper.WEAR_CINNAMON_BUN_0));
    }

    @Test
    public void test_allWearConstantsAreAccountedForAndValid() throws Exception {
        Set<String> fieldNames = new java.util.HashSet<>();

        for (java.lang.reflect.Field field : WearApiVersionHelper.class.getDeclaredFields()) {
            if (field.getName().startsWith("WEAR_")
                    && field.getType().equals(String.class)
                    && java.lang.reflect.Modifier.isPublic(field.getModifiers())
                    && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                fieldNames.add(field.getName());
                String value = (String) field.get(null);

                if (value != null) {
                    // Verify that the constant is actually parsable and maps to a valid platform
                    // (This catches missing cases in the WearApiVersionCompat switch statement)
                    WearApiVersionHelper.isApiVersionAtLeast(value);
                }
            }
        }

        // Verify the @StringDef matches the constants.
        // Because @StringDef is SOURCE retention, we read the file to verify it.
        // We look for the source file relative to the project root.
        String sourcePath = "src/main/java/androidx/wear/utils/WearApiVersionHelper.java";
        java.io.File file = new java.io.File(sourcePath);
        if (!file.exists()) {
            // Try to find it if we are running in a different context (e.g. IDE vs command line)
            file = new java.io.File("frameworks/support/wear/wear-core/" + sourcePath);
        }

        if (file.exists()) {
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(file),
                            java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String content = sb.toString();

            // Find the @StringDef for WearApiVersionCode
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                    "@StringDef\\(\\s*value\\s*=\\s*\\{(.*?)\\}\\s*\\).*?public @interface "
                            + "WearApiVersionCode", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher m = p.matcher(content);

            if (m.find()) {
                String valuesBlock = m.group(1);
                Set<String> stringDefValues = new java.util.HashSet<>();
                if (valuesBlock != null) {
                    for (String val : valuesBlock.split(",")) {
                        String trimmed = val.trim();
                        if (!trimmed.isEmpty()) {
                            stringDefValues.add(trimmed);
                        }
                    }
                }

                // Check for constants in the class but missing from the @StringDef
                Set<String> missingFromStringDef = new java.util.HashSet<>(fieldNames);
                missingFromStringDef.removeAll(stringDefValues);
                assertTrue("The following WEAR_ constants are defined in the class but are "
                                + "missing from the @WearApiVersionCode @StringDef: "
                                + missingFromStringDef,
                        missingFromStringDef.isEmpty());

                // Check for entries in the @StringDef that don't match a constant
                Set<String> orphanedInStringDef = new java.util.HashSet<>(stringDefValues);
                orphanedInStringDef.removeAll(fieldNames);
                assertTrue("The @WearApiVersionCode @StringDef contains the following values "
                                + "that do not match any WEAR_ constant in the class: "
                                + orphanedInStringDef,
                        orphanedInStringDef.isEmpty());
            } else {
                org.junit.Assert.fail("Could not find @WearApiVersionCode @StringDef in "
                        + "WearApiVersionHelper.java for verification.");
            }
        } else {
            // Fallback for environments where source is not accessible (e.g. CI against AARs)
            System.err.println("Warning: Could not find source file " + file.getAbsolutePath()
                    + " to verify @StringDef consistency. Skipping source-based check.");
        }
    }
}


