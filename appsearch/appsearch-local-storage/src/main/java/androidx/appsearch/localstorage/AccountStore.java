/*
 * Copyright 2025 The Android Open Source Project
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

import android.accounts.Account;
import android.util.AtomicFile;

import androidx.annotation.RestrictTo;
import androidx.annotation.VisibleForTesting;
import androidx.appsearch.app.AppSearchAccount;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PropertyPath;
import androidx.appsearch.app.SearchSpec;
import androidx.appsearch.ast.Node;
import androidx.appsearch.ast.TextNode;
import androidx.appsearch.ast.operators.AndNode;
import androidx.appsearch.ast.operators.OrNode;
import androidx.appsearch.ast.operators.PropertyRestrictNode;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.appsearch.localstorage.converter.AccountToProtoConverter;
import androidx.appsearch.localstorage.util.PrefixUtil;
import androidx.collection.ArraySet;

import com.google.android.appsearch.proto.AccountProto;
import com.google.android.icing.protobuf.CodedInputStream;
import com.google.android.icing.protobuf.CodedOutputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the persistent storage and state of all {@link Account}s known to AppSearch.
 *
 * <p>This class is responsible for reading and writing the list of AppSearch accounts to disk
 * during initialization and persistence. It ensures that account data is loaded once and only
 * updated when a structural change (add, remove, rename) is detected.
 *
 * <p>All accounts are stored in a dedicated Protobuf file ({@link #ACCOUNT_FILE_NAME}) within
 * the account directory. Persistence operations are performed atomically using a file swap
 * mechanism to prevent data corruption.
 *
 * <p>This class is not thread-safe. External synchronization is required if methods are called
 * from multiple threads concurrently.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
public class AccountStore {
    private static final String TAG = "AppSearchAccountStore";
    private static final String ACCOUNT_FILE_NAME = "accounts_file";

    @NonNull
    private final File mAccountDir;

    /**
     * The set of {@link AccountProto} objects that are actively tracked and persisted by AppSearch.
     *
     * <p>This set does not necessarily contain all existing system accounts, but only those that
     * AppSearch has interacted with and must track for persistence or special handling (e.g.,
     * accounts that need to be wiped upon removal). Since only a small subset of system accounts
     * is typically tracked and accounts are small data structures, this set is unlikely to grow
     * arbitrarily large and cause an OOM (Out-Of-Memory) issue.
     *
     * <p>NOTE: This is the only mutable member that is persisted to disk via
     * {@link #ACCOUNT_FILE_NAME}.
     */
    @VisibleForTesting
    @NonNull final Set<AccountProto> mAppSearchAccountProtos;

    /**
     * An in-memory cache of the current set of {@link Account} objects existing on the system.
     *
     * <p>This set is loaded from the system account manager and is NOT persisted to disk by
     * {@link AccountStore}. It is used to verify that an account exists before indexing.
     */
    private @Nullable Set<Account> mAllExistingAccounts;

    /**
     * Flag indicating whether the file of {@link #ACCOUNT_FILE_NAME} has been mutated (added,
     * removed, or renamed) and need to call {@link #persistToDisk()}.
     */
    @VisibleForTesting
    boolean mFileNeedsPersist = false;

    /**
     * Private constructor to initialize the {@link AccountStore}.
     *
     * @param accountDir The directory for storing account files.
     * @param appSearchAccountProtos The set of existing {@link AccountProto}s loaded from disk.
     */
    private AccountStore(@NonNull File accountDir,
            @NonNull Set<AccountProto> appSearchAccountProtos) {
        mAccountDir = accountDir;
        mAppSearchAccountProtos = appSearchAccountProtos;
    }

    /**  Get the current set of {@link Account} objects existing on the system.  */
    @Nullable
    public Set<Account> getAllExistingAccounts() {
        return mAllExistingAccounts;
    }

    /**
     * Creates and initializes an {@link AccountStore} instance.
     *
     * <p>This method creates the account directory and loads existing account data from disk,
     * if available.
     *
     * @param icingDir The root directory where Icing (AppSearch) stores its files.
     * @throws AppSearchException if reading the existing account file fails.
     */
    @NonNull
    public static AccountStore create(@NonNull File icingDir) throws AppSearchException {
        File accountDir = new File(icingDir, "account_dir");
        if (!accountDir.exists() && !accountDir.mkdirs()) {
            throw new AppSearchException(AppSearchResult.RESULT_IO_ERROR,
                    "Cannot create the account file directory: "
                            + accountDir.getAbsolutePath());
        }
        File accountFile = new File(accountDir, ACCOUNT_FILE_NAME);
        Set<AccountProto> appSearchAccountProtos = new ArraySet<>();
        if (!accountFile.exists()) {
            // Success: File does not exist, initialize empty and return.
            return new AccountStore(accountDir, appSearchAccountProtos);
        }

        AtomicFile atomicFile = new AtomicFile(accountFile);
        try (InputStream inputStream = atomicFile.openRead()) {
            CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
            while (!codedInputStream.isAtEnd()) {
                byte[] serializedMessage = codedInputStream.readByteArray();
                AccountProto accountProto = AccountProto.parseFrom(serializedMessage);
                appSearchAccountProtos.add(accountProto);
            }
        } catch (IOException e) {
            throw new AppSearchException(AppSearchResult.RESULT_IO_ERROR,
                    "Cannot read AppSearch Account file.", e);
        }
        return new AccountStore(accountDir, appSearchAccountProtos);
    }

    /**
     * Resets the {@link AccountStore} by deleting the account file from disk and clearing
     * all in-memory account data.
     *
     * <p>The file is deleted atomically using {@link AtomicFile#delete()}.
     *
     * @throws AppSearchException with {@link AppSearchResult#RESULT_IO_ERROR} if deleting the
     * file fails.
     */
    public void reset() throws AppSearchException {
        File accountFile = new File(mAccountDir, ACCOUNT_FILE_NAME);
        AtomicFile atomicFile = new AtomicFile(accountFile);

        mAppSearchAccountProtos.clear();
        mAllExistingAccounts = null;
        mFileNeedsPersist = false;

        if (accountFile.exists()) {
            atomicFile.delete();
        }
    }

    /**
     * Verifies that all {@link AppSearchAccount}s present in a {@link GenericDocument} exist
     * in the currently known set of system accounts.
     *
     * <p>If a document contains an account that is not tracked, an exception is thrown.
     *
     * <p>New, valid accounts are added to the {@code newlyAddedAccounts} set for later persistence.
     *
     * @param allExistingAccounts          The current set of {@link Account} objects existing on
     *                                     the system.
     * @param prefixedAccountPropertyPaths The map of prefixed schema types that point to all its
     *                                     account property paths
     * @param prefix                       The package name prefix for the document's schema.
     * @param document                     The {@link GenericDocument} being verified.
     * @return  A set to collect newly found, valid {@link AccountProto}s. Or {@code null} if the
     * document's schema doesn't contains wipeout account property paths or the document contains no
     * Account property.
     * @throws AppSearchException          if the account name or type is missing in the document,
     *                                     or if the account does not exist in the system.
     */
    @Nullable
    public static Set<AccountProto> verifyAccountExists(
            @NonNull Set<Account> allExistingAccounts,
            @NonNull Map<String, Set<String>> prefixedAccountPropertyPaths,
            @NonNull String prefix, @NonNull GenericDocument document)
            throws AppSearchException {
        Set<AccountProto> newlyAddedAccounts = null;
        Set<String> accountPropertyPaths = prefixedAccountPropertyPaths.get(
                prefix + document.getSchemaType());
        if (accountPropertyPaths == null) {
            // This schema type doesn't have account properties.
            return newlyAddedAccounts;
        }
        for (String accountPropertyPath : accountPropertyPaths) {
            GenericDocument[] accountDocuments =
                    document.getPropertyDocumentArray(accountPropertyPath);
            if (accountDocuments == null) {
                continue;
            }
            for (int i = 0; i < accountDocuments.length; i++) {
                AppSearchAccount appSearchAccount = new AppSearchAccount(accountDocuments[i]);
                String accountName = appSearchAccount.getAccountName();
                if (accountName == null) {
                    throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                            "The account name at " + i + " of " + accountPropertyPath
                                    + " is missing.");
                }
                String accountType = appSearchAccount.getAccountType();
                if (accountType == null) {
                    throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                            "The account type at " + i + " of " + accountPropertyPath
                                    + " is missing.");
                }
                Account account = new Account(accountName, accountType);
                if (!allExistingAccounts.contains(account)) {
                    throw new AppSearchException(AppSearchResult.RESULT_INVALID_ARGUMENT,
                            "The account at " + i +  " of " + accountPropertyPath
                                    + " doesn't exist on this device.");
                }
                if (newlyAddedAccounts == null) {
                    newlyAddedAccounts = new ArraySet<>();
                }
                newlyAddedAccounts.add(AccountToProtoConverter.toAccountProto(appSearchAccount));
            }
        }
        return newlyAddedAccounts;
    }

    /**
     * Adds a set of new {@link AccountProto}s to the internal store.
     *
     * @param newAddingAccounts The set of new {@link AccountProto}s to add.
     */
    public void putAccounts(@NonNull Set<AccountProto> newAddingAccounts) {
        if (mAppSearchAccountProtos.addAll(newAddingAccounts)) {
            mFileNeedsPersist = true;
        }
    }

    /**
     * Updates the internal list of AppSearch accounts ({@link #mAppSearchAccountProtos}) by
     * reconciling it with the current set of system accounts.
     *
     * <p>This handles three mutation cases:
     * <ul>
     * <li>**Removal:** If an account is in {@code mAppSearchAccountProtos} but not in
     * {@code allExistingAccounts}.</li>
     * <li>**Rename:** If an account is missing but found in {@code renamedAccounts},
     * the old proto is removed and a new proto with the updated name is added.</li>
     * <li>**Addition:** New accounts will be handled separately via {@link #putAccounts}.</li>
     * </ul>
     *
     * @param allExistingAccounts The complete set of current system accounts.
     * @param renamedAccounts A map of {@code <OldAccount, NewAccount>} for accounts that were
     * renamed.
     * @return A set of {@link AccountProto} objects that were removed/deleted from the system.
     * @throws AppSearchException if an IO error occurs during update or conversion.
     */
    @NonNull
    // TODO(b/413089233) handle remove and reindex document for renaming case in the following CL
    //TODO(b/413089233) add log stats for this method.
    public Set<AccountProto> updateAccounts(@NonNull Set<Account> allExistingAccounts,
            @NonNull Map<Account, Account> renamedAccounts) throws AppSearchException {
        mAllExistingAccounts = allExistingAccounts;

        // Check all App
        ArraySet<AccountProto> deletedAccounts = new ArraySet<>();
        ArraySet<AccountProto> protosToRemove = new ArraySet<>();
        ArraySet<AccountProto> protosToAdd = new ArraySet<>();
        for (AccountProto accountProto : mAppSearchAccountProtos) {
            Account account = new Account(accountProto.getAccountName(),
                    accountProto.getAccountType());
            if (!mAllExistingAccounts.contains(account)) {
                // The account is missing, check whether it is renamed or removed.
                if (renamedAccounts.containsKey(account)) {
                    // Renamed, we passed account.equals(renamedAccount.get(account.name)),
                    // this will never be null.
                    String newName = renamedAccounts.get(account).name;
                    AccountProto newProto = AccountProto.newBuilder()
                            .setAccountName(newName)
                            .setAccountType(accountProto.getAccountType())
                            .setAccountId(accountProto.getAccountId())
                            .build();
                    protosToAdd.add(newProto);
                } else {
                    // Removed
                    deletedAccounts.add(accountProto);
                }
                // we need to remove the old account proto for both cases.
                protosToRemove.add(accountProto);
            }
        }
        if (!protosToAdd.isEmpty() || !protosToRemove.isEmpty()) {
            // Perform the removals first, then perform the additions.
            mAppSearchAccountProtos.removeAll(protosToRemove);
            mAppSearchAccountProtos.addAll(protosToAdd);
            mFileNeedsPersist = true;
        }
        return deletedAccounts;
    }

    /**
     * Removes documents from AppSearch's index that reference accounts which have been deleted
     * in the system.
     *
     * <p>This method iterates through all known schema types that contain an account property.
     * For each such schema, it constructs a specialized query and executes removeByQuery to
     * efficiently delete the affected documents.
     *
     * <p>Documents are targeted based on matching the {@code accountType} and either the
     * {@code accountId} (preferred) or the old {@code accountName} of the deleted accounts.
     *
     * @param prefixedAccountPropertyPaths The map of prefixed schema types that point to all its
     *                                     account property paths
     * @param appSearchImpl                The implementation core of AppSearch used to execute the
     *                                     deletion.
     * @param deletedAccounts              The set of {@link AccountProto} objects representing
     *                                     accounts that are no longer present in the system.
     * @throws AppSearchException if an error occurs during the {@code removeByQuery} operation.
     */
    //TODO(b/413089233) add log stats for this method.
    public void removeAccountDocuments(
            @NonNull Map<String, Set<String>> prefixedAccountPropertyPaths,
            @NonNull AppSearchImpl appSearchImpl,
            @NonNull Set<AccountProto> deletedAccounts) throws AppSearchException {
        if (deletedAccounts.isEmpty() || prefixedAccountPropertyPaths.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Set<String>> entry : prefixedAccountPropertyPaths.entrySet()) {
            if (entry.getValue().isEmpty()) {
                // This should never happened, we should already move empty set from the
                // prefixedAccountPropertyPaths. In case we didn't something wrong, we should avoid
                // to trigger a removal by empty query.
                continue;
            }
            String prefixSchemaType = entry.getKey();
            SearchSpec searchSpec = new SearchSpec.Builder()
                    .addFilterSchemas(PrefixUtil.removePrefix(entry.getKey()))
                    .setVerbatimSearchEnabled(true)
                    .setTermMatch(SearchSpec.TERM_MATCH_EXACT_ONLY)
                    .build();
            String query = buildRemovalQuery(deletedAccounts, entry.getValue());
            if (query.isEmpty()) {
                // This should never happened, In case we didn't something wrong, we should avoid
                // to trigger a removal by empty query.
                throw new AppSearchException(AppSearchResult.RESULT_INTERNAL_ERROR,
                        "Built an empty removal query");
            }
            appSearchImpl.removeByQuery(PrefixUtil.getPackageName(prefixSchemaType),
                    PrefixUtil.getDatabaseName(prefixSchemaType),
                    query,
                    searchSpec,
                    /*deletedIds=*/null,
                    /*removeStatsBuilder=*/null,
                    /*callStatsBuilder=*/null);
        }
    }

    /**
     * Constructs a verbatim search query string used by {@code removeByQuery} to identify
     * documents that reference the given set of deleted accounts.
     *
     * @param deletedAccounts The set of deleted {@link AccountProto}, used to identify reference
     *                        documents.
     * @param accountPropertyPaths A set of account property paths within the current schema type.
     * @return A search query string that, when executed, will match documents referencing
     * any of the {@code deletedAccounts}.
     */
    @VisibleForTesting
    static String buildRemovalQuery(@NonNull Set<AccountProto> deletedAccounts,
            @NonNull Set<String> accountPropertyPaths) {
        if (deletedAccounts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot build remove query if the deletedAccount is empty.");
        }
        if (accountPropertyPaths.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot build remove query if the accountPropertyPaths is empty.");
        }
        Node finalNode = null;
        for (AccountProto deletedAccount : deletedAccounts) {
            Node perAccountNode;
            for (String accountPropertyPath : accountPropertyPaths) {
                TextNode accountType = new TextNode(deletedAccount.getAccountType());
                accountType.setVerbatim(true);
                perAccountNode = new PropertyRestrictNode(
                        getFullAccountPropertyPath(accountPropertyPath,
                                AppSearchAccount.PROPERTY_ACCOUNT_TYPE),
                        accountType);
                // Use the Id to remove account first since account name may be changed.
                if (deletedAccount.hasAccountId()) {
                    TextNode accountId = new TextNode(deletedAccount.getAccountId());
                    accountId.setVerbatim(true);
                    perAccountNode = new AndNode(perAccountNode, new PropertyRestrictNode(
                            getFullAccountPropertyPath(accountPropertyPath,
                                    AppSearchAccount.PROPERTY_ACCOUNT_ID),
                            accountId));
                } else {
                    TextNode accountName = new TextNode(deletedAccount.getAccountName());
                    accountName.setVerbatim(true);
                    perAccountNode = new AndNode(perAccountNode, new PropertyRestrictNode(
                            getFullAccountPropertyPath(accountPropertyPath,
                                    AppSearchAccount.PROPERTY_ACCOUNT_ID),
                            accountName));
                }
                if (finalNode == null) {
                    finalNode = perAccountNode;
                } else {
                    finalNode = new OrNode(finalNode, perAccountNode);
                }
            }
        }
        // Both deletedAccount and accountPropertyPaths are not empty, finalNode should never be
        // null.
        return Objects.requireNonNull(finalNode).toString();
    }

    private static PropertyPath getFullAccountPropertyPath(@NonNull String accountPropertyPath,
            @NonNull String propertyName) {
        return new PropertyPath(accountPropertyPath + "." + propertyName);
    }

    /**
     * Persists the current state of {@link #mAppSearchAccountProtos} to disk.
     *
     * <p>This operation is skipped if no mutations have occurred since the last call.
     * Persistence uses a temporary file and an atomic swap to ensure data integrity.
     *
     * @throws AppSearchException with {@link AppSearchResult#RESULT_IO_ERROR} if writing to disk
     * or swapping files fails.
     */
    public void persistToDisk() throws AppSearchException {
        if (!mFileNeedsPersist) {
            return;
        }
        File accountFile = new File(mAccountDir, ACCOUNT_FILE_NAME);
        AtomicFile atomicFile = new AtomicFile(accountFile);

        FileOutputStream outputStream = null;
        try {
            // 1. Start atomic write, returns FileOutputStream to write to the temporary file
            outputStream = atomicFile.startWrite();
            CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputStream);

            // 2. Write data
            for (AccountProto proto : mAppSearchAccountProtos) {
                byte[] serializedMessage = proto.toByteArray();
                codedOutputStream.writeByteArrayNoTag(serializedMessage);
            }
            codedOutputStream.flush();

            // 3. Commit atomic write (renames temp file to accountFile)
            atomicFile.finishWrite(outputStream);
            // Prevent closing a stream that was already finished/closed by AtomicFile
            outputStream = null;
        } catch (IOException e) {
            // 4. Fail and roll back if error occurred
            if (outputStream != null) {
                atomicFile.failWrite(outputStream);
            }
            throw new AppSearchException(AppSearchResult.RESULT_IO_ERROR,
                    "Failed to persist accounts to disk.", e);
        }
        mFileNeedsPersist = false;
    }
}
