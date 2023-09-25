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

package androidx.camera.camera2.pipe.media

import android.os.Build
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.same
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Tests for [SharedReference] */
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class SharedReferenceTest {
    private val finalizer: Finalizer<AutoCloseable> = mock()
    private val closeableObject: AutoCloseable = mock()
    private val sharedReference = SharedReference(closeableObject, ClosingFinalizer)

    @Test
    fun decrementingSharedReferenceInvokesFinalizerExactlyOnce() {
        verify(closeableObject, never()).close()
        sharedReference.decrement()
        sharedReference.decrement()
        sharedReference.decrement()
        verify(closeableObject, times(1)).close()
    }

    @Test
    fun acquiringObjectIncrementsSharedReference() {
        val obj = sharedReference.acquireOrNull()
        assertThat(obj).isNotNull()

        sharedReference.decrement()
        verify(closeableObject, never()).close()

        sharedReference.decrement()
        verify(closeableObject, times(1)).close()
    }

    @Test
    fun settingFinalizerReplacesOldFinalizer() {
        sharedReference.setFinalizer(finalizer)
        verify(closeableObject, never()).close()
        verify(finalizer, never()).finalize(any())

        sharedReference.decrement()
        verify(finalizer, times(1)).finalize(same(closeableObject))
    }
}
