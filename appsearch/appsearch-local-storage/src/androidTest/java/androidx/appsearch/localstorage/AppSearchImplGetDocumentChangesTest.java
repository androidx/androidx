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

package androidx.appsearch.localstorage;

import static androidx.appsearch.testutil.AppSearchTestUtils.createMockVisibilityChecker;

import static com.google.common.truth.Truth.assertThat;

import androidx.annotation.OptIn;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.InternalSetSchemaResponse;
import androidx.appsearch.app.InternalVisibilityConfig;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.appsearch.localstorage.visibilitystore.CallerAccess;
import androidx.appsearch.localstorage.visibilitystore.VisibilityChecker;
import androidx.appsearch.observer.DocumentChangeResponse;
import androidx.appsearch.observer.DocumentChangeSet;
import androidx.appsearch.observer.ObserverSpec;
import androidx.appsearch.testutil.AppSearchTestUtils;

import com.google.android.icing.IcingSearchEngine;
import com.google.android.icing.proto.DocumentChangeResponseProto;
import com.google.android.icing.proto.GetDocumentChangesRequestProto;
import com.google.android.icing.proto.IcingSearchEngineOptions;
import com.google.android.icing.proto.StatusProto;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tests for {@link AppSearchImpl#getDocumentChanges}. */
@OptIn(markerClass = ExperimentalAppSearchApi.class)
@SuppressWarnings("deprecation")
public class AppSearchImplGetDocumentChangesTest {
    private static final String PACKAGE_1 = "package1";
    private static final String PACKAGE_2 = "package2";
    private static final String OTHER_PACKAGE = "otherPackage";
    private static final String DATABASE_1 = "database1";
    private static final String DATABASE_2 = "database2";
    private static final String NAMESPACE = "namespace";
    private static final String TYPE_1 = "Type1";
    private static final String TYPE_2 = "Type2";

    private static final DocumentChangeResponse.PaginationToken START_TOKEN =
            new DocumentChangeResponse.PaginationToken(/*lastSyncTimestampMillis=*/ 0);

    @Rule
    public final RuleChain mRuleChain = AppSearchTestUtils.createCommonTestRules();

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();

    private final AppSearchConfig mConfig = new AppSearchConfigImpl(
            new UnlimitedLimitConfig(), new LocalStorageIcingOptionsConfig());

    private File mAppSearchDir;
    private AppSearchImpl mAppSearchImpl;
    private TestIcingSearchEngine mIcingSearchEngine;

    /**
     * An {@link IcingSearchEngine} that records {@link #getDocumentChanges} requests, and can
     * optionally override the response.
     */
    private static final class TestIcingSearchEngine extends IcingSearchEngine {
        final List<GetDocumentChangesRequestProto> mRequests = new ArrayList<>();
        @Nullable DocumentChangeResponseProto mResponseOverride;

        TestIcingSearchEngine(@NonNull IcingSearchEngineOptions options) {
            super(options);
        }

        @Override
        public @NonNull DocumentChangeResponseProto getDocumentChanges(
                @NonNull GetDocumentChangesRequestProto request) {
            mRequests.add(request);
            if (mResponseOverride != null) {
                return mResponseOverride;
            }
            return super.getDocumentChanges(request);
        }
    }

    @Before
    public void setUp() throws Exception {
        mAppSearchDir = mTemporaryFolder.newFolder();
        // Package1's Type1 in database1 is visible to OTHER_PACKAGE, nothing else is.
        createAppSearchImpl(createMockVisibilityChecker(
                ImmutableSet.of(PrefixUtil.createPrefix(PACKAGE_1, DATABASE_1) + TYPE_1)));
    }

    @After
    public void tearDown() {
        mAppSearchImpl.close();
    }

    private void createAppSearchImpl(@Nullable VisibilityChecker visibilityChecker)
            throws Exception {
        IcingSearchEngineOptions options = IcingSearchEngineOptions.newBuilder(
                        mConfig.toIcingSearchEngineOptions(
                                mAppSearchDir.getAbsolutePath(), /*isVmEnabled=*/ false))
                .setEnableOfflineObservers(true)
                .build();
        mIcingSearchEngine = new TestIcingSearchEngine(options);
        mAppSearchImpl = AppSearchImpl.create(
                mAppSearchDir,
                mConfig,
                new AppSearchUserPlugins.Builder()
                        .setIcingSearchEngine(mIcingSearchEngine)
                        .setVisibilityChecker(visibilityChecker)
                        .build(),
                /*optimizeStrategy=*/ optimizeInfo -> true);
    }

    private void setSchema(@NonNull String packageName, @NonNull String databaseName)
            throws Exception {
        InternalSetSchemaResponse response = mAppSearchImpl.setSchema(
                packageName,
                databaseName,
                ImmutableList.of(
                        new AppSearchSchema.Builder(TYPE_1).build(),
                        new AppSearchSchema.Builder(TYPE_2).build()),
                /*visibilityConfigs=*/ Collections.emptyList(),
                /*accountPropertyPaths=*/ ImmutableMap.of(),
                /*forceOverride=*/ false,
                /*version=*/ 0,
                /*callingUid=*/ InternalVisibilityConfig.INVALID_UID,
                /*setSchemaStatsBuilder=*/ null,
                /*callStatsBuilder=*/ null);
        assertThat(response.isSuccess()).isTrue();
    }

    private void putDocument(@NonNull String packageName, @NonNull String databaseName,
            @NonNull String id, @NonNull String schemaType) throws Exception {
        mAppSearchImpl.putDocument(
                packageName,
                databaseName,
                new GenericDocument.Builder<>(NAMESPACE, id, schemaType).build(),
                /*sendChangeNotifications=*/ false,
                /*logger=*/ null,
                /*callStatsBuilder=*/ null);
    }

    private static @NonNull List<String> getDocumentIds(@NonNull DocumentChangeSet changeSet) {
        List<String> ids = new ArrayList<>();
        for (DocumentChangeSet.ChangeToken token : changeSet.getChangeTokens()) {
            ids.add(token.getDocumentId());
        }
        return ids;
    }

    private static @Nullable DocumentChangeSet findChangeSet(
            @NonNull DocumentChangeResponse response, @NonNull String packageName,
            @NonNull String databaseName, @NonNull String schemaType) {
        for (DocumentChangeSet changeSet : response.getDocumentChangeSets()) {
            if (changeSet.getPackageName().equals(packageName)
                    && changeSet.getDatabaseName().equals(databaseName)
                    && changeSet.getSchemaName().equals(schemaType)) {
                return changeSet;
            }
        }
        return null;
    }

    @Test
    public void testGetDocumentChanges_returnsAddedDocuments() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id1", TYPE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id2", TYPE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id3", TYPE_2);

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess(PACKAGE_1));

        assertThat(response.hasMoreChanges()).isFalse();
        assertThat(response.getDocumentChangeSets()).hasSize(2);
        DocumentChangeSet type1Changes = findChangeSet(response, PACKAGE_1, DATABASE_1, TYPE_1);
        assertThat(type1Changes).isNotNull();
        assertThat(type1Changes.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(getDocumentIds(type1Changes)).containsExactly("id1", "id2").inOrder();
        DocumentChangeSet type2Changes = findChangeSet(response, PACKAGE_1, DATABASE_1, TYPE_2);
        assertThat(type2Changes).isNotNull();
        assertThat(getDocumentIds(type2Changes)).containsExactly("id3");
        assertThat(response.getNextPaginationToken().getLastSyncTimestampMillis())
                .isGreaterThan(0);
    }

    @Test
    public void testGetDocumentChanges_filtersByObserverSpec() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id1", TYPE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id2", TYPE_2);

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().addFilterSchemas(TYPE_2).build(),
                START_TOKEN, new CallerAccess(PACKAGE_1));

        assertThat(response.getDocumentChangeSets()).hasSize(1);
        DocumentChangeSet changeSet = response.getDocumentChangeSets().get(0);
        assertThat(changeSet.getSchemaName()).isEqualTo(TYPE_2);
        assertThat(getDocumentIds(changeSet)).containsExactly("id2");

        // Only the matching prefixed schema type is requested from Icing.
        GetDocumentChangesRequestProto request = mIcingSearchEngine.mRequests.get(0);
        assertThat(request.getSchemaFiltersList()).containsExactly(
                PrefixUtil.createPrefix(PACKAGE_1, DATABASE_1) + TYPE_2);
    }

    @Test
    public void testGetDocumentChanges_onlyReturnsTargetPackage() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        setSchema(PACKAGE_1, DATABASE_2);
        setSchema(PACKAGE_2, DATABASE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id1", TYPE_1);
        putDocument(PACKAGE_1, DATABASE_2, "id2", TYPE_1);
        putDocument(PACKAGE_2, DATABASE_1, "id3", TYPE_1);

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess(PACKAGE_1));

        assertThat(response.getDocumentChangeSets()).hasSize(2);
        assertThat(getDocumentIds(findChangeSet(response, PACKAGE_1, DATABASE_1, TYPE_1)))
                .containsExactly("id1");
        assertThat(getDocumentIds(findChangeSet(response, PACKAGE_1, DATABASE_2, TYPE_1)))
                .containsExactly("id2");

        // The request sent to Icing only contains package1's schema types.
        GetDocumentChangesRequestProto request = mIcingSearchEngine.mRequests.get(0);
        String package1Db1 = PrefixUtil.createPrefix(PACKAGE_1, DATABASE_1);
        String package1Db2 = PrefixUtil.createPrefix(PACKAGE_1, DATABASE_2);
        assertThat(request.getSchemaFiltersList()).containsExactly(
                package1Db1 + TYPE_1, package1Db1 + TYPE_2,
                package1Db2 + TYPE_1, package1Db2 + TYPE_2);
    }

    @Test
    public void testGetDocumentChanges_onlyReturnsVisibleSchemaTypes() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id1", TYPE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id2", TYPE_2);

        // OTHER_PACKAGE can only see package1's Type1 in database1.
        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess(OTHER_PACKAGE));

        assertThat(response.getDocumentChangeSets()).hasSize(1);
        DocumentChangeSet changeSet = response.getDocumentChangeSets().get(0);
        assertThat(changeSet.getSchemaName()).isEqualTo(TYPE_1);
        assertThat(getDocumentIds(changeSet)).containsExactly("id1");
        assertThat(mIcingSearchEngine.mRequests.get(0).getSchemaFiltersList()).containsExactly(
                PrefixUtil.createPrefix(PACKAGE_1, DATABASE_1) + TYPE_1);
    }

    @Test
    public void testGetDocumentChanges_noVisibleSchemaTypes_returnsEmpty() throws Exception {
        setSchema(PACKAGE_2, DATABASE_1);
        putDocument(PACKAGE_2, DATABASE_1, "id1", TYPE_1);

        DocumentChangeResponse.PaginationToken token =
                new DocumentChangeResponse.PaginationToken(/*lastSyncTimestampMillis=*/ 123);
        // OTHER_PACKAGE can't see any of package2's schema types.
        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_2, new ObserverSpec.Builder().build(), token,
                new CallerAccess(OTHER_PACKAGE));

        assertThat(response.getDocumentChangeSets()).isEmpty();
        assertThat(response.hasMoreChanges()).isFalse();
        assertThat(response.getNextPaginationToken()).isEqualTo(token);
        // Icing must not be called, as an empty schema filter would match all schema types.
        assertThat(mIcingSearchEngine.mRequests).isEmpty();
    }

    @Test
    public void testGetDocumentChanges_nonExistentPackage_returnsEmpty() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id1", TYPE_1);

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                "nonExistentPackage", new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess("nonExistentPackage"));

        assertThat(response.getDocumentChangeSets()).isEmpty();
        assertThat(response.hasMoreChanges()).isFalse();
        assertThat(mIcingSearchEngine.mRequests).isEmpty();
    }

    @Test
    public void testGetDocumentChanges_returnsRemovedDocuments() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id1", TYPE_1);
        putDocument(PACKAGE_1, DATABASE_1, "id2", TYPE_1);

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess(PACKAGE_1));
        assertThat(getDocumentIds(response.getDocumentChangeSets().get(0)))
                .containsExactly("id1", "id2").inOrder();

        mAppSearchImpl.remove(PACKAGE_1, DATABASE_1, NAMESPACE, "id1",
                /*removeStatsBuilder=*/ null, /*callStatsBuilder=*/ null);

        // Continue from the previous response's token. Only the removal is returned.
        response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(),
                response.getNextPaginationToken(), new CallerAccess(PACKAGE_1));
        assertThat(response.getDocumentChangeSets()).hasSize(1);
        assertThat(getDocumentIds(response.getDocumentChangeSets().get(0)))
                .containsExactly("id1");
    }

    @Test
    public void testGetDocumentChanges_preservesChronologicalOrderOfChangeTokens()
            throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        String prefix = PrefixUtil.createPrefix(PACKAGE_1, DATABASE_1);
        DocumentChangeResponseProto.DocumentChangeSetProto.ChangeToken.Builder tokenBuilder =
                DocumentChangeResponseProto.DocumentChangeSetProto.ChangeToken.newBuilder();
        // Icing returns change tokens ordered by operation timestamp, which is not the same as
        // the order of the document ids.
        mIcingSearchEngine.mResponseOverride = DocumentChangeResponseProto.newBuilder()
                .setStatus(StatusProto.newBuilder().setCode(StatusProto.Code.OK))
                .addDocumentChangeSets(DocumentChangeResponseProto.DocumentChangeSetProto
                        .newBuilder()
                        .setNameSpace(prefix + NAMESPACE)
                        .setSchema(prefix + TYPE_1)
                        .addChangeTokens(tokenBuilder.setUri("c").setOperationTimestampMs(10))
                        .addChangeTokens(tokenBuilder.setUri("a").setOperationTimestampMs(11))
                        .addChangeTokens(tokenBuilder.setUri("b").setOperationTimestampMs(12)))
                .setNextPageToken(DocumentChangeResponseProto.PaginationToken.newBuilder()
                        .setLastSyncTimestampMs(12))
                .build();

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess(PACKAGE_1));

        assertThat(response.getDocumentChangeSets()).hasSize(1);
        assertThat(response.getDocumentChangeSets().get(0).getChangeTokens()).containsExactly(
                new DocumentChangeSet.ChangeToken("c", 10),
                new DocumentChangeSet.ChangeToken("a", 11),
                new DocumentChangeSet.ChangeToken("b", 12)).inOrder();
    }

    @Test
    public void testGetDocumentChanges_filtersOutDisallowedResultsFromIcing() throws Exception {
        setSchema(PACKAGE_1, DATABASE_1);
        setSchema(PACKAGE_2, DATABASE_1);
        String package1Prefix = PrefixUtil.createPrefix(PACKAGE_1, DATABASE_1);
        String package2Prefix = PrefixUtil.createPrefix(PACKAGE_2, DATABASE_1);

        // Make Icing return changes for schema types that were not requested.
        mIcingSearchEngine.mResponseOverride = DocumentChangeResponseProto.newBuilder()
                .setStatus(StatusProto.newBuilder().setCode(StatusProto.Code.OK))
                .addDocumentChangeSets(createChangeSetProto(
                        package1Prefix + NAMESPACE, package1Prefix + TYPE_1, "id1", 10))
                // Not visible to OTHER_PACKAGE.
                .addDocumentChangeSets(createChangeSetProto(
                        package1Prefix + NAMESPACE, package1Prefix + TYPE_2, "id2", 11))
                // Belongs to a different package.
                .addDocumentChangeSets(createChangeSetProto(
                        package2Prefix + NAMESPACE, package2Prefix + TYPE_1, "id3", 12))
                // Namespace belongs to a different package than the schema type.
                .addDocumentChangeSets(createChangeSetProto(
                        package2Prefix + NAMESPACE, package1Prefix + TYPE_1, "id4", 13))
                .setHasMoreChanges(true)
                .setNextPageToken(DocumentChangeResponseProto.PaginationToken.newBuilder()
                        .setLastSyncTimestampMs(13))
                .build();

        DocumentChangeResponse response = mAppSearchImpl.getDocumentChanges(
                PACKAGE_1, new ObserverSpec.Builder().build(), START_TOKEN,
                new CallerAccess(OTHER_PACKAGE));

        assertThat(response.getDocumentChangeSets()).containsExactly(
                new DocumentChangeSet(PACKAGE_1, DATABASE_1, NAMESPACE, TYPE_1,
                        ImmutableList.of(new DocumentChangeSet.ChangeToken("id1", 10))));
        assertThat(response.hasMoreChanges()).isTrue();
        assertThat(response.getNextPaginationToken())
                .isEqualTo(new DocumentChangeResponse.PaginationToken(13));
    }

    private static DocumentChangeResponseProto.@NonNull DocumentChangeSetProto
            createChangeSetProto(@NonNull String prefixedNamespace,
            @NonNull String prefixedSchemaType, @NonNull String uri, long timestampMs) {
        return DocumentChangeResponseProto.DocumentChangeSetProto.newBuilder()
                .setNameSpace(prefixedNamespace)
                .setSchema(prefixedSchemaType)
                .addChangeTokens(
                        DocumentChangeResponseProto.DocumentChangeSetProto.ChangeToken.newBuilder()
                                .setUri(uri)
                                .setOperationTimestampMs(timestampMs))
                .build();
    }
}
