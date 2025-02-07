/*
 * Copyright 2020 The Android Open Source Project
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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.os.Build;

import androidx.appsearch.app.AppSearchSchema.PropertyConfig;
import androidx.appsearch.app.AppSearchSchema.StringPropertyConfig;
import androidx.appsearch.flags.Flags;
import androidx.appsearch.platformstorage.PlatformStorage;
import androidx.appsearch.platformstorage.util.AppSearchVersionUtil;
import androidx.appsearch.testutil.flags.RequiresFlagsEnabled;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SdkSuppress;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
public class AppSearchSessionPlatformInternalTest extends AppSearchSessionInternalTestBase {
    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(@NonNull String dbName) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName).build());
    }

    @Override
    protected ListenableFuture<AppSearchSession> createSearchSessionAsync(
            @NonNull String dbName, @NonNull ExecutorService executor) {
        Context context = ApplicationProvider.getApplicationContext();
        return PlatformStorage.createSearchSessionAsync(
                new PlatformStorage.SearchContext.Builder(context, dbName)
                        .setWorkerExecutor(executor).build());
    }

    // TODO(b/384947619): move delete propagation tests back to AppSearchSessionCtsTestBase once the
    //   API is ready.
    @Test
    @RequiresFlagsEnabled(Flags.FLAG_ENABLE_DELETE_PROPAGATION_TYPE)
    public void testGetSchema_deletePropagationTypePropagateFrom_notSupported() throws Exception {
        assumeTrue(mDb1.getFeatures().isFeatureSupported(Features.JOIN_SPEC_AND_QUALIFIED_ID));
        assumeFalse(mDb1.getFeatures().isFeatureSupported(
                Features.SCHEMA_STRING_PROPERTY_CONFIG_DELETE_PROPAGATION_TYPE_PROPAGATE_FROM));

        AppSearchSchema inSchema = new AppSearchSchema.Builder("Test")
                .addProperty(new StringPropertyConfig.Builder("qualifiedId")
                        .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(StringPropertyConfig.JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .setDeletePropagationType(
                                StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM)
                        .build()
                ).build();

        SetSchemaRequest request = new SetSchemaRequest.Builder()
                .addSchemas(inSchema).build();

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () ->
                mDb1.setSchemaAsync(request).get());
        assertThat(e.getMessage()).isEqualTo(
                "StringPropertyConfig.DELETE_PROPAGATION_TYPE_PROPAGATE_FROM is not supported on "
                        + "this AppSearch implementation.");
    }

    @Override
    public void testQuery_genericDocumentWrapsParentTypeForPolymorphism() throws Exception {
        // TODO(b/371610934): Enable this test for B devices once we are able to call
        //  SearchResult#getParentTypeMap in platform storage to hook up the parent information.
        assumeFalse(AppSearchVersionUtil.isAtLeastB());
        super.testQuery_genericDocumentWrapsParentTypeForPolymorphism();
    }
}
