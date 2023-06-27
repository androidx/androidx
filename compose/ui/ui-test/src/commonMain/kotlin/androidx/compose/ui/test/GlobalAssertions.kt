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
@file:JvmName("GlobalAssertions")

package androidx.compose.ui.test

/**
 * Adds a named assertion to the collection of assertions to be executed before test actions.
 *
 * This API is intended to be invoked by assertion frameworks to register assertions
 * that must hold on the entire application whenever it's fully loaded and ready to
 * interact with. They will be invoked upon common actions such as `performClick`, and
 * they always verify every element on the screen, not just the element the action is
 * performed on.
 *
 * This is particularly useful to automatically catch accessibility problems such
 * as contrast ratio, minimum touch-target size, etc.
 *
 * @param name An identifier for the assertion. It can subsequently be used to deactivate the
 *   assertion with [removeGlobalAssertion].
 * @param assertion A function to be executed.
 */
@ExperimentalTestApi
fun addGlobalAssertion(name: String, assertion: (SemanticsNodeInteraction) -> Unit) {
  GlobalAssertionsCollection.put(name, assertion)
}

/**
 * Removes a named assertion from the collection of assertions to be executed before test actions.
 *
 * @param name An identifier that was previously used in a call to [addGlobalAssertion].
 */
@ExperimentalTestApi
fun removeGlobalAssertion(name: String) {
  GlobalAssertionsCollection.remove(name)
}

/**
 * Executes all of the assertions registered by [addGlobalAssertion]. This may be useful in a custom
 * test action.
 *
 * @return the [SemanticsNodeInteraction] that is the receiver of this method
 */
@ExperimentalTestApi
fun SemanticsNodeInteraction.invokeGlobalAssertions(): SemanticsNodeInteraction {
  GlobalAssertionsCollection.invoke(this)
  return this
}

/**
 * Executes all of the assertions registered by [addGlobalAssertion], each of which will receive the
 * first node of this collection. This may be useful in a custom test action.
 *
 * @return the [SemanticsNodeInteractionCollection] that is the receiver of this method
 */
@ExperimentalTestApi
fun SemanticsNodeInteractionCollection.invokeGlobalAssertions():
    SemanticsNodeInteractionCollection {
  GlobalAssertionsCollection.invoke(this)
  return this
}

/** Assertions intended to be executed before test actions. */
internal object GlobalAssertionsCollection {
  const val TAG = "GlobalAssertions"

  /** Map of assertion names to their functions */
  private val globalAssertions = mutableMapOf<String, (SemanticsNodeInteraction) -> Unit>()

  /** Implementation of [addGlobalAssertion] */
  internal fun put(name: String, assertion: (SemanticsNodeInteraction) -> Unit) {
    globalAssertions[name] = assertion
  }

  /** Implementation of [removeGlobalAssertion] */
  internal fun remove(name: String) {
    globalAssertions.remove(name)
  }

  /** Executes every assertion on the given node. */
  internal fun invoke(sni: SemanticsNodeInteraction) {
    for (entry in globalAssertions.entries) {
      printToLog(TAG, "Executing \"${entry.key}\"")
      entry.value.invoke(sni)
    }
  }

  /** Executes every assertion on the first node of the given collection. */
  internal fun invoke(snic: SemanticsNodeInteractionCollection) {
    invoke(snic.onFirst())
  }
}
