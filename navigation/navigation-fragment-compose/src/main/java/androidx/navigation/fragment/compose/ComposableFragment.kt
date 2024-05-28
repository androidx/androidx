/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.navigation.fragment.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.reflect.getDeclaredComposableMethod
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment

/**
 * This class provides a [Fragment] wrapper around a composable function that is loaded via
 * reflection. The composable function has access to this fragment instance via [LocalFragment].
 *
 * This class is constructed via a factory method: make sure you add `import
 * androidx.navigation.fragment.compose.ComposableFragment.Companion.ComposableFragment`
 */
class ComposableFragment internal constructor() : Fragment() {

    private val composableMethod by lazy {
        val arguments = requireArguments()
        val fullyQualifiedName =
            checkNotNull(arguments.getString(FULLY_QUALIFIED_NAME)) {
                "Instances of ComposableFragment must be created with the factory function " +
                    "ComposableFragment(fullyQualifiedName)"
            }
        val (className, methodName) = fullyQualifiedName.split("$")
        val clazz = Class.forName(className)
        clazz.getDeclaredComposableMethod(methodName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Consider using Fragment.content from fragment-compose once it is stable
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CompositionLocalProvider(LocalFragment provides this@ComposableFragment) {
                    composableMethod.invoke(currentComposer, null)
                }
            }
        }
    }

    companion object {
        internal const val FULLY_QUALIFIED_NAME =
            "androidx.navigation.fragment.compose.FULLY_QUALIFIED_NAME"

        /**
         * Creates a new [ComposableFragment] instance that will wrap the Composable method loaded
         * via reflection from [fullyQualifiedName].
         *
         * @param fullyQualifiedName the fully qualified name of the static, no argument Composable
         *   method that this fragment should display. It should be formatted in the format
         *   `com.example.NameOfFileKt/$MethodName`.
         */
        @JvmStatic
        fun ComposableFragment(fullyQualifiedName: String): ComposableFragment {
            return ComposableFragment().apply {
                arguments = bundleOf(FULLY_QUALIFIED_NAME to fullyQualifiedName)
            }
        }
    }
}
