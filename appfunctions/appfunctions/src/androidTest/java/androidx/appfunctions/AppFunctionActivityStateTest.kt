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

package androidx.appfunctions

import android.os.Binder
import android.os.Build
import android.util.ArraySet
import androidx.appfunctions.metadata.AppFunctionName
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
class AppFunctionActivityStateTest {

    @Test
    fun testEqualsAndHashCode() {
        val binder1 = Binder()
        val binder2 = Binder()

        val functionNames1 = ArraySet<AppFunctionName>()
        functionNames1.add(AppFunctionName("com.example", "myFunction1"))
        val functionNames2 = ArraySet<AppFunctionName>()
        functionNames2.add(AppFunctionName("com.example", "myFunction2"))

        val state1 = createAppFunctionActivityState(binder1, functionNames1)
        val state1Duplicate = createAppFunctionActivityState(binder1, functionNames1)
        val stateWithDifferentActivityId = createAppFunctionActivityState(binder2, functionNames1)
        val stateWithDifferentFunctionNames =
            createAppFunctionActivityState(binder1, functionNames2)

        assertThat(state1).isEqualTo(state1Duplicate)
        assertThat(state1.hashCode()).isEqualTo(state1Duplicate.hashCode())

        assertThat(state1).isNotEqualTo(stateWithDifferentActivityId)
        assertThat(state1.hashCode()).isNotEqualTo(stateWithDifferentActivityId.hashCode())

        assertThat(state1).isNotEqualTo(stateWithDifferentFunctionNames)
        assertThat(state1.hashCode()).isNotEqualTo(stateWithDifferentFunctionNames.hashCode())
    }

    private fun createAppFunctionActivityState(
        binder: android.os.IBinder,
        functionNames: ArraySet<AppFunctionName>,
    ): AppFunctionActivityState {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
        return Api37Impl.createAppFunctionActivityState(binder, functionNames)
    }

    /**
     * Helper class to call APIs that are only available on SDK 37 (Cinnamon Bun) or later.
     *
     * This is packaged as a nested class to prevent the JVM/Dalvik class loader from resolving
     * signatures referencing [android.app.appfunctions.AppFunctionActivityId], which would
     * otherwise cause a [NoClassDefFoundError].
     */
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    private object Api37Impl {
        fun createAppFunctionActivityId(
            binder: android.os.IBinder
        ): android.app.appfunctions.AppFunctionActivityId {
            val parcel = android.os.Parcel.obtain()
            try {
                parcel.writeStrongBinder(binder)
                parcel.setDataPosition(0)
                return android.app.appfunctions.AppFunctionActivityId.CREATOR.createFromParcel(
                    parcel
                )
            } finally {
                parcel.recycle()
            }
        }

        fun createAppFunctionActivityState(
            binder: android.os.IBinder,
            functionNames: ArraySet<AppFunctionName>,
        ): AppFunctionActivityState {
            val activityId = createAppFunctionActivityId(binder)
            return AppFunctionActivityState(activityId, functionNames)
        }
    }
}
