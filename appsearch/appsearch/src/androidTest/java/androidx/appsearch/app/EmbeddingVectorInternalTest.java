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

package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Test;

/** Tests for private/restricted APIs of {@link EmbeddingVector}. */
public class EmbeddingVectorInternalTest {

    /**
     * Tests that the 3-argument internal constructor accepts both non-null values and quantized
     * data (which happens during SafeParcelable deserialization).
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testConstructor_bothValuesAndQuantizedData() {
        float[] values = new float[] {0.0f};
        String modelSignature = "model_v1";
        EmbeddingVector.QuantizedData quantizedData =
                new EmbeddingVector.QuantizedData(0.0f, 1.0f, new byte[] {1, 2, 3});

        EmbeddingVector vector = new EmbeddingVector(values, modelSignature, quantizedData);

        assertThat(vector.getValues()).isEqualTo(values);
        assertThat(vector.getModelSignature()).isEqualTo(modelSignature);
        assertThat(vector.getQuantizedData()).isEqualTo(quantizedData);
    }

    /**
     * Tests that passing both null values and null quantized data to the 3-argument internal
     * constructor throws an IllegalArgumentException.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testConstructor_bothNullValuesAndQuantizedData_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new EmbeddingVector(
                        /* values= */ null,
                        "model_v1",
                        /* quantizedData= */ null));
    }
}
