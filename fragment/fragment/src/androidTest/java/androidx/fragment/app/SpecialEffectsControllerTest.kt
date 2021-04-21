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

package androidx.fragment.app

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class SpecialEffectsControllerTest {
    @Test
    fun factoryCreateController() {
        val map = mutableMapOf<ViewGroup, TestSpecialEffectsController>()
        val factory = SpecialEffectsControllerFactory { container ->
            TestSpecialEffectsController(container).also {
                map[container] = it
            }
        }
        val container = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val controller = factory.createController(container)
        assertThat(controller)
            .isEqualTo(map[container])
        assertThat(controller.container)
            .isEqualTo(container)

        // Ensure that a new container gets a new controller
        val secondContainer = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val secondController = factory.createController(secondContainer)
        assertThat(secondController)
            .isEqualTo(map[secondContainer])
        assertThat(secondController)
            .isNotEqualTo(controller)
    }

    @Test
    fun getOrCreateController() {
        var count = 0
        val map = mutableMapOf<ViewGroup, TestSpecialEffectsController>()
        val factory = SpecialEffectsControllerFactory { container ->
            count++
            TestSpecialEffectsController(container).also {
                map[container] = it
            }
        }
        val container = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val controller = SpecialEffectsController.getOrCreateController(container, factory)
        assertThat(controller)
            .isEqualTo(map[container])
        assertThat(controller.container)
            .isEqualTo(container)
        assertThat(count)
            .isEqualTo(1)

        // Recreating the controller shouldn't cause the count to increase
        val recreatedController = SpecialEffectsController.getOrCreateController(
            container, factory
        )
        assertThat(recreatedController)
            .isEqualTo(controller)
        assertThat(recreatedController.container)
            .isEqualTo(container)
        assertThat(count)
            .isEqualTo(1)

        // But creating a controller for a different view returns a new instance
        val secondContainer = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val secondController = SpecialEffectsController.getOrCreateController(
            secondContainer, factory
        )
        assertThat(secondController)
            .isEqualTo(map[secondContainer])
        assertThat(secondController.container)
            .isEqualTo(secondContainer)
        assertThat(count)
            .isEqualTo(2)
    }

    @Test
    fun noExecuteIfEmpty() {
        val container = FrameLayout(InstrumentationRegistry.getInstrumentation().context)
        val controller = InstantSpecialEffectsController(container)
        controller.executePendingOperations()

        assertThat(controller.executeOperationsCallCount).isEqualTo(0)
    }

    @MediumTest
    @Test
    fun enqueueAddAndExecute() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            fm.specialEffectsControllerFactory = SpecialEffectsControllerFactory {
                InstantSpecialEffectsController(it)
            }
            val fragment = StrictViewFragment()
            val fragmentStore = FragmentStore()
            fragmentStore.nonConfig = FragmentManagerViewModel(true)
            val fragmentStateManager = FragmentStateManager(
                fm.lifecycleCallbacksDispatcher,
                fragmentStore, fragment
            )
            // Set up the Fragment and FragmentStateManager as if the Fragment was
            // added to the container via a FragmentTransaction
            fragment.mFragmentManager = fm
            fragment.mAdded = true
            fragment.mContainerId = android.R.id.content
            fragmentStateManager.setFragmentManagerState(Fragment.ACTIVITY_CREATED)
            onActivity {
                // This moves the Fragment up to ACTIVITY_CREATED,
                // calling enqueueAdd() under the hood
                fragmentStateManager.moveToExpectedState()
            }
            assertThat(fragment.view)
                .isNotNull()
            // setFragmentManagerState() doesn't call moveToExpectedState() itself
            fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
            val controller = SpecialEffectsController.getOrCreateController(container, fm)
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)
            onActivity {
                // However, executePendingOperations(), since we're using our
                // TestSpecialEffectsController, does immediately call complete()
                // which in turn calls moveToExpectedState()
                controller.executePendingOperations()
            }
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isNull()
            // Assert that we actually moved to the STARTED state
            assertThat(fragment.lifecycle.currentState)
                .isEqualTo(Lifecycle.State.STARTED)
        }
    }

    @MediumTest
    @Test
    fun enqueueRemoveAndExecute() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            fm.specialEffectsControllerFactory = SpecialEffectsControllerFactory {
                InstantSpecialEffectsController(it)
            }
            val fragment = StrictViewFragment()
            val fragmentStore = FragmentStore()
            fragmentStore.nonConfig = FragmentManagerViewModel(true)
            val fragmentStateManager = FragmentStateManager(
                fm.lifecycleCallbacksDispatcher,
                fragmentStore, fragment
            )
            // Set up the Fragment and FragmentStateManager as if the Fragment was
            // added to the container via a FragmentTransaction
            fragment.mFragmentManager = fm
            fragment.mAdded = true
            fragment.mContainerId = android.R.id.content
            fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
            val controller = SpecialEffectsController.getOrCreateController(container, fm)
            onActivity {
                // moveToExpectedState() first to call enqueueAdd()
                fragmentStateManager.moveToExpectedState()
                // Then executePendingOperations() to clear that out
                controller.executePendingOperations()
            }
            assertThat(fragment.lifecycle.currentState)
                .isEqualTo(Lifecycle.State.STARTED)
            // setFragmentManagerState() doesn't call moveToExpectedState() itself
            fragmentStateManager.setFragmentManagerState(Fragment.CREATED)
            onActivity {
                fragmentStateManager.moveToExpectedState()
            }
            // setFragmentManagerState() doesn't call moveToExpectedState() itself
            fragmentStateManager.setFragmentManagerState(Fragment.ATTACHED)
            onActivity {
                // However, executePendingOperations(), since we're using our
                // TestSpecialEffectsController, does immediately call complete()
                // which in turn calls moveToExpectedState()
                controller.executePendingOperations()
            }
            // Assert that we actually moved to the ATTACHED state
            assertThat(fragment.calledOnDestroy)
                .isTrue()
            assertThat(fragment.calledOnDetach)
                .isFalse()
        }
    }

    @MediumTest
    @Test
    fun enqueueAddAndCancel() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            fm.specialEffectsControllerFactory = SpecialEffectsControllerFactory {
                TestSpecialEffectsController(it)
            }
            val fragment = StrictViewFragment()
            val fragmentStore = FragmentStore()
            fragmentStore.nonConfig = FragmentManagerViewModel(true)
            val fragmentStateManager = FragmentStateManager(
                fm.lifecycleCallbacksDispatcher,
                fragmentStore, fragment
            )
            // Set up the Fragment and FragmentStateManager as if the Fragment was
            // added to the container via a FragmentTransaction
            fragment.mFragmentManager = fm
            fragment.mAdded = true
            fragment.mContainerId = android.R.id.content
            fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
            val controller = SpecialEffectsController
                .getOrCreateController(container, fm) as TestSpecialEffectsController
            onActivity {
                // This moves the Fragment up to STARTED,
                // calling enqueueAdd() under the hood
                fragmentStateManager.moveToExpectedState()
                controller.executePendingOperations()
            }
            assertThat(fragment.view)
                .isNotNull()
            val operations = controller.operationsToExecute
            assertThat(operations)
                .hasSize(1)
            val firstOperation = operations[0]
            assertThat(firstOperation.lifecycleImpact)
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)
            assertThat(firstOperation.fragment)
                .isSameInstanceAs(fragment)
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)
            fragmentStateManager.setFragmentManagerState(Fragment.CREATED)
            onActivity {
                // move the Fragment's state back down, which
                // cancels the ADD operation
                fragmentStateManager.moveToExpectedState()
                controller.executePendingOperations()
            }
            assertThat(firstOperation.isCanceled)
                .isTrue()
            assertThat(controller.operationsToExecute)
                .doesNotContain(firstOperation)
            assertThat(controller.operationsToExecute)
                .hasSize(1)
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.REMOVING)
            onActivity {
                controller.completeAllOperations()
            }
            assertThat(controller.operationsToExecute)
                .isEmpty()
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isNull()
            assertThat(fragment.lifecycle.currentState)
                .isEqualTo(Lifecycle.State.CREATED)
        }
    }

    @MediumTest
    @Test
    fun enqueueAddAndForceCompleteAllPending() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            fm.specialEffectsControllerFactory = SpecialEffectsControllerFactory {
                TestSpecialEffectsController(it)
            }
            val fragment = StrictViewFragment()
            val fragmentStore = FragmentStore()
            fragmentStore.nonConfig = FragmentManagerViewModel(true)
            val fragmentStateManager = FragmentStateManager(
                fm.lifecycleCallbacksDispatcher,
                fragmentStore, fragment
            )
            // Set up the Fragment and FragmentStateManager as if the Fragment was
            // added to the container via a FragmentTransaction
            fragment.mFragmentManager = fm
            fragment.mAdded = true
            fragment.mContainerId = android.R.id.content
            fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
            val controller = SpecialEffectsController
                .getOrCreateController(container, fm) as TestSpecialEffectsController
            onActivity {
                // This moves the Fragment up to STARTED,
                // calling enqueueAdd() under the hood
                fragmentStateManager.moveToExpectedState()
            }
            assertThat(controller.operationsToExecute)
                .isEmpty()
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)

            onActivity {
                // Now force all operations to immediately complete
                controller.forceCompleteAllOperations()
            }

            assertThat(controller.operationsToExecute)
                .isEmpty()
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isNull()
        }
    }

    @MediumTest
    @Test
    fun enqueueAddAndForceCompleteAllExecuting() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            fm.specialEffectsControllerFactory = SpecialEffectsControllerFactory {
                TestSpecialEffectsController(it)
            }
            val fragment = StrictViewFragment()
            val fragmentStore = FragmentStore()
            fragmentStore.nonConfig = FragmentManagerViewModel(true)
            val fragmentStateManager = FragmentStateManager(
                fm.lifecycleCallbacksDispatcher,
                fragmentStore, fragment
            )
            // Set up the Fragment and FragmentStateManager as if the Fragment was
            // added to the container via a FragmentTransaction
            fragment.mFragmentManager = fm
            fragment.mAdded = true
            fragment.mContainerId = android.R.id.content
            fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
            val controller = SpecialEffectsController
                .getOrCreateController(container, fm) as TestSpecialEffectsController
            onActivity {
                // This moves the Fragment up to STARTED,
                // calling enqueueAdd() under the hood
                fragmentStateManager.moveToExpectedState()
                controller.executePendingOperations()
            }
            val operations = controller.operationsToExecute
            assertThat(operations)
                .hasSize(1)
            val firstOperation = operations[0]
            assertThat(firstOperation.lifecycleImpact)
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)
            assertThat(firstOperation.fragment)
                .isSameInstanceAs(fragment)
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)

            var lifecycleImpactOnCompletion:
                SpecialEffectsController.Operation.LifecycleImpact? = null
            firstOperation.addCompletionListener {
                lifecycleImpactOnCompletion = controller.getAwaitingCompletionLifecycleImpact(
                    fragmentStateManager
                )
            }
            onActivity {
                // Now force all operations to immediately complete
                controller.forceCompleteAllOperations()
            }

            assertThat(firstOperation.isCanceled)
                .isTrue()
            assertThat(lifecycleImpactOnCompletion).isNull()
            assertThat(controller.operationsToExecute)
                .isEmpty()
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isNull()
        }
    }

    @MediumTest
    @Test
    fun enqueueAddAndPostpone() {
        with(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val container = withActivity { findViewById<ViewGroup>(android.R.id.content) }
            val fm = withActivity { supportFragmentManager }
            fm.specialEffectsControllerFactory = SpecialEffectsControllerFactory {
                TestSpecialEffectsController(it)
            }
            val fragment = StrictViewFragment()
            fragment.postponeEnterTransition()
            val fragmentStore = FragmentStore()
            fragmentStore.nonConfig = FragmentManagerViewModel(true)
            val fragmentStateManager = FragmentStateManager(
                fm.lifecycleCallbacksDispatcher,
                fragmentStore, fragment
            )
            // Set up the Fragment and FragmentStateManager as if the Fragment was
            // added to the container via a FragmentTransaction
            fragment.mFragmentManager = fm
            fragment.mAdded = true
            fragment.mContainerId = android.R.id.content
            fragmentStateManager.setFragmentManagerState(Fragment.STARTED)
            val controller = SpecialEffectsController
                .getOrCreateController(container, fm) as TestSpecialEffectsController
            onActivity {
                // This moves the Fragment up to STARTED,
                // calling enqueueAdd() under the hood
                fragmentStateManager.moveToExpectedState()
            }
            assertThat(controller.operationsToExecute)
                .isEmpty()
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)

            // Mark the postponed state
            controller.markPostponedState()
            controller.executePendingOperations()

            // Verify that executePendingOperations() didn't actually execute
            // anything since we are postponed
            assertThat(controller.operationsToExecute)
                .isEmpty()
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)

            onActivity {
                fragment.startPostponedEnterTransition()
            }
            // Wait for idle thread to handle the post() that startPostponedEnterTransition() does.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            // Verify that the operation was sent for execution
            assertThat(controller.operationsToExecute)
                .hasSize(1)
            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isEqualTo(SpecialEffectsController.Operation.LifecycleImpact.ADDING)

            controller.completeAllOperations()

            assertThat(controller.getAwaitingCompletionLifecycleImpact(fragmentStateManager))
                .isNull()
            // Assert that we actually moved to the STARTED state
            assertThat(fragment.lifecycle.currentState)
                .isEqualTo(Lifecycle.State.STARTED)
        }
    }
}

internal class TestSpecialEffectsController(
    container: ViewGroup
) : SpecialEffectsController(container) {
    val operationsToExecute = mutableListOf<Operation>()

    override fun executeOperations(operations: MutableList<Operation>, isPop: Boolean) {
        operationsToExecute.addAll(operations)
        operations.forEach { operation ->
            operation.addCompletionListener {
                operationsToExecute.remove(operation)
            }
        }
    }

    fun completeAllOperations() {
        operationsToExecute.forEach(Operation::complete)
        operationsToExecute.clear()
    }
}

internal class InstantSpecialEffectsController(
    container: ViewGroup
) : SpecialEffectsController(container) {
    var executeOperationsCallCount = 0

    override fun executeOperations(operations: MutableList<Operation>, isPop: Boolean) {
        executeOperationsCallCount++
        operations.forEach(Operation::complete)
    }
}
