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
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFiles
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-enable max-line-length */
/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class UnsafeRepeatOnLifecycleDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = UnsafeRepeatOnLifecycleDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(UnsafeRepeatOnLifecycleDetector.ISSUE)

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
    fun `called directly from fragment`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("Fragment()", "repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expect(
                """
                src/foo/MyFragment.kt:13: Error: The repeatOnLifecycle API should be used with viewLifecycleOwner [UnsafeRepeatOnLifecycleDetector]
                            repeatOnLifecycle(Lifecycle.State.STARTED) { }
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `called with fragment lifecycle`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("Fragment()", "lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expect(
                """
                src/foo/MyFragment.kt:13: Error: The repeatOnLifecycle API should be used with viewLifecycleOwner [UnsafeRepeatOnLifecycleDetector]
                            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `called with fragment viewLifecycleOwner property`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("Fragment()", "viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `called with fragment viewLifecycleOwner function`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("Fragment()", "getViewLifecycleOwner().repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `called with fragment viewLifecycleOwner property lifecycle`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("Fragment()", "viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `called with fragment viewLifecycleOwner function lifecycle`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("Fragment()", "getViewLifecycleOwner().lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `called with extended fragment listener`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("BaseFragment(), FragmentListener", "repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expect(
                """
                src/foo/MyFragment.kt:13: Error: The repeatOnLifecycle API should be used with viewLifecycleOwner [UnsafeRepeatOnLifecycleDetector]
                            repeatOnLifecycle(Lifecycle.State.STARTED) { }
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `called with two extended fragment listeners`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("FragmentListener, OtherFragmentListener, BaseFragment()", "repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expect(
                """
                src/foo/MyFragment.kt:13: Error: The repeatOnLifecycle API should be used with viewLifecycleOwner [UnsafeRepeatOnLifecycleDetector]
                            repeatOnLifecycle(Lifecycle.State.STARTED) { }
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
                1 errors, 0 warnings
                """.trimIndent()
            )
    }

    @Test
    fun `called with dialog fragment`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            TestFiles.kt(
                TEMPLATE.format("DialogFragment()", "repeatOnLifecycle(Lifecycle.State.STARTED) { }")
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `viewLifecycleOwner in with outside of launch`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            kotlin(
                """
                    package foo
                    
                    import androidx.lifecycle.Lifecycle
                    import androidx.lifecycle.LifecycleOwner
                    import androidx.lifecycle.repeatOnLifecycle
                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.GlobalScope
                    import androidx.fragment.app.Fragment
                    
                    class MyFragment : Fragment() {
                        fun onCreateView() {
                            with(viewLifecycleOwner) {
                                lifecycleScope.launch {
                                    repeatOnLifecycle(Lifecycle.State.STARTED) {}
                                }       
                            }               
                        }
                    }
                """.trimIndent()
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }

    @Test
    fun `viewLifecycleOwner scope directly`() {
        lint().files(
            *REPEAT_ON_LIFECYCLE_STUBS,
            kotlin(
                """
                    package foo
                    
                    import androidx.lifecycle.Lifecycle
                    import androidx.lifecycle.LifecycleOwner
                    import androidx.lifecycle.repeatOnLifecycle
                    import kotlinx.coroutines.CoroutineScope
                    import kotlinx.coroutines.GlobalScope
                    import androidx.fragment.app.Fragment
                    
                    class MyFragment : Fragment() {
                        fun onCreateView() {
                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {}
                            }                    
                        }
                    }
                """.trimIndent()
            )
        )
            .allowCompilationErrors(false)
            .run()
            .expectClean()
    }
}
