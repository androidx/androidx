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

package androidx.privacysandbox.databridge.client.util

import com.google.common.truth.Truth.assertThat

class KeyValueUtil() {
    companion object {
        fun assertKeySetSuccessfully(value: Any, result: Result<Any?>) {
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo(value)
        }

        fun assertGetValueThrowsClassCastException(result: Result<Any?>) {
            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull() is ClassCastException).isTrue()
        }

        fun assertKeyIsMissing(result: Result<Any?>) {
            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isNull()
        }
    }
}
