/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appsearch.cts.app;

import androidx.annotation.NonNull;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

/** Util class for GMSCore AppSearch related Cts tests. */
public final class GmsTestUtil {

    private GmsTestUtil() {}

    /**
     * This method returns false when GMSCore or GMSCore AppSearch module are unavailable on
     * device.
     */
    public static boolean isGmsAvailable(
            @NonNull ListenableFuture<?> sessionListenableFuture) throws Exception {
        try {
            sessionListenableFuture.get();
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof ApiException) {
                ApiException apiException = (ApiException) exception.getCause();
                if (apiException.getStatusCode() == CommonStatusCodes.API_NOT_CONNECTED) {
                    // GMSCore or GMSCore AppSearch Module not installed on device.
                    // TODO(b/280864281): Also handle the case when GMSCore and AppSearch
                    //  module present but AppSearch dynamite module not present.
                    return false;
                }
            }

            throw exception;
        } catch (InterruptedException exception) {
            throw exception;
        }
        return true;
    }
}
