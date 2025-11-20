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
// @exportToFramework:skipFile()
package androidx.appsearch.app;

import androidx.annotation.RequiresFeature;

/**
 * Provides a connection to all enterprise (work profile) AppSearch databases the querying
 * application has been granted access to.
 *
 * <p>This session can be created from any user profile but will only properly return results when
 * created from the main profile. If the user is not the main profile or an associated work profile
 * does not exist, queries will still successfully complete but with empty results.
 *
 * <p>Schemas must be explicitly tagged enterprise and may require additional permissions to be
 * visible from an enterprise session. Retrieved documents may also have certain fields restricted
 * or modified unlike if they were retrieved directly from {@link GlobalSearchSession} on the work
 * profile.
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @see GlobalSearchSession
 */
@RequiresFeature(
        enforcement = "androidx.appsearch.app.Features#isFeatureSupported",
        name = Features.ENTERPRISE_GLOBAL_SEARCH_SESSION)
public interface EnterpriseGlobalSearchSession extends ReadOnlyGlobalSearchSession {
}
