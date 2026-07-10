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

package androidx.text.vertical;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

// NOTE: Do not convert this code to Kotlin -- This test suite is intentionally
// created as Java file to ensure interoperability with legacy Java code.
@RunWith(JUnit4.class)
public class AnnotationPositionJavaTest {
    @Test
    public void constants_interop_java() {
        assertThat(AnnotationPosition.Unknown.value).isEqualTo(VALUE_UNKNOWN);
        assertThat(AnnotationPosition.Before.value).isEqualTo(VALUE_BEFORE);
        assertThat(AnnotationPosition.After.value).isEqualTo(VALUE_AFTER);
    }

    @Test
    public void fromInt_interop_java() {
        assertThat(AnnotationPosition.fromInt(VALUE_UNKNOWN))
                .isEqualTo(AnnotationPosition.Unknown);
        assertThat(AnnotationPosition.fromInt(VALUE_BEFORE))
                .isEqualTo(AnnotationPosition.Before);
        assertThat(AnnotationPosition.fromInt(VALUE_AFTER))
                .isEqualTo(AnnotationPosition.After);
    }

    @Test
    public void fromInt_fallback_interop_java() {
        // Test fallback for unknown values
        assertThat(AnnotationPosition.fromInt(VALUE_OOB_LOWER))
                .isEqualTo(AnnotationPosition.Unknown);
        assertThat(AnnotationPosition.fromInt(VALUE_OOB))
                .isEqualTo(AnnotationPosition.Unknown);
    }

    private static final int VALUE_UNKNOWN = -1;
    private static final int VALUE_BEFORE = 0;
    private static final int VALUE_AFTER = 1;
    private static final int VALUE_OOB = 1_000;
    private static final int VALUE_OOB_LOWER = -1_000;
}
