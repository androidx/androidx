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

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class AppFunctionStateTest {

    @Test
    fun testEqualsAndHashCode() {
        val myFunction1 = AppFunctionName("com.example", "myFunction")
        val myFunction2 = AppFunctionName("com.example", "myFunction")
        val otherFunction = AppFunctionName("com.example", "otherFunction")

        val myFunctionStateEnabled = AppFunctionState(myFunction1, true)
        val myFunctionStateEnabled2 = AppFunctionState(myFunction2, true)
        val myFunctionStateDisabled = AppFunctionState(myFunction1, false)
        val otherFunctionStateEnabled = AppFunctionState(otherFunction, true)

        assertThat(myFunctionStateEnabled).isEqualTo(myFunctionStateEnabled2)
        assertThat(myFunctionStateEnabled.hashCode()).isEqualTo(myFunctionStateEnabled2.hashCode())

        assertThat(myFunctionStateEnabled).isNotEqualTo(myFunctionStateDisabled)
        assertThat(myFunctionStateEnabled.hashCode())
            .isNotEqualTo(myFunctionStateDisabled.hashCode())

        assertThat(myFunctionStateEnabled).isNotEqualTo(otherFunctionStateEnabled)
        assertThat(myFunctionStateDisabled.hashCode())
            .isNotEqualTo(otherFunctionStateEnabled.hashCode())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    fun testEqualsAndHashCode_withActivityIds() {
        val myFunction1 = AppFunctionName("com.example", "myFunction")
        val myFunction2 = AppFunctionName("com.example", "myFunction")

        val binder1 = Binder()
        val binder2 = Binder()

        val myActivityStateEnabled = createAppFunctionState(myFunction1, true, listOf(binder1))
        val myActivityStateEnabled2 = createAppFunctionState(myFunction2, true, listOf(binder1))
        val otherActivityStateEnabled = createAppFunctionState(myFunction1, true, listOf(binder2))
        val noActivityStateEnabled = AppFunctionState(myFunction1, true)

        assertThat(myActivityStateEnabled).isEqualTo(myActivityStateEnabled2)
        assertThat(myActivityStateEnabled.hashCode()).isEqualTo(myActivityStateEnabled2.hashCode())

        assertThat(myActivityStateEnabled).isNotEqualTo(otherActivityStateEnabled)
        assertThat(myActivityStateEnabled.hashCode())
            .isNotEqualTo(otherActivityStateEnabled.hashCode())

        assertThat(myActivityStateEnabled).isNotEqualTo(noActivityStateEnabled)
        assertThat(myActivityStateEnabled.hashCode())
            .isNotEqualTo(noActivityStateEnabled.hashCode())
    }

    private fun createAppFunctionState(
        functionName: AppFunctionName,
        isEnabled: Boolean,
        binders: List<android.os.IBinder>,
    ): AppFunctionState {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
        return Api37Impl.createAppFunctionState(functionName, isEnabled, binders)
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

        fun createAppFunctionState(
            functionName: AppFunctionName,
            isEnabled: Boolean,
            binders: List<android.os.IBinder>,
        ): AppFunctionState {
            val activityIds = ArraySet<android.app.appfunctions.AppFunctionActivityId>()
            for (binder in binders) {
                activityIds.add(createAppFunctionActivityId(binder))
            }
            return AppFunctionState(functionName, isEnabled, activityIds)
        }
    }
}
