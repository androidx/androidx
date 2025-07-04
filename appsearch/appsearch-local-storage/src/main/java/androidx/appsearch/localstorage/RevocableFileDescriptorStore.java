/*
 * Copyright 2024 The Android Open Source Project
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

import android.os.ParcelFileDescriptor;


import androidx.annotation.GuardedBy;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchResult;
import androidx.appsearch.app.ExperimentalAppSearchApi;
import androidx.appsearch.exceptions.AppSearchException;
import androidx.collection.ArrayMap;
import androidx.collection.ArraySet;
import androidx.core.util.Preconditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The base class for revocable file descriptors storage.
 *
 * <p>This store allows wrapping {@link ParcelFileDescriptor} instances into revocable file
 * descriptors, enabling the ability to close and revoke it's access to the file even if the
 * {@link ParcelFileDescriptor} has been sent to the client side.
 *
 * <p> This class can provide controlled access to resources by associating each file descriptor
 * with a package and allowing them to be individually revoked by package or revoked all at once.
 *
 * <p> The sub-class must define how to wrap a {@link ParcelFileDescriptor} to a
 * {@link AppSearchRevocableFileDescriptor}.
 *
 * <p> This class stores {@link AppSearchBlobHandle} and returned
 * {@link AppSearchRevocableFileDescriptor} for writing in key-value pairs map. Only one opened
 * {@link AppSearchRevocableFileDescriptor} for writing will be allowed for each
 * {@link AppSearchBlobHandle}.
 *
 * <p> This class stores {@link AppSearchRevocableFileDescriptor} for reading in a list. There is no
 * use case to look up and invoke a single {@link AppSearchRevocableFileDescriptor} for reading.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
public abstract class RevocableFileDescriptorStore {

    private final Object mLock  = new Object();
    private final AppSearchConfig mConfig;

    public RevocableFileDescriptorStore(@NonNull AppSearchConfig config) {
        mConfig = Preconditions.checkNotNull(config);
    }

    @GuardedBy("mLock")
    // Map<package, Map<blob handle, sent rfds>> map to track all sent rfds for writing. We only
    // allow user to open 1 pfd for write for same file.
    private final Map<String, Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor>>
            mSentRevocableFileDescriptorsForWriteLocked = new ArrayMap<>();

    @GuardedBy("mLock")
    // <package, List<sent rfds> map to track all sent rfds for reading. We allow opening
    // multiple pfds for read for the same file.
    private final Map<String, List<AppSearchRevocableFileDescriptor>>
            mSentRevocableFileDescriptorsForReadLocked = new ArrayMap<>();

    /**
     * Wraps the provided {@link ParcelFileDescriptor} into a revocable file descriptor.
     *
     * <p>This allows for controlled access to the file descriptor, making it revocable by the
     * store.
     *
     * @param packageName The package name requesting the revocable file descriptor.
     * @param blobHandle The blob handle associated with the file descriptor. It cannot be null if
     *                   the mode is READ_WRITE.
     * @param parcelFileDescriptor The original {@link ParcelFileDescriptor} to be wrapped.
     * @param mode  The mode of the given {@link ParcelFileDescriptor}. It should be
     * {@link ParcelFileDescriptor#MODE_READ_ONLY} or {@link ParcelFileDescriptor#MODE_READ_WRITE}.
     * @return A {@link ParcelFileDescriptor} that can be revoked by the store.
     * @throws IOException if an I/O error occurs while creating the revocable file descriptor.
     */
    public  @NonNull ParcelFileDescriptor wrapToRevocableFileDescriptor(
            @NonNull String packageName,
            @Nullable AppSearchBlobHandle blobHandle,
            @NonNull ParcelFileDescriptor parcelFileDescriptor,
            int mode) throws IOException {
        AppSearchRevocableFileDescriptor revocableFileDescriptor =
                wrapToRevocableFileDescriptor(parcelFileDescriptor, mode);
        setCloseListenerToFd(packageName, blobHandle, revocableFileDescriptor);
        addToSentRevocableFileDescriptorMap(packageName, blobHandle,
                revocableFileDescriptor);
        return revocableFileDescriptor.getRevocableFileDescriptor();
    }

    /**
     * Wraps the provided {@link ParcelFileDescriptor} into a specific type of
     * {@link AppSearchRevocableFileDescriptor}.
     */
    protected abstract @NonNull AppSearchRevocableFileDescriptor wrapToRevocableFileDescriptor(
            @NonNull ParcelFileDescriptor parcelFileDescriptor,
            int mode) throws IOException;

    /**
     * Gets the opened revocable file descriptor for write associated with the given
     * {@link AppSearchBlobHandle}.
     *
     * @param packageName The package name associated with the file descriptor.
     * @param blobHandle The blob handle associated with the file descriptor.
     * @return The opened revocable file descriptor, or {@code null} if not found.
     */
    public @Nullable ParcelFileDescriptor getOpenedRevocableFileDescriptorForWrite(
            @NonNull String packageName,
            @NonNull AppSearchBlobHandle blobHandle) throws IOException {
        synchronized (mLock) {
            Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor> rfdsForPackage =
                    mSentRevocableFileDescriptorsForWriteLocked.get(packageName);
            if (rfdsForPackage == null) {
                return null;
            }
            AppSearchRevocableFileDescriptor revocableFileDescriptor =
                    rfdsForPackage.get(blobHandle);
            if (revocableFileDescriptor == null) {
                return null;
            }
            if (!revocableFileDescriptor.getRevocableFileDescriptor().getFileDescriptor().valid()) {
                // In Android T and below, even if the sent file descriptor is closed, the resource
                // won't be released immediately. We should revoke now and recreate new pfd.
                revocableFileDescriptor.revoke();
                rfdsForPackage.remove(blobHandle);
                return null;
            }
            // The revocableFileDescriptor should never be revoked, otherwise it should be removed
            // from the map.
            return revocableFileDescriptor.getRevocableFileDescriptor();
        }
    }

    /**
     * Revokes all revocable file descriptors previously issued by the store.
     * After calling this method, any access to these file descriptors will fail.
     *
     * @throws IOException If an I/O error occurs while revoking file descriptors.
     */
    public void revokeAll() throws IOException {
        synchronized (mLock) {
            Set<String> packageNames =
                    new ArraySet<>(mSentRevocableFileDescriptorsForReadLocked.keySet());
            packageNames.addAll(mSentRevocableFileDescriptorsForWriteLocked.keySet());
            for (String packageName : packageNames) {
                revokeForPackage(packageName);
            }
        }
    }

    /**
     * Revokes all revocable file descriptors for a specified package.
     * Only file descriptors associated with the given package name will be revoked.
     *
     * @param packageName The package name whose file descriptors should be revoked.
     * @throws IOException If an I/O error occurs while revoking file descriptors.
     */
    public void revokeForPackage(@NonNull String packageName) throws IOException {
        synchronized (mLock) {
            List<AppSearchRevocableFileDescriptor> rfdsForRead =
                    mSentRevocableFileDescriptorsForReadLocked.remove(packageName);
            if (rfdsForRead != null) {
                for (int i = rfdsForRead.size() - 1; i >= 0; i--) {
                    rfdsForRead.get(i).revoke();
                }
            }

            Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor> rfdsForWrite =
                    mSentRevocableFileDescriptorsForWriteLocked.remove(packageName);
            if (rfdsForWrite != null) {
                for (AppSearchRevocableFileDescriptor rfdForWrite : rfdsForWrite.values()) {
                    rfdForWrite.revoke();
                }
            }
        }
    }

    /**
     * Revokes the revocable file descriptors for write associated with the given
     * {@link AppSearchBlobHandle}.
     *
     * <p> Once a blob is sealed, we should call this method to revoke the sent file descriptor for
     * write. Otherwise, the user could keep writing to the committed file.
     *
     * @param packageName The package name whose file descriptors should be revoked.
     * @param blobHandle  The blob handle associated with the file descriptors.
     * @throws IOException If an I/O error occurs while revoking file descriptors.
     */
    public void revokeFdForWrite(@NonNull String packageName,
            @NonNull AppSearchBlobHandle blobHandle) throws IOException {
        synchronized (mLock) {
            Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor> rfdsForWrite =
                    mSentRevocableFileDescriptorsForWriteLocked.get(packageName);
            if (rfdsForWrite == null) {
                return;
            }
            AppSearchRevocableFileDescriptor revocableFileDescriptor =
                    rfdsForWrite.remove(blobHandle);
            if (revocableFileDescriptor == null) {
                return;
            }
            revocableFileDescriptor.revoke();
            if (rfdsForWrite.isEmpty()) {
                mSentRevocableFileDescriptorsForWriteLocked.remove(packageName);
            }
        }
    }

    /**  Checks if the specified package has reached its blob storage limit. */
    public void checkBlobStoreLimit(@NonNull String packageName) throws AppSearchException {
        synchronized (mLock) {
            int totalOpenFdSize = 0;
            List<AppSearchRevocableFileDescriptor> rfdsForRead =
                    mSentRevocableFileDescriptorsForReadLocked.get(packageName);
            if (rfdsForRead != null) {
                totalOpenFdSize += rfdsForRead.size();
            }
            Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor> rfdsForWrite =
                    mSentRevocableFileDescriptorsForWriteLocked.get(packageName);
            if (rfdsForWrite != null) {
                totalOpenFdSize += rfdsForWrite.size();
            }
            if (totalOpenFdSize >= mConfig.getMaxOpenBlobCount()) {
                throw new AppSearchException(AppSearchResult.RESULT_OUT_OF_SPACE,
                        "Package \"" + packageName + "\" exceeded limit of "
                                + mConfig.getMaxOpenBlobCount()
                                + " opened file descriptors. Some file descriptors "
                                + "must be closed to open additional ones.");
            }
        }
    }

    /**
     * Sets a close listener to the revocable file descriptor for write.
     *
     * <p>The listener will be invoked when the file descriptor is closed.
     *
     * @param packageName The package name associated with the file descriptor.
     * @param blobHandle The blob handle associated with the file descriptor. It cannot be null if
     *                   the mode is READ_WRITE.
     * @param revocableFileDescriptor The revocable file descriptor to set the listener to.
     */
    private void setCloseListenerToFd(
            @NonNull String packageName,
            @Nullable AppSearchBlobHandle blobHandle,
            @NonNull AppSearchRevocableFileDescriptor revocableFileDescriptor) {
        ParcelFileDescriptor.OnCloseListener closeListener;
        switch (revocableFileDescriptor.getMode()) {
            case ParcelFileDescriptor.MODE_READ_ONLY:
                closeListener = e -> {
                    synchronized (mLock) {
                        List<AppSearchRevocableFileDescriptor> fdsForPackage =
                                mSentRevocableFileDescriptorsForReadLocked.get(packageName);
                        if (fdsForPackage != null) {
                            fdsForPackage.remove(revocableFileDescriptor);
                            if (fdsForPackage.isEmpty()) {
                                mSentRevocableFileDescriptorsForReadLocked.remove(packageName);
                            }
                        }
                    }
                };
                break;
            case ParcelFileDescriptor.MODE_READ_WRITE:
                Preconditions.checkNotNull(blobHandle);
                closeListener = e -> {
                    synchronized (mLock) {
                        Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor>
                                rfdsForPackage = mSentRevocableFileDescriptorsForWriteLocked
                                .get(packageName);
                        if (rfdsForPackage != null) {
                            AppSearchRevocableFileDescriptor rfd =
                                    rfdsForPackage.get(blobHandle);
                            if (rfd == revocableFileDescriptor) {
                                // In Platform, this close listener will be only called
                                // when the resource of sent pfd is released in the sdk
                                // side. In T and below this may happened on a delay.
                                // If user re-write with the same blob handle before release
                                // the resource, the mSentRevocableFileDescriptorsForWrite
                                // will be override by new AppSearchRevocableFileDescriptor.
                                // We should only revoke and remove from map if there is no
                                // other rfd created for this blob handle.
                                try {
                                    rfdsForPackage.remove(blobHandle);
                                    rfd.revoke();
                                } catch (IOException ioException) {
                                    // ignore, the sent RevocableFileDescriptor should already
                                    // be closed.
                                }
                            }
                            if (rfdsForPackage.isEmpty()) {
                                mSentRevocableFileDescriptorsForWriteLocked.remove(packageName);
                            }
                        }
                    }
                };
                break;
            default:
                throw new UnsupportedOperationException(
                        "Cannot support the AppSearchRevocableFileDescriptor mode: "
                                + revocableFileDescriptor.getMode());
        }
        revocableFileDescriptor.setOnCloseListener(closeListener);
    }

    /**
     * Adds a revocable file descriptor to the sent revocable file descriptor map.
     *
     * @param packageName The package name associated with the file descriptor.
     * @param blobHandle The blob handle associated with the file descriptor. It cannot be null if
     *                   the mode is READ_WRITE.
     * @param revocableFileDescriptor The revocable file descriptor to add.
     */
    private void addToSentRevocableFileDescriptorMap(
            @NonNull String packageName,
            @Nullable AppSearchBlobHandle blobHandle,
            @NonNull AppSearchRevocableFileDescriptor revocableFileDescriptor) {
        synchronized (mLock) {
            switch (revocableFileDescriptor.getMode()) {
                case ParcelFileDescriptor.MODE_READ_ONLY:
                    List<AppSearchRevocableFileDescriptor> rfdListForPackage =
                            mSentRevocableFileDescriptorsForReadLocked.get(packageName);
                    if (rfdListForPackage == null) {
                        rfdListForPackage = new ArrayList<>();
                        mSentRevocableFileDescriptorsForReadLocked.put(packageName,
                                rfdListForPackage);
                    }
                    rfdListForPackage.add(revocableFileDescriptor);
                    break;
                case ParcelFileDescriptor.MODE_READ_WRITE:
                    Preconditions.checkNotNull(blobHandle);
                    Map<AppSearchBlobHandle, AppSearchRevocableFileDescriptor> rfdMapForPackage =
                            mSentRevocableFileDescriptorsForWriteLocked.get(packageName);
                    if (rfdMapForPackage == null) {
                        rfdMapForPackage = new ArrayMap<>();
                        mSentRevocableFileDescriptorsForWriteLocked.put(packageName,
                                rfdMapForPackage);
                    }
                    rfdMapForPackage.put(blobHandle, revocableFileDescriptor);
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Cannot support the AppSearchRevocableFileDescriptor mode: "
                                    + revocableFileDescriptor.getMode());
            }
        }
    }
}
