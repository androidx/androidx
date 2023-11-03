
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

package androidx.appactions.interaction.capabilities.core.impl.spec

import androidx.annotation.VisibleForTesting
import java.util.IdentityHashMap
import kotlin.reflect.KClass

/**
 * Contains a mapping of argument Class to its ActionSpec. This is used for testing.
 * The ActionSpec can be used to convert an Arguments instance into wire-format representation.
 */
object ActionSpecRegistry {
  private val argumentsClassToActionSpec = IdentityHashMap<Class<*>, ActionSpec<Any, *>>()
  private val outputClassToActionSpec = IdentityHashMap<Class<*>, ActionSpec<*, Any>>()

  @Suppress("UNCHECKED_CAST")
  fun <T : Any, R : Any> registerActionSpec(
    argumentsClass: KClass<T>,
    outputClass: KClass<R>,
    actionSpec: ActionSpec<T, R>
  ) {
    argumentsClassToActionSpec.put(
      argumentsClass.java,
      actionSpec as ActionSpec<Any, *>
    )
    outputClassToActionSpec.put(
      outputClass.java,
      actionSpec as ActionSpec<*, Any>
    )
  }

  @VisibleForTesting
  fun getActionSpecForArguments(arguments: Any): ActionSpec<Any, *>? {
    return argumentsClassToActionSpec[arguments.javaClass]
  }

  @VisibleForTesting
  fun getActionSpecForOutput(output: Any): ActionSpec<*, Any>? {
    return outputClassToActionSpec[output.javaClass]
  }
}
