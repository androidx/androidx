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

package androidx.compose.runtime.tooling

/** Interface providing a value to identify a scope for tooling. */
@ComposeToolingApi
public interface IdentifiableRecomposeScope {
    /**
     * A value that identifies a scope independently of movement caused by recompositions. This is
     * the same identity value as returned by identity of the CompositionGroup that contains the
     * RecomposeScope.
     */
    public val identity: Any?
}
