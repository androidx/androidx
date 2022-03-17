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

import static org.junit.Assert.assertThrows;

import org.junit.Test;

/** Tests for private APIs of {@link GetSchemaResponse}. */
public class GetSchemaResponseInternalTest {
    // TODO(b/205749173): Expose this API and move this test to CTS. Without this API, clients can't
    //   write unit tests that check their code in an environment where visibility settings are not
    //   supported.
    @Test
    public void testRebuild_noSupportedException() {
        AppSearchSchema schema1 = new AppSearchSchema.Builder("Email1")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        GetSchemaResponse.Builder builder =
                new GetSchemaResponse.Builder(/*getVisibilitySettingSupported=*/false)
                        .setVersion(42).addSchema(schema1);

        GetSchemaResponse original = builder.build();
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getSchemaTypesNotDisplayedBySystem());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getSchemaTypesVisibleToPackages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getRequiredPermissionsForSchemaTypeVisibility());

        // rebuild will throw same exception
        GetSchemaResponse rebuild = builder.setVersion(42).build();
        assertThrows(
                UnsupportedOperationException.class,
                () -> rebuild.getSchemaTypesNotDisplayedBySystem());
        assertThrows(
                UnsupportedOperationException.class,
                () -> rebuild.getSchemaTypesVisibleToPackages());
        assertThrows(
                UnsupportedOperationException.class,
                () -> original.getRequiredPermissionsForSchemaTypeVisibility());
    }
}
