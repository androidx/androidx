/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.fragment.lint

import androidx.fragment.lint.stubs.REPEAT_ON_LIFECYCLE_STUBS
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.checks.infrastructure.TestLintTask
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class UnsafeRepeatOnLifecycleDetectorTest(private val config: TestConfig) {

    data class TestConfig(
        val fragmentExtensions: String,
        val apiCall: String,
        val shouldWarn: Boolean
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun spec(): List<TestConfig> =
            // Adding permutations manually to manually control if something should give a warning
            listOf(
                TestConfig(
                    "Fragment()",
                    "repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "Fragment()",
                    "lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "Fragment()",
                    "viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    false
                ),
                TestConfig(
                    "Fragment()",
                    "lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "Fragment()",
                    "getViewLifecycleOwner().repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    false
                ),
                TestConfig(
                    "Fragment()",
                    "getLifecycleOwner().repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "Fragment()",
                    "viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    false
                ),
                TestConfig(
                    "Fragment()",
                    "lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "Fragment()",
                    "getViewLifecycleOwner().lifecycle." +
                        "repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    false
                ),
                TestConfig(
                    "Fragment()",
                    "getLifecycleOwner().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "BaseFragment(), FragmentListener",
                    "getLifecycleOwner().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "FragmentListener, OtherFragmentListener, BaseFragment()",
                    "getLifecycleOwner().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    true
                ),
                TestConfig(
                    "DialogFragment()",
                    "getLifecycleOwner().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }",
                    false
                ),
            )
    }

    private val TEMPLATE = """
        package foo

        import androidx.lifecycle.Lifecycle
        import androidx.lifecycle.LifecycleOwner
        import androidx.lifecycle.repeatOnLifecycle
        import kotlinx.coroutines.CoroutineScope
        import kotlinx.coroutines.GlobalScope
        import androidx.fragment.app.Fragment

        class MyFragment : %s {
            fun onCreateView() {
                GlobalScope.launch {
                    %s
                }
            }
        }

        class BaseFragment : Fragment()
        interface FragmentListener
        interface OtherFragmentListener
    """.trimIndent()

    @Test
    fun basicTest() {
        val testLintResult = TestLintTask.lint()
            .files(
                *REPEAT_ON_LIFECYCLE_STUBS,
                TestFiles.kt(TEMPLATE.format(config.fragmentExtensions, config.apiCall))
            )
            .issues(UnsafeRepeatOnLifecycleDetector.ISSUE)
            .allowCompilationErrors(true) // b/193540422
            .run()

        if (config.shouldWarn) {
            /* ktlint-disable max-line-length */
            testLintResult.expect(
                """
                src/foo/MyFragment.kt:13: Error: The repeatOnLifecycle API should be used with viewLifecycleOwner [UnsafeRepeatOnLifecycleDetector]
                            ${config.apiCall}
                            ${"~".repeat(config.apiCall.length)}
                1 errors, 0 warnings
                """.trimIndent()
            )
            /* ktlint-enable max-line-length */
        } else {
            testLintResult.expectClean()
        }
    }
}
