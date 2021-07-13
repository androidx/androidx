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
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
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
        val activityResult = registry.register(
            "test", lifecycleOwner,
            TakePicturePreview(),
            ActivityResultCallback {
                resultReturned = true
            }
        )

        // move the state to started
        lifecycleOwner.currentState = Lifecycle.State.STARTED

        // launch the result
        activityResult.launch(null)

        assertThat(resultReturned).isTrue()
    }

    @Test
    fun testLifecycleOwnerCallbackRestoredThenStarted() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.CREATED)

        // register for the result
        val activityResult = registry.register(
            "test", lifecycleOwner,
            TakePicturePreview(), ActivityResultCallback {}
        )

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
        registry.register(
            "test", lifecycleOwner, TakePicturePreview(),
            ActivityResultCallback {
                resultReturned = true
            }
        )

        lifecycleOwner.currentState = Lifecycle.State.STARTED

        assertThat(resultReturned).isTrue()
    }

    @Test
    fun testLifecycleOwnerRegisterWhenStarted() {
        val lifecycleOwner = TestLifecycleOwner()

        try {
            // register for the result
            registry.register(
                "test", lifecycleOwner,
                TakePicturePreview(), ActivityResultCallback {}
            )
            fail("Registering for activity result after Lifecycle ON_CREATE should fail")
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains(
                "LifecycleOwner $lifecycleOwner is attempting to register while current state " +
                    "is " + lifecycleOwner.currentState + ". LifecycleOwners must call " +
                    "register before they are STARTED."
            )
        }
    }

    @Test
    fun testLifecycleOwnerCallbackWhenStarted() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        // register for the result
        val activityResult = registry.register(
            "test", lifecycleOwner,
            TakePicturePreview(), ActivityResultCallback {}
        )

        // saved the state of the registry
        val state = Bundle()
        registry.onSaveInstanceState(state)

        // unregister the callback to simulate process death
        activityResult.unregister()

        // restore the state of the registry
        registry.onRestoreInstanceState(state)

        var resultReturned = false
        // re-register for the result that should have been saved
        registry.register(
            "test", lifecycleOwner, TakePicturePreview(),
            ActivityResultCallback {
                resultReturned = true
            }
        )

        // launch the result
        activityResult.launch(null)

        // move to CREATED and make sure the callback is not fired
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(resultReturned).isFalse()

        // move to STARTED and make sure the callback fires
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isTrue()

        // Reset back to CREATED
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        resultReturned = false

        // Move back to STARTED and make sure the previously returned result
        // isn't sent a second time
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isFalse()
    }

    @Test
    fun testLifecycleOwnerCallbackWithDispatchResult() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        val dispatchResultRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, true)
            }
        }

        var resultReturned = false
        val activityResult = dispatchResultRegistry.register(
            "test", lifecycleOwner, TakePicture(),
            ActivityResultCallback {
                resultReturned = true
            }
        )

        // launch the result
        activityResult.launch(null)

        // move to CREATED and make sure the callback is not fired
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(resultReturned).isFalse()

        // move to STARTED and make sure the callback fires
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isTrue()

        // Reset back to CREATED
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        resultReturned = false

        // Move back to STARTED and make sure the previously returned result
        // isn't sent a second time
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isFalse()
    }

    @Test
    fun testLifecycleOwnerCallbackWithNullDispatchResult() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        val dispatchResultRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, null)
            }
        }

        var resultReturned = false
        val activityResult = dispatchResultRegistry.register(
            "test", lifecycleOwner, TakePicturePreview(),
            ActivityResultCallback {
                resultReturned = true
            }
        )

        // launch the result
        activityResult.launch(null)

        // move to CREATED and make sure the callback is not fired
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(resultReturned).isFalse()

        // move to STARTED and make sure the callback fires
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isTrue()

        // Reset back to CREATED
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        resultReturned = false

        // Move back to STARTED and make sure the previously returned result
        // isn't sent a second time
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isFalse()
    }

    @Test
    fun testLifecycleOwnerCallbackUnregistered() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)

        // register for the result
        val activityResult = registry.register(
            "test", lifecycleOwner,
            TakePicturePreview(), ActivityResultCallback {}
        )

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
        registry.register(
            "test", lifecycleOwner, TakePicturePreview(),
            ActivityResultCallback {
                resultReturned = true
            }
        )

        // move to CREATED and make sure the callback is not fired
        lifecycleOwner.currentState = Lifecycle.State.CREATED
        assertThat(resultReturned).isFalse()

        // unregister the callback
        registry.unregister("test")

        // move to STARTED and make sure the callback is not fired
        lifecycleOwner.currentState = Lifecycle.State.STARTED
        assertThat(resultReturned).isFalse()
    }

    @Test
    fun testUnregisterAfterSavedState() {
        val lifecycleOwner = TestLifecycleOwner(Lifecycle.State.INITIALIZED)
        var resultReturned = false
        val activityResult = registry.register("key", lifecycleOwner, StartActivityForResult()) { }

        activityResult.launch(null)

        val savedState = Bundle()
        registry.onSaveInstanceState(savedState)

        registry.unregister("key")

        val restoredRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, RESULT_OK, Intent())
            }
        }

        restoredRegistry.onRestoreInstanceState(savedState)

        restoredRegistry.register("key", lifecycleOwner, StartActivityForResult()) {
            resultReturned = true
        }

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

    @Test
    fun testRegisterBeforeRestoreInstanceState() {
        registry.register("key", StartActivityForResult()) { }

        val savedState = Bundle()
        registry.onSaveInstanceState(savedState)

        val restoredRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, RESULT_OK, Intent())
            }
        }

        restoredRegistry.register("key", StartActivityForResult()) { }
        restoredRegistry.onRestoreInstanceState(savedState)

        val newSavedState = Bundle()
        restoredRegistry.onSaveInstanceState(newSavedState)

        val keys = newSavedState.getStringArrayList("KEY_COMPONENT_ACTIVITY_REGISTERED_KEYS")

        assertThat(keys?.size).isEqualTo(1)
    }

    @Test
    fun testKeepKeyAfterLaunch() {
        var code = 0
        val noDispatchRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                code = requestCode
            }
        }

        val activityResult = noDispatchRegistry.register("key", StartActivityForResult()) { }

        activityResult.launch(null)
        activityResult.unregister()

        var callbackExecuted = false
        noDispatchRegistry.register("key", StartActivityForResult()) {
            callbackExecuted = true
        }

        noDispatchRegistry.dispatchResult(code, RESULT_OK, Intent())

        assertThat(callbackExecuted).isTrue()
    }

    @Test
    fun testKeepKeyAfterLaunchDispatchResult() {
        var code = 0
        val noDispatchRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                code = requestCode
            }
        }

        val activityResult = noDispatchRegistry.register("key", StartActivityForResult()) { }

        activityResult.launch(null)
        activityResult.unregister()

        var callbackExecuted = false
        noDispatchRegistry.register("key", StartActivityForResult()) {
            callbackExecuted = true
        }

        noDispatchRegistry.dispatchResult(code, ActivityResult(RESULT_OK, Intent()))

        assertThat(callbackExecuted).isTrue()
    }

    @Test
    fun testLaunchUnregistered() {
        val contract = StartActivityForResult()
        val activityResult = registry.register("key", contract) { }

        activityResult.unregister()

        try {
            activityResult.launch(null)
        } catch (e: IllegalStateException) {
            assertThat(e).hasMessageThat().contains(
                "Attempting to launch an unregistered ActivityResultLauncher with contract " +
                    contract + " and input null. You must ensure the ActivityResultLauncher is " +
                    "registered before calling launch()."
            )
        }
    }

    @Test
    fun testSavePendingOnRestore() {
        var code = 0
        val noDispatchRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                code = requestCode
            }
        }

        val contract = StartActivityForResult()
        val launcher = noDispatchRegistry.register("key", contract) { }

        launcher.launch(Intent())
        launcher.unregister()

        noDispatchRegistry.dispatchResult(code, RESULT_OK, Intent())

        val savedState = Bundle()
        noDispatchRegistry.onSaveInstanceState(savedState)

        val newNoDispatchRegistry = object : ActivityResultRegistry() {
            override fun <I : Any?, O : Any?> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                code = requestCode
            }
        }

        var completedLaunch = false
        newNoDispatchRegistry.register("key", contract) {
            completedLaunch = true
        }

        newNoDispatchRegistry.onRestoreInstanceState(savedState)

        newNoDispatchRegistry.dispatchResult(code, RESULT_OK, Intent())

        assertThat(completedLaunch).isTrue()
    }
}