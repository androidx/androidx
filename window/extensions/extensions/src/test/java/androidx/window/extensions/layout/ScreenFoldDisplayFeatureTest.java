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

import static androidx.window.extensions.layout.SupportedDisplayFeature.ScreenFoldDisplayFeature.TYPE_HINGE;

import static org.junit.Assert.assertEquals;

import androidx.window.extensions.layout.SupportedDisplayFeature.ScreenFoldDisplayFeature;

import org.junit.Test;

public class ScreenFoldDisplayFeatureTest {

    @Test
    public void test_builder_matches_constructor() {
        ScreenFoldDisplayFeature expected = new ScreenFoldDisplayFeature(TYPE_HINGE, true);

        ScreenFoldDisplayFeature actual = new ScreenFoldDisplayFeature.Builder(TYPE_HINGE, true)
                .build();

        assertEquals(expected, actual);
    }

    @Test
    public void test_equals_matches() {
        ScreenFoldDisplayFeature first = new ScreenFoldDisplayFeature(TYPE_HINGE, true);
        ScreenFoldDisplayFeature second = new ScreenFoldDisplayFeature(TYPE_HINGE, true);

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void test_getter_matches_values() {
        final int type = TYPE_HINGE;
        final boolean isHalfOpened = true;

        ScreenFoldDisplayFeature actual = new ScreenFoldDisplayFeature.Builder(type, isHalfOpened)
                .build();

        assertEquals(type, actual.getType());
        assertEquals(isHalfOpened, actual.isHalfOpenedSupported());
    }
}
