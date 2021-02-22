/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.textclassifier;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link ExtrasUtils}.
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class ExtrasUtilsTest {

    @Test
    public void testGetTopLanguage() {
        final Intent intent = ExtrasUtils.buildFakeTextClassifierIntent("ja", "en");
        assertThat(ExtrasUtils.getTopLanguage(intent).getLanguage()).isEqualTo("ja");
    }

    @Test
    public void testGetTopLanguage_differentLanguage() {
        final Intent intent = ExtrasUtils.buildFakeTextClassifierIntent("de");
        assertThat(ExtrasUtils.getTopLanguage(intent).getLanguage()).isEqualTo("de");
    }

    @Test
    public void testGetTopLanguage_nullLanguageBundle() {
        assertThat(ExtrasUtils.getTopLanguage(new Intent())).isNull();
    }

    @Test
    public void testGetTopLanguage_null() {
        assertThat(ExtrasUtils.getTopLanguage(null)).isNull();
    }
}
