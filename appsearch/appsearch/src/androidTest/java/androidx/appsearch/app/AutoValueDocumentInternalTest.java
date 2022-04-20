/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.appsearch.annotation.Document;

import com.google.auto.value.AutoValue;

import org.junit.Test;


public class AutoValueDocumentInternalTest{

    /**
     * Simple Document to demonstrate use of AutoValue and Document annotations, also nested
     */
    @Document
    @AutoValue
    public abstract static class SampleAutoValue {
        @AutoValue.CopyAnnotations @Document.Id abstract String id();
        @AutoValue.CopyAnnotations @Document.Namespace abstract String namespace();
        @AutoValue.CopyAnnotations @Document.StringProperty abstract String property();

        /** AutoValue constructor */
        public static SampleAutoValue create(String id, String namespace, String property) {
            return new AutoValue_AutoValueDocumentInternalTest_SampleAutoValue(id, namespace,
                    property);
        }
    }

    @Test
    public void testGenericDocumentConversion_AutoValue() throws Exception {
        SampleAutoValue sampleAutoValue = SampleAutoValue.create("id", "namespace", "property");
        GenericDocument genericDocument = GenericDocument.fromDocumentClass(sampleAutoValue);
        assertThat(genericDocument.getId()).isEqualTo("id");
        assertThat(genericDocument.getNamespace()).isEqualTo("namespace");
        assertThat(genericDocument.getSchemaType()).isEqualTo("SampleAutoValue");
        assertThat(genericDocument.getProperty("property")).isEqualTo(new String[]{"property"});
    }
}
