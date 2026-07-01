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

package androidx.appsearch.platformstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;

import androidx.annotation.OptIn;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.Features;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.platformstorage.UnsupportedPlatformConversionAdapter;
import androidx.appsearch.testutil.TestPlatformConversionAdapter;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@OptIn(markerClass = ExperimentalAppSearchApi.class)
public class SetSchemaRequestToPlatformConverterTest {
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void testSetSchemasWipeoutAccountPropertyPaths() {
        assumeTrue(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SET_SCHEMA_REQUEST_SET_WIPEOUT_ACCOUNT));
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(
                        new AppSearchSchema.DocumentPropertyConfig.Builder(
                                "prop", "builtin:Account").build())
                .build();
        AppSearchSchema accountSchema =
                new AppSearchSchema.Builder("builtin:Account").build();
        SetSchemaRequest jetpackRequest = new SetSchemaRequest.Builder()
                .addSchemas(schema, accountSchema)
                .setSchemaTypeWipeoutAccountPropertyPaths(
                        "testSchema",
                        Collections.singleton(new PropertyPath("prop")),
                        /*autoWipeout=*/true)
                .build();
        TestPlatformConversionAdapter adapter = new TestPlatformConversionAdapter();

        SetSchemaRequestToPlatformConverter.toPlatformSetSchemaRequest(
                mContext, jetpackRequest, adapter);

        Map<String, Set<String>> capturedPaths =
                adapter.getSchemasWipeoutAccountPropertyPaths();
        assertThat(capturedPaths).containsExactly("testSchema", Collections.singleton("prop"));
    }

    @Test
    public void testSetSchemasWipeoutAccountPropertyPaths_throws() {
        assumeFalse(PlatformStorage.getFeatures(mContext).isFeatureSupported(
                Features.SET_SCHEMA_REQUEST_SET_WIPEOUT_ACCOUNT));
        AppSearchSchema schema = new AppSearchSchema.Builder("testSchema")
                .addProperty(
                        new AppSearchSchema.DocumentPropertyConfig.Builder(
                                "prop", "builtin:Account").build())
                .build();
        AppSearchSchema accountSchema =
                new AppSearchSchema.Builder("builtin:Account").build();
        SetSchemaRequest jetpackRequest = new SetSchemaRequest.Builder()
                .addSchemas(schema, accountSchema)
                .setSchemaTypeWipeoutAccountPropertyPaths(
                        "testSchema",
                        Collections.singleton(new PropertyPath("prop")),
                        /*autoWipeout=*/true)
                .build();
        UnsupportedPlatformConversionAdapter adapter = new UnsupportedPlatformConversionAdapter();

        assertThrows(UnsupportedOperationException.class,
                () -> SetSchemaRequestToPlatformConverter.toPlatformSetSchemaRequest(
                        mContext, jetpackRequest, adapter));
    }
}
