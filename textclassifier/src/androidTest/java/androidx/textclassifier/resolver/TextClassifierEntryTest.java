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

package androidx.textclassifier.resolver;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;

import org.junit.Test;

@SmallTest
public class TextClassifierEntryTest {

    @Test
    public void testCreateAospEntry() {
        TextClassifierEntry textClassifierEntry = TextClassifierEntry.createAospEntry();
        assertTrue(textClassifierEntry.isAosp());
        assertFalse(textClassifierEntry.isOem());
    }

    @Test
    public void testCreateOemEntry() {
        TextClassifierEntry textClassifierEntry = TextClassifierEntry.createOemEntry();
        assertTrue(textClassifierEntry.isOem());
        assertFalse(textClassifierEntry.isAosp());
    }

    @Test
    public void testCreatePackageEntry() {
        final String packageName = "xxx";
        final String cert = "yyy";
        TextClassifierEntry textClassifierEntry =
                TextClassifierEntry.createPackageEntry(packageName, cert);
        assertThat(textClassifierEntry.packageName).isEqualTo(packageName);
        assertThat(textClassifierEntry.certificate).isEqualTo(cert);
        assertFalse(textClassifierEntry.isOem());
        assertFalse(textClassifierEntry.isAosp());
    }
}
