/*
 * Copyright (C) 2022 The Android Open Source Project
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
package androidx.health.platform.client.error

import androidx.health.platform.client.error.ErrorStatus.Companion.create
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ErrorStatusTest {
    @Test
    fun unknownErrorCode_internal() {
        val errorStatus = create(-1)
        assertThat(errorStatus.errorCode).isEqualTo(ErrorCode.INTERNAL_ERROR)
    }

    @Test
    fun knownErrorCode() {
        val errorStatus = create(ErrorCode.DATABASE_ERROR)
        assertThat(errorStatus.errorCode).isEqualTo(ErrorCode.DATABASE_ERROR)
    }
}
