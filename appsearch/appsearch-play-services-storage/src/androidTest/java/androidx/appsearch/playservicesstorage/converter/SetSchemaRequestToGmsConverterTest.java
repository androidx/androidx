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

package androidx.appsearch.playservicesstorage.converter;

import static org.junit.Assert.assertThrows;

import androidx.annotation.OptIn;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SchemaVisibilityConfig;
import androidx.appsearch.app.SetSchemaRequest;

import org.junit.Test;

import java.util.Collections;

@OptIn(markerClass = ExperimentalAppSearchApi.class)
public class SetSchemaRequestToGmsConverterTest {
    @Test
    public void testAllowedPackageWithTags_throws() {
        SetSchemaRequest jetpackRequest = new SetSchemaRequest.Builder()
                .addSchemas(new AppSearchSchema.Builder("testSchema").build())
                .addSchemaTypeVisibleToConfig(
                        "testSchema",
                        new SchemaVisibilityConfig.Builder()
                                .setAllowedPackageWithTags(
                                        new PackageIdentifier("com.package.foo", new byte[32]),
                                        Collections.singleton("tagA"))
                                .build())
                .build();

        // The tags must not be silently dropped on a backend that can't enforce them.
        assertThrows(UnsupportedOperationException.class,
                () -> SetSchemaRequestToGmsConverter.toGmsSetSchemaRequest(jetpackRequest));
    }
}
