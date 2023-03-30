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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.PackageIdentifier;
import androidx.appsearch.app.VisibilityDocument;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.AppSearchImpl;
import androidx.appsearch.localstorage.JetpackIcingOptionsConfig;
import androidx.appsearch.localstorage.OptimizeStrategy;
import androidx.appsearch.localstorage.UnlimitedLimitConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Collections;

public class VisibilityStoreTest {

    /**
     * Always trigger optimize in this class. OptimizeStrategy will be tested in its own test class.
     */
    private static final OptimizeStrategy ALWAYS_OPTIMIZE = optimizeInfo -> true;
    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    private File mAppSearchDir;
    private AppSearchImpl mAppSearchImpl;
    private VisibilityStore mVisibilityStore;

    @Before
    public void setUp() throws Exception {
        mAppSearchDir = mTemporaryFolder.newFolder();
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                new UnlimitedLimitConfig(),
                new JetpackIcingOptionsConfig(),
                /*initStatsBuilder=*/ null,
                ALWAYS_OPTIMIZE,
                /*visibilityChecker=*/null);
        mVisibilityStore = new VisibilityStore(mAppSearchImpl);
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    /**
     * Make sure that we don't conflict with any special characters that AppSearchImpl has reserved.
     */
    @Test
    public void testValidPackageName() {
        assertThat(VisibilityStore.VISIBILITY_PACKAGE_NAME)
                .doesNotContain(String.valueOf(PrefixUtil.PACKAGE_DELIMITER));
        assertThat(VisibilityStore.VISIBILITY_PACKAGE_NAME)
                .doesNotContain(String.valueOf(PrefixUtil.DATABASE_DELIMITER));
    }

    /**
     * Make sure that we don't conflict with any special characters that AppSearchImpl has reserved.
     */
    @Test
    public void testValidDatabaseName() {
        assertThat(VisibilityStore.VISIBILITY_DATABASE_NAME)
                .doesNotContain(String.valueOf(PrefixUtil.PACKAGE_DELIMITER));
        assertThat(VisibilityStore.VISIBILITY_DATABASE_NAME)
                .doesNotContain(String.valueOf(PrefixUtil.DATABASE_DELIMITER));
    }

    @Test
    public void testSetAndGetVisibility() throws Exception {
        String prefix = PrefixUtil.createPrefix("packageName", "databaseName");
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();
        mVisibilityStore.setVisibility(ImmutableList.of(visibilityDocument));

        assertThat(mVisibilityStore.getVisibility(prefix + "Email"))
                .isEqualTo(visibilityDocument);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ prefix + "Email",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument).isEqualTo(visibilityDocument);
    }

    @Test
    public void testRemoveVisibility() throws Exception {
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder("Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();
        mVisibilityStore.setVisibility(ImmutableList.of(visibilityDocument));

        assertThat(mVisibilityStore.getVisibility("Email"))
                .isEqualTo(visibilityDocument);
        // Verify the VisibilityDocument is saved to AppSearchImpl.
        VisibilityDocument actualDocument =  new VisibilityDocument(mAppSearchImpl.getDocument(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                VisibilityDocument.NAMESPACE,
                /*id=*/ "Email",
                /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(actualDocument).isEqualTo(visibilityDocument);

        mVisibilityStore.removeVisibility(ImmutableSet.of(visibilityDocument.getId()));
        assertThat(mVisibilityStore.getVisibility("Email")).isNull();
        // Verify the VisibilityDocument is removed from AppSearchImpl.
        AppSearchException e = assertThrows(AppSearchException.class,
                () -> mAppSearchImpl.getDocument(
                        VisibilityStore.VISIBILITY_PACKAGE_NAME,
                        VisibilityStore.VISIBILITY_DATABASE_NAME,
                        VisibilityDocument.NAMESPACE,
                        /*id=*/ "Email",
                        /*typePropertyPaths=*/ Collections.emptyMap()));
        assertThat(e).hasMessageThat().contains(
                "Document (VS#Pkg$VS#Db/, Email) not found.");
    }

    @Test
    public void testRecoverBrokenVisibilitySchema() throws Exception {
        // Create a broken schema which could be recovered to the latest schema in a compatible
        // change. Since we won't set force override to true to recover the broken case.
        AppSearchSchema brokenSchema = new AppSearchSchema.Builder(VisibilityDocument.SCHEMA_TYPE)
                .build();

        // Index a broken schema into AppSearch, use the latest version to make it broken.
        InternalSetSchemaResponse internalSetSchemaResponse = mAppSearchImpl.setSchema(
                VisibilityStore.VISIBILITY_PACKAGE_NAME,
                VisibilityStore.VISIBILITY_DATABASE_NAME,
                Collections.singletonList(brokenSchema),
                /*visibilityDocuments=*/ Collections.emptyList(),
                /*forceOverride=*/ true,
                /*version=*/ VisibilityDocument.SCHEMA_VERSION_LATEST,
                /*setSchemaStatsBuilder=*/ null);
        assertThat(internalSetSchemaResponse.isSuccess()).isTrue();
        // Create VisibilityStore should recover the broken schema
        mVisibilityStore = new VisibilityStore(mAppSearchImpl);

        // We should be able to set and get Visibility settings.
        String prefix = PrefixUtil.createPrefix("packageName", "databaseName");
        VisibilityDocument visibilityDocument = new VisibilityDocument.Builder(prefix + "Email")
                .setNotDisplayedBySystem(true)
                .addVisibleToPackage(new PackageIdentifier("pkgBar", new byte[32]))
                .build();
        mVisibilityStore.setVisibility(ImmutableList.of(visibilityDocument));

        assertThat(mVisibilityStore.getVisibility(prefix + "Email"))
                .isEqualTo(visibilityDocument);
    }
}
