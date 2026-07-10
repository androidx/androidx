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

package androidx.appsearch.cts.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;

import org.junit.Test;

public class EmbeddingVectorCtsTest {

    /** Tests that an EmbeddingVector can be created with standard float values. */
    @Test
    public void testConstructor_floats() {
        float[] values = new float[] {0.1f, 0.2f, 0.3f};
        String modelSignature = "model_v1";

        EmbeddingVector vector = new EmbeddingVector(values, modelSignature);

        assertThat(vector.getValues()).isEqualTo(values);
        assertThat(vector.getModelSignature()).isEqualTo(modelSignature);
    }

    /**
     * Tests that an EmbeddingVector can be created with pre-quantized values and packs them
     * correctly.
     */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testCreateQuantized_preQuantized() {
        String modelSignature = "model_v1";
        float minValue = -1.0f;
        float scale = 0.01f;
        byte[] quantizedValues = new byte[] {(byte) 0, (byte) 127, (byte) 255};

        EmbeddingVector vector =
                new EmbeddingVector(
                        new EmbeddingVector.QuantizedData(minValue, scale, quantizedValues),
                        modelSignature);

        assertThat(vector.getModelSignature()).isEqualTo(modelSignature);
        assertThat(vector.getValues()).isEqualTo(new float[] {0.0f});
        assertThat(vector.getQuantizedData()).isNotNull();
        assertThat(vector.getQuantizedData().getMinValue()).isEqualTo(minValue);
        assertThat(vector.getQuantizedData().getScale()).isEqualTo(scale);
        assertThat(vector.getQuantizedData().getQuantizedValues()).isEqualTo(quantizedValues);
    }

    /** Tests that getQuantizedData returns correct values for quantized embeddings. */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testGetQuantizedData() {
        String modelSignature = "model_v1";
        float minValue = -1.0f;
        float scale = 0.01f;
        byte[] quantizedValues = new byte[] {(byte) 0, (byte) 127, (byte) 255};

        EmbeddingVector vector =
                new EmbeddingVector(
                        new EmbeddingVector.QuantizedData(minValue, scale, quantizedValues),
                        modelSignature);

        EmbeddingVector.QuantizedData data = vector.getQuantizedData();
        assertThat(data).isNotNull();
        assertThat(data.getMinValue()).isEqualTo(minValue);
        assertThat(data.getScale()).isEqualTo(scale);
        assertThat(data.getQuantizedValues()).isEqualTo(quantizedValues);
    }

    /** Tests that getQuantizedData returns null for standard float embeddings. */
    @Test
    public void testGetQuantizedData_returnsNullForFloats() {
        float[] values = new float[] {0.1f, 0.2f, 0.3f};
        String modelSignature = "model_v1";

        EmbeddingVector vector = new EmbeddingVector(values, modelSignature);

        assertThat(vector.getQuantizedData()).isNull();
    }

    /** Tests that creating an EmbeddingVector with empty quantized data throws an exception. */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testCreateQuantized_emptyPreQuantizedData_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new EmbeddingVector(
                                new EmbeddingVector.QuantizedData(-1.0f, 0.01f, new byte[0]),
                                "model_v1"));
    }

    /** Tests the static factory method for creating quantized embeddings from floats. */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testFromUnquantizedValues() {
        float[] values = new float[] {1.0f, 2.0f, 3.0f};
        String modelSignature = "model_v1";

        EmbeddingVector vector =
                new EmbeddingVector(
                        EmbeddingVector.QuantizedData.fromUnquantizedValues(values),
                        modelSignature);

        assertThat(vector.getModelSignature()).isEqualTo(modelSignature);
        assertThat(vector.getValues()).isEqualTo(new float[] {0.0f});
        assertThat(vector.getQuantizedData()).isNotNull();
        assertThat(vector.getQuantizedData().getMinValue()).isEqualTo(1.0f);
        assertThat(vector.getQuantizedData().getScale()).isEqualTo(127.5f);
        assertThat(vector.getQuantizedData().getQuantizedValues())
                .isEqualTo(new byte[]{(byte) 0, (byte) 128, (byte) 255});
    }

    /** Tests creating quantized embeddings when all values are identical (prevents div by zero). */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testFromUnquantizedValues_allIdenticalValues() {
        float[] values = new float[] {2.5f, 2.5f};
        String modelSignature = "model_v1";

        EmbeddingVector vector =
                new EmbeddingVector(
                        EmbeddingVector.QuantizedData.fromUnquantizedValues(values),
                        modelSignature);

        assertThat(vector.getModelSignature()).isEqualTo(modelSignature);
        assertThat(vector.getValues()).isEqualTo(new float[] {0.0f});
        assertThat(vector.getQuantizedData()).isNotNull();
        assertThat(vector.getQuantizedData().getMinValue()).isEqualTo(2.5f);
        assertThat(vector.getQuantizedData().getScale()).isEqualTo(0.0f);
        assertThat(vector.getQuantizedData().getQuantizedValues())
                .isEqualTo(new byte[]{(byte) 0, (byte) 0});
    }

    /** Tests equals and hashCode contracts for standard float embeddings. */
    @Test
    public void testEqualsAndHashCode() {
        float[] values = new float[] {0.1f, 0.2f};
        String modelSignature = "model_v1";

        EmbeddingVector vector1 = new EmbeddingVector(values, modelSignature);
        EmbeddingVector vector2 = new EmbeddingVector(values, modelSignature);

        EmbeddingVector vector3 = new EmbeddingVector(new float[] {0.3f, 0.4f}, modelSignature);
        EmbeddingVector vector4 = new EmbeddingVector(values, "model_v2");

        assertThat(vector1).isEqualTo(vector2);
        assertThat(vector1.hashCode()).isEqualTo(vector2.hashCode());

        assertThat(vector1).isNotEqualTo(vector3);
        assertThat(vector1.hashCode()).isNotEqualTo(vector3.hashCode());

        assertThat(vector1).isNotEqualTo(vector4);
        assertThat(vector1.hashCode()).isNotEqualTo(vector4.hashCode());
    }

    /** Tests equals and hashCode contracts for quantized embeddings. */
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_EMBEDDING_PRE_QUANTIZED_DATA)
    public void testEqualsAndHashCode_quantized() {
        String modelSignature = "model_v1";
        byte[] quantizedValues = new byte[] {1, 2, 3};

        EmbeddingVector vector1 =
                new EmbeddingVector(
                        new EmbeddingVector.QuantizedData(0.0f, 1.0f, quantizedValues),
                        modelSignature);
        EmbeddingVector vector2 =
                new EmbeddingVector(
                        new EmbeddingVector.QuantizedData(0.0f, 1.0f, quantizedValues),
                        modelSignature);

        EmbeddingVector vector3 =
                new EmbeddingVector(
                        new EmbeddingVector.QuantizedData(1.0f, 1.0f, quantizedValues),
                        modelSignature);
        EmbeddingVector vector4 =
                new EmbeddingVector(
                        new EmbeddingVector.QuantizedData(0.0f, 1.0f, quantizedValues), "model_v2");

        assertThat(vector1).isEqualTo(vector2);
        assertThat(vector1.hashCode()).isEqualTo(vector2.hashCode());

        assertThat(vector1).isNotEqualTo(vector3);
        assertThat(vector1.hashCode()).isNotEqualTo(vector3.hashCode());

        assertThat(vector1).isNotEqualTo(vector4);
        assertThat(vector1.hashCode()).isNotEqualTo(vector4.hashCode());
    }
}
