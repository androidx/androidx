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

// @exportToFramework:copyToPath(../../../cts/tests/appsearch/testutils/src/android/app/appsearch/testutil/external/JetpackRevocableFileDescriptorStore.java)
package androidx.appsearch.localstorage;

import android.os.ParcelFileDescriptor;

import androidx.annotation.RestrictTo;
import androidx.appsearch.app.ExperimentalAppSearchApi;

import org.jspecify.annotations.NonNull;

/**
 * The local storage implementation of {@link RevocableFileDescriptorStore}.
 *
 * <p> The {@link ParcelFileDescriptor} sent to the client side from the local storage won't cross
 * the binder, we could revoke the {@link ParcelFileDescriptor} in the client side by directly close
 * the one in AppSearch side. This won't work in the framework, since even if we close the
 * {@link ParcelFileDescriptor} in the server side, the one in the client side could still work.
 *
 * <p>This class is thread safety.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@ExperimentalAppSearchApi
public class JetpackRevocableFileDescriptorStore extends
        RevocableFileDescriptorStore {

    public JetpackRevocableFileDescriptorStore(@NonNull AppSearchConfig config) {
        super(config);
    }

    @Override
    protected @NonNull AppSearchRevocableFileDescriptor wrapToRevocableFileDescriptor(
            @NonNull ParcelFileDescriptor parcelFileDescriptor,
            int mode) {
        return new JetpackAppSearchRevocableFileDescriptor(parcelFileDescriptor, mode);
    }
}
