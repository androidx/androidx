/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.webkit.test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.junit.Assert.assertNotNull;

import androidx.annotation.RestrictTo;
import androidx.webkit.WebViewFeature;

import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests that validate {@link WebViewFeature} string constants.
 *
 * <p>Implemented as a unit test to allow ByteBuddy to read .class files and detect the {@link
 * RestrictTo} annotation.
 */
@RunWith(JUnit4.class)
public class PublicFeatureAvailabilityTest {

    /**
     * Feature constants that are public in AndroidX but still hidden in WebView.
     *
     * <p>Feature constants should generally <em>not</em> be added here, and must link to bugs that
     * explain the circumstances and how the situation will be resolved.
     */
    private static final Map<String, String> EXCEPTION_FEATURE_CONSTANTS =
            Collections.singletonMap(
                    WebViewFeature.HYPERLINK_CONTEXT_MENU_ITEMS, "https://crbug.com/538133088");

    @Test
    public void checkAllPublicFeatureValuesAreDistinct() throws IllegalAccessException {
        Map<String, Integer> counts = new HashMap<>();
        for (Field field : WebViewFeature.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers)
                    && Modifier.isStatic(modifiers)
                    && Modifier.isFinal(modifiers)
                    && field.getType().equals(String.class)) {
                String value = (String) field.get(null);
                counts.put(value, counts.getOrDefault(value, 0) + 1);
            }
        }
        Set<String> duplicateFeatureValues = new HashSet<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicateFeatureValues.add(entry.getKey());
            }
        }
        assertWithMessage("Duplicate WebViewFeature constant values found")
                .that(duplicateFeatureValues)
                .isEmpty();
    }

    /**
     * There has been several instances of a feature being released (i.e. having the {@link
     * RestrictTo} annotation removed) without the feature being supported by WebView. Most often,
     * this is because the DEV_SUFFIX is left behind in the Chromium repository.
     *
     * <p>This test guards against that by forcing feature authors to link to the CL where they make
     * the feature available in WebView. It is not a perfect system, but a reminder to the CL
     * reviewers to check.
     */
    @Test
    public void testWebViewFeaturesAreSupported()
            throws NoSuchFieldException, IllegalAccessException {
        TypePool pool = TypePool.Default.ofSystemLoader();
        Class<?> clazz = WebViewFeature.class;
        TypeDescription type = pool.describe(clazz.getName()).resolve();

        assertNotNull(type);
        Set<String> publicApiFields = new HashSet<>();
        for (FieldDescription.InDefinedShape field : type.getDeclaredFields()) {
            if (field.isPublic()
                    && field.isStatic()
                    && field.isFinal()
                    && field.getType().represents(String.class)
                    && !field.getDeclaredAnnotations().isAnnotationPresent(Deprecated.class)
                    && !field.getDeclaredAnnotations().isAnnotationPresent(RestrictTo.class)) {
                publicApiFields.add(field.getActualName());
            }
        }
        assertThat(publicApiFields).isNotEmpty();

        List<String> publicFieldValuesMissingMapping = new ArrayList<>();
        for (String fieldName : publicApiFields) {
            String value = (String) clazz.getField(fieldName).get(null);
            if (!EXCEPTION_FEATURE_CONSTANTS.containsKey(value)
                    && !PublicFeatureAvailability.PUBLIC_FEATURE_UNHIDE_CLS.containsKey(value)) {
                publicFieldValuesMissingMapping.add(fieldName);
            }
        }
        assertWithMessage(
                        "All public feature constants must be linked to the Chromium CL that"
                            + " unhides it. Update the mapping in PublicFeatureAvailability.java")
                .that(publicFieldValuesMissingMapping)
                .isEmpty();
    }

    @Test
    public void checkAllLinksAreChromiumCls() {
        Map<String, String> invalidEntries = new HashMap<>();
        for (Map.Entry<String, String> entry :
                PublicFeatureAvailability.PUBLIC_FEATURE_UNHIDE_CLS.entrySet()) {
            if (!entry.getValue().startsWith("https://crrev.com/c/")) {
                invalidEntries.put(entry.getKey(), entry.getValue());
            }
        }
        assertWithMessage(
                        "All public features should link to a Chromium CL using short-link syntax")
                .that(invalidEntries)
                .isEmpty();
    }

    @Test
    public void checkAllBugsAreChromiumBugs() {
        Map<String, String> invalidEntries = new HashMap<>();
        for (Map.Entry<String, String> entry : EXCEPTION_FEATURE_CONSTANTS.entrySet()) {
            if (!entry.getValue().startsWith("https://crbug.com/")) {
                invalidEntries.put(entry.getKey(), entry.getValue());
            }
        }
        assertWithMessage("All exceptions should link to a Chromium bug using short-link syntax")
                .that(invalidEntries)
                .isEmpty();
    }
}
