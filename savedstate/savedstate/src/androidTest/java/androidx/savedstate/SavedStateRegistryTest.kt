/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.savedstate

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleRegistry
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class SavedStateRegistryTest {

    @UiThreadTest
    @Test
    fun saveRestoreFlow() {
        startFlow { registry ->
            registry.registerSavedStateProvider("a") { bundleOf("foo", 1) }
            registry.registerSavedStateProvider("b") { bundleOf("foo", 2) }
        }.recreateAndCheck { registry ->
            val bundleForA = registry.consumeRestoredStateForKey("a")
            val bundleForB = registry.consumeRestoredStateForKey("b")
            assertThat(bundleForA.isSame(bundleOf("foo", 1))).isTrue()
            assertThat(bundleForA.isSame(bundleOf("foo", 1))).isTrue()
            assertThat(bundleForB.isSame(bundleOf("foo", 2))).isTrue()
        }
    }

    @UiThreadTest
    @Test
    fun registerWithSameKey() {
        startFlow { registry ->
            registry.registerSavedStateProvider("key") { bundleOf("foo", "a") }
            try {
                registry.registerSavedStateProvider("key") { bundleOf("foo", "b") }
                Assert.fail("can't register with the same key")
            } catch (e: IllegalArgumentException) {
                // fail as expected
            }
        }
    }

    @UiThreadTest
    @Test
    fun consumeSameTwice() {
        startFlow { registry ->
            registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        }.recreateAndCheck { registry ->
            assertThat(registry.consumeRestoredStateForKey("a").isSame(bundleOf("key", "fo")))
                .isTrue()
            assertThat(registry.consumeRestoredStateForKey("a")).isNull()
        }
    }

    @UiThreadTest
    @Test
    fun unregister() {
        startFlow { registry ->
            registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
            registry.unregisterSavedStateProvider("a")
            // this call should succeed
            registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
            registry.unregisterSavedStateProvider("a")
        }.recreateAndCheck { registry ->
            assertThat(registry.consumeRestoredStateForKey("a")).isNull()
        }
    }

    @UiThreadTest
    @Test
    fun unconsumedSavedState() {
        startFlow { registry ->
            registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        }.recreateAndCheck {
            // so we don't consume anything after restoration
        }.recreateAndCheck { registry ->
            assertThat(registry.consumeRestoredStateForKey("a").isSame(bundleOf("key", "fo")))
                .isTrue()
        }
    }

    @UiThreadTest
    @Test
    fun unconsumedSavedStateClashWithCallback() {
        startFlow { registry ->
            registry.registerSavedStateProvider("a") { bundleOf("key", "fo") }
        }.recreateAndCheck { intermediateRegistry ->
            // there is unconsumed value for "a"
            intermediateRegistry.registerSavedStateProvider("a") { bundleOf("key", "ba") }
        }.recreateAndCheck { registry ->
            assertThat(registry.consumeRestoredStateForKey("a").isSame(bundleOf("key", "ba")))
                .isTrue()
        }
    }

    @UiThreadTest
    @Test
    fun autoRecreatedThrowOnMissingDefaultConstructor() {
        @Suppress("UNUSED_PARAMETER")
        class InvalidConstructorClass(unused: Int) : SavedStateRegistry.AutoRecreated {
            override fun onRecreated(owner: SavedStateRegistryOwner) {
                TODO("not implemented")
            }
        }
        startFlow { registry ->
            try {
                registry.runOnNextRecreation(InvalidConstructorClass::class.java)
                Assert.fail()
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(IllegalArgumentException::class.java)
            }
        }
    }

    @UiThreadTest
    @Test
    fun sneakClass() {
        startFlow { registry ->
            @Suppress("UNCHECKED_CAST")
            val sneak = ErrorInStaticBlock::class.java as Class<SavedStateRegistry.AutoRecreated>
            registry.runOnNextRecreation(sneak)
        }.recreate { owner ->
            try {
                owner.lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
                Assert.fail()
            } catch (e: Exception) {
                assertThat(e).isInstanceOf(ClassCastException::class.java)
            }
        }
    }

    @UiThreadTest
    @Test
    fun throwSavedStateRegistry() {
        val owner = FakeSavedStateRegistryOwner()
        // shouldn't throw, though we aren't even created
        owner.savedStateRegistry.runOnNextRecreation(ToBeRecreated::class.java)
        owner.savedStateRegistryController.performRestore(null)
        owner.lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        owner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        try {
            owner.savedStateRegistry.runOnNextRecreation(ToBeRecreated::class.java)
            Assert.fail()
        } catch (e: IllegalStateException) {
            assertThat(e.message).contains("Can not perform this action after onSaveInstanceState")
        }
        owner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        // shouldn't fail
        owner.savedStateRegistry.runOnNextRecreation(ToBeRecreated::class.java)
    }

    @UiThreadTest
    @Test
    fun emptyBundle() {
        val owner = FakeSavedStateRegistryOwner()
        val outBundle = Bundle()
        owner.savedStateRegistryController.performSave(outBundle)
        assertWithMessage("Bundle $outBundle should be empty")
            .that(outBundle.isEmpty)
            .isTrue()
    }

    @UiThreadTest
    @Test
    fun runOnNextRecreationFromEarlyRegisteredObserver() {
        val owner = FakeSavedStateRegistryOwner()
        owner.savedStateRegistryController.performAttach()
        // shouldn't throw, though we aren't even created
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START)
                    owner.savedStateRegistry.runOnNextRecreation(ToBeRecreated::class.java)
            }
        )
        owner.savedStateRegistryController.performRestore(null)
        owner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        // now ON_STOP event will be sent
        owner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        // now ON_START event will be sent again, previously registered observer shouldn't throw
        owner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    private class TestFlow(val lastState: Bundle?) {
        fun recreate(block: (FakeSavedStateRegistryOwner) -> Unit): TestFlow {
            val fakeOwner = FakeSavedStateRegistryOwner()
            fakeOwner.savedStateRegistryController.performRestore(lastState)
            assertThat(fakeOwner.savedStateRegistry.isRestored).isTrue()
            block(fakeOwner)
            val outBundle = Bundle()
            fakeOwner.savedStateRegistryController.performSave(outBundle)
            return TestFlow(outBundle)
        }

        fun recreateAndCheck(block: (SavedStateRegistry) -> Unit): TestFlow {
            return recreate { block(it.savedStateRegistry) }
        }
    }

    private fun startFlow(block: (SavedStateRegistry) -> Unit) = TestFlow(null)
        .recreateAndCheck(block)
}

private class ToBeRecreated : SavedStateRegistry.AutoRecreated {
    override fun onRecreated(owner: SavedStateRegistryOwner) {
        TODO("not implemented")
    }
}

private class FakeSavedStateRegistryOwner : SavedStateRegistryOwner {
    val lifecycleRegistry = LifecycleRegistry(this)
    val savedStateRegistryController = SavedStateRegistryController.create(this)

    override fun getLifecycle() = lifecycleRegistry
    override fun getSavedStateRegistry() = savedStateRegistryController.savedStateRegistry
}

private fun bundleOf(key: String, value: Int): Bundle {
    val result = Bundle()
    result.putInt(key, value)
    return result
}

private fun bundleOf(key: String, value: String): Bundle {
    val result = Bundle()
    result.putString(key, value)
    return result
}

private fun Bundle?.isSame(other: Bundle): Boolean {
    if (this == null) {
        return false
    }
    if (keySet() != other.keySet()) {
        return false
    }
    for (key in keySet()) {
        if (get(key) != other.get(key)) {
            return false
        }
    }
    return true
}