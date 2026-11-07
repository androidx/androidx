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

/** Runs {@link AppSearchImplOptimizeTestBase} with the single-stage optimize. */
// TODO(b/448657720): Remove this class once the 2-stage optimize is stable and the
//  single-stage optimize is removed.
public class AppSearchImplOptimizeTest extends AppSearchImplOptimizeTestBase {
    @Override
    protected boolean enableUpgradableLockAndTwoStageOptimize() {
        return false;
    }
}
