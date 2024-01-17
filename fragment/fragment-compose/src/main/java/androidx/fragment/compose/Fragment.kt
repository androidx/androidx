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

package androidx.fragment.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment

/**
 * Wrapper function that handles the setup for creating a custom Fragment that hosts Compose
 * content. It automatically sets the [ViewCompositionStrategy] to
 * [ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed].
 *
 * It should be used as part of the implementation of [Fragment.onCreateView] and requires a context
 * meaning the fragment must be attached to a FragmentManager.
 *
 * ```
 * class ExampleFragment : Fragment() {
 *     override fun onCreateView(
 *         inflater: LayoutInflater,
 *         container: ViewGroup?,
 *         savedInstanceState: Bundle?
 *     ) = content {
 *         val viewModel: ExampleViewModel = viewModel()
 *         // put your @Composable content here
 *     }
 * }
 * ```
 */
fun Fragment.content(content: @Composable () -> Unit): ComposeView {
    return ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent(content)
    }
}
