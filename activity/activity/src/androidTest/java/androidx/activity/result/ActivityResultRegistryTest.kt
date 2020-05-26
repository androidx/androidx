/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.result

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ActivityResultRegistryTest {
    private val registry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            dispatchResult(requestCode, RESULT_OK, Intent())
        }
    }

    @Test
    fun testRegisterLifecycleOwnerCallback() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var resultReturned = false

        // register for the result
        val activityResult = registry.register("test", lifecycleOwner,
            TakePicturePreview(), ActivityResultCallback {
                resultReturned = true
            })

        // move the state to started
        lifecycleOwner.currentState = Lifecycle.State.STARTED

        // launch the result
        activityResult.launch(null)

        assertThat(resultReturned).isTrue()
    }

    @Test
    fun testLifecycleOwnerCallbackAlreadyStarted() {
        val lifecycleOwner = TestLifecycleOwner()

        // register for the result
        val activityResult = registry.register("test", lifecycleOwner,
            TakePicturePreview(), ActivityResultCallback {})

        // saved the state of the registry
        val state = Bundle()
        registry.onSaveInstanceState(state)

        // unregister the callback to simulate process death
        activityResult.unregister()

        // restore the state of the registry
        registry.onRestoreInstanceState(state)

        // launch the result
        activityResult.launch(null)

        var resultReturned = false
        // re-register for the result that should have been saved
        registry.register("test", lifecycleOwner, TakePicturePreview(), ActivityResultCallback {
            resultReturned = true
        })

        assertThat(resultReturned).isTrue()
    }

    @Test
    fun testLifecycleOwnerCallbackWhenStarted() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        // register for the result
        val activityResult = registry.register("test", lifecycleOwner,
            TakePicturePreview(), ActivityResultCallback {})

        // saved the state of the registry
        val state = Bundle()
        registry.onSaveInstanceState(state)

        // unregister the callback to simulate process death
        activityResult.unregister()

        // restore the state of the registry
        registry.onRestoreInstanceState(state)

        // launch the result
        activityResult.launch(null)

        var resultReturned = false
        // re-register for the result that should have been saved
        registry.register("test", lifecycleOwner, TakePicturePreview(), ActivityResultCallback {
                resultReturned = true
            })

        // move to CREATED and make sure the callback is not fired
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(resultReturned).isFalse()

        // move to STARTED and make sure the callback fires
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isTrue()
    }

    @Test
    fun testOnRestoreInstanceState() {
        registry.register("key", StartActivityForResult()) {}

        val savedState = Bundle()
        registry.onSaveInstanceState(savedState)

        registry.onRestoreInstanceState(savedState)
    }

    @Test
    fun testOnRestoreInstanceStateNoKeys() {
        registry.onRestoreInstanceState(Bundle())
    }
}