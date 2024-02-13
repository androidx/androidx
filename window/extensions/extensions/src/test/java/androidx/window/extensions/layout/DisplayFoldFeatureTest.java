/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.window.extensions.layout;

import static androidx.window.extensions.layout.DisplayFoldFeature.FOLD_PROPERTY_SUPPORTS_HALF_OPENED;
import static androidx.window.extensions.layout.DisplayFoldFeature.TYPE_HINGE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class DisplayFoldFeatureTest {

    @Test
    public void test_builder_matches_constructor() {
        Set<@DisplayFoldFeature.FoldProperty Integer> properties = new HashSet<>();
        properties.add(FOLD_PROPERTY_SUPPORTS_HALF_OPENED);
        DisplayFoldFeature expected = new DisplayFoldFeature(TYPE_HINGE, properties);

        DisplayFoldFeature actual = new DisplayFoldFeature.Builder(TYPE_HINGE)
                .addProperty(FOLD_PROPERTY_SUPPORTS_HALF_OPENED)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void test_equals_matches() {
        Set<@DisplayFoldFeature.FoldProperty Integer> properties = new HashSet<>();
        properties.add(FOLD_PROPERTY_SUPPORTS_HALF_OPENED);
        DisplayFoldFeature first = new DisplayFoldFeature(TYPE_HINGE, properties);
        DisplayFoldFeature second = new DisplayFoldFeature(TYPE_HINGE, properties);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void test_getter_matches_values() {
        final int type = TYPE_HINGE;
        DisplayFoldFeature actual = new DisplayFoldFeature.Builder(type)
                .addProperty(FOLD_PROPERTY_SUPPORTS_HALF_OPENED)
                .build();

        assertEquals(type, actual.getType());
        assertTrue(actual.hasProperty(FOLD_PROPERTY_SUPPORTS_HALF_OPENED));
        assertTrue(actual.hasProperties(FOLD_PROPERTY_SUPPORTS_HALF_OPENED));
    }
}
