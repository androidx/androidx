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

package androidx.appsearch.localstorage.visibilitystore;

import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV1.DEPRECATED_ROLE_ASSISTANT;
import static androidx.appsearch.localstorage.visibilitystore.VisibilityStoreMigrationHelperFromV1.DEPRECATED_ROLE_HOME;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.SetSchemaRequest;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.JetpackIcingOptionsConfig;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

public class VisibilityStoreMigrationHelperFromV1Test {

    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mFile;

    @Before
    public void setUp() throws Exception {
        // Give ourselves global query permissions
        mFile = mTemporaryFolder.newFolder();
    }

    @Test
    public void testVisibilityMigration_from1() throws Exception {
        // Values for a "foo" client
        String packageNameFoo = "packageFoo";
        byte[] sha256CertFoo = new byte[32];

        // Values for a "bar" client
        String packageNameBar = "packageBar";
        byte[] sha256CertBar = new byte[32];

        // Create AppSearchImpl with visibility document version 1;
        AppSearchImpl appSearchImplInV1 = AppSearchImpl.create(mFile, new UnlimitedLimitConfig(),
                new JetpackIcingOptionsConfig(), /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        InternalSetSchemaResponse internalSetSchemaResponse = appSearchImplInV1.setSchema(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                ImmutableList.of(VisibilityDocumentV1.SCHEMA),
                /*prefixedVisibilityBundles=*/ Collections.emptyList(),
                /*forceOverride=*/ true, // force push the old version into disk
                /*version=*/ 1,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        // Build deprecated visibility documents in version 1
        String prefix = PrefixUtil.createPrefix("package", "database");
        VisibilityDocumentV1 visibilityDocumentV1 =
                new VisibilityDocumentV1.Builder(prefix + "Schema")
                        .setNotDisplayedBySystem(true)
                        .addVisibleToPackage(new PackageIdentifier(packageNameFoo, sha256CertFoo))
                        .addVisibleToPackage(new PackageIdentifier(packageNameBar, sha256CertBar))
                        .setVisibleToRoles(ImmutableSet.of(DEPRECATED_ROLE_HOME,
                                DEPRECATED_ROLE_ASSISTANT))
                        .setVisibleToPermissions(ImmutableSet.of(SetSchemaRequest.READ_SMS,
                                SetSchemaRequest.READ_CALENDAR))
                        .build();

        // Set client schema into AppSearchImpl with empty VisibilityDocument since we need to
        // directly put old version of VisibilityDocument.
        internalSetSchemaResponse = appSearchImplInV1.setSchema(
                "package",
                "database",
                ImmutableList.of(
                        new AppSearchSchema.Builder("Schema").build()),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ false,
                /*schemaVersion=*/ 0,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();

        // Put deprecated visibility documents in version 0 to AppSearchImpl
        appSearchImplInV1.putDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                visibilityDocumentV1,
                /*sendChangeNotifications=*/ false,
                /*logger=*/null);

        // Persist to disk and re-open the AppSearchImpl
        appSearchImplInV1.close();
        AppSearchImpl appSearchImpl = AppSearchImpl.create(mFile, new UnlimitedLimitConfig(),
                new JetpackIcingOptionsConfig(), /*initStatsBuilder=*/ null, ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);

        VisibilityDocument actualDocument = new VisibilityDocument(
                appSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                        VisibilityDocument.NAMESPACE,
                        /*id=*/ prefix + "Schema",
                        /*typePropertyPaths=*/ Collections.emptyMap()));

        assertThat(actualDocument.isNotDisplayedBySystem()).isTrue();
        assertThat(actualDocument.getPackageNames()).asList().containsExactly(packageNameFoo,
                packageNameBar);
        assertThat(actualDocument.getSha256Certs()).isEqualTo(
                new byte[][] {sha256CertFoo, sha256CertBar});
        assertThat(actualDocument.getVisibleToPermissions()).containsExactlyElementsIn(
                ImmutableSet.of(
                        ImmutableSet.of(SetSchemaRequest.READ_SMS, SetSchemaRequest.READ_CALENDAR),
                        ImmutableSet.of(SetSchemaRequest.READ_HOME_APP_SEARCH_DATA),
                        ImmutableSet.of(SetSchemaRequest.READ_ASSISTANT_APP_SEARCH_DATA)));
        appSearchImpl.close();
    }
}
