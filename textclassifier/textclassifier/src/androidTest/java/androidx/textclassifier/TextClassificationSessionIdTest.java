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

package androidx.textclassifier;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumentation unit tests for {@link TextClassificationSessionId}. */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class TextClassificationSessionIdTest {
    private static final String VALUE = "value";

    @Test
    public void testToBundle() {
        TextClassificationSessionId textClassificationSessionId =
                new TextClassificationSessionId(VALUE);
        Bundle bundle = textClassificationSessionId.toBundle();
        TextClassificationSessionId restored = TextClassificationSessionId.createFromBundle(bundle);
        assertThat(restored.flattenToString()).isEqualTo(VALUE);
    }

    @Test
    public void testFlattenToString() {
        final String value = "xxx";
        TextClassificationSessionId textClassificationSessionId =
                new TextClassificationSessionId(value);
        assertThat(textClassificationSessionId.flattenToString()).isEqualTo(value);
    }

    @Test
    public void testUnflattenFromString() {
        final String value = "xxx";
        TextClassificationSessionId textClassificationSessionId =
                TextClassificationSessionId.unflattenFromString(value);
        assertThat(textClassificationSessionId.flattenToString()).isEqualTo(value);
    }
}
