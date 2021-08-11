/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GetSchemaResponse;

import org.junit.Test;

public class GetSchemaResponseCtsTest {
    @Test
    public void testRebuild() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();
        AppSearchSchema schema2 = new AppSearchSchema.Builder("Email2")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        GetSchemaResponse.Builder builder =
                new GetSchemaResponse.Builder().setVersion(42).addSchema(schema1);

        GetSchemaResponse original = builder.build();
        GetSchemaResponse rebuild = builder.setVersion(37).addSchema(schema2).build();

        // rebuild won't effect the original object
        assertThat(original.getVersion()).isEqualTo(42);
        assertThat(original.getSchemas()).containsExactly(schema1);

        assertThat(rebuild.getVersion()).isEqualTo(37);
        assertThat(rebuild.getSchemas()).containsExactly(schema1, schema2);
    }
}
