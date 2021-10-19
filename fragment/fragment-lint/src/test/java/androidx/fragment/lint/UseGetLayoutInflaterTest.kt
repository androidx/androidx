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

import androidx.fragment.lint.stubs.DIALOG_FRAGMENT
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-enable max-line-length */
/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class UseGetLayoutInflaterTest : LintDetectorTest() {

    override fun getDetector(): Detector = UseGetLayoutInflater()

    override fun getIssues(): MutableList<Issue> = mutableListOf(UseGetLayoutInflater.ISSUE)

    private val dialogFragmentCorrectImplementationStubJava = java(
        """
            package foo;
            import android.os.Bundle;
            import android.view.View;
            import android.view.ViewGroup;
            import androidx.annotation.NonNull;
            import androidx.annotation.Nullable;
            import androidx.fragment.app.DialogFragment;
            public class TestFragment extends DialogFragment {
                @NonNull
                @Override
                public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
                    getLayoutInflater().inflate(R.layout.some_layout, null);
                    return super.onCreateDialog(savedInstanceState);
                }
            }
            """
    ).indented()

    private val dialogFragmentCorrectImplementationStubKotlin = kotlin(
        """
            package foo
            import android.app.Dialog
            import android.os.Bundle
            import androidx.fragment.app.DialogFragment
            class Test : DialogFragment() {
                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                    layoutInflater.inflate(R.layout.some_layout, null)
                    return super.onCreateDialog(savedInstanceState)
                }
            }
            """
    ).indented()

    private val dialogFragmentStubJava = java(
        """
            package foo;
            import android.os.Bundle;
            import android.view.LayoutInflater;
            import android.view.LayoutInflater123;
            import android.view.View;
            import android.view.ViewGroup;
            import androidx.annotation.NonNull;
            import androidx.annotation.Nullable;
            import androidx.fragment.app.DialogFragment;
            public class TestFragment extends DialogFragment {
                @Nullable
                @Override
                public View onCreateView(@NonNull LayoutInflater inflater,
                                            @Nullable ViewGroup container,
                                            @Nullable Bundle savedInstanceState) {
                    LayoutInflater li = LayoutInflater.from(requireContext());
                    // this  will not be triggered
                    LayoutInflater123 li123 = LayoutInflater123.from(requireContext());
                    return super.onCreateView(inflater, container, savedInstanceState);
                }
            }
            """
    ).indented()

    private val fragmentStubJava = java(
        """
            package foo;
            import android.os.Bundle;
            import android.view.LayoutInflater;
            import android.view.View;
            import android.view.ViewGroup;
            import androidx.annotation.NonNull;
            import androidx.annotation.Nullable;
            import androidx.fragment.app.DialogFragment;
            public class TestFragment extends Fragment {
                @Nullable
                @Override
                public View onCreateView(@NonNull LayoutInflater inflater,
                                            @Nullable ViewGroup container,
                                            @Nullable Bundle savedInstanceState) {
                    LayoutInflater li = LayoutInflater.from(requireContext());
                    return super.onCreateView(inflater, container, savedInstanceState);
                }
            }
            """
    ).indented()

    private val dialogFragmentStubKotlin = kotlin(
        """
            package foo
            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.annotation.NonNull
            import androidx.annotation.Nullable
            import androidx.fragment.app.DialogFragment
            class TestFragment : DialogFragment() {
                override fun onCreateView(inflater: LayoutInflater,
                                            container: ViewGroup?,
                                            savedInstanceState: Bundle?): View? {
                    val li = LayoutInflater.from(requireContext())
                    return super.onCreateView(inflater, container, savedInstanceState)
                }
            }
            """
    ).indented()

    private val fragmentStubKotlin = kotlin(
        """
            package foo
            import android.os.Bundle
            import android.view.LayoutInflater
            import android.view.View
            import android.view.ViewGroup
            import androidx.annotation.NonNull
            import androidx.annotation.Nullable
            import androidx.fragment.app.DialogFragment
            class TestFragment : Fragment() {
                fun someFunction() {
                    val li = LayoutInflater.from(requireContext())
                }
            }
            """
    ).indented()

    @Test
    fun `java expect fail dialog fragment with fix`() {
        lint().files(dialogFragmentStubJava)
            .allowCompilationErrors(true) // b/193540422
            .run()
            .expect(
                """
src/foo/TestFragment.java:16: Warning: Use of LayoutInflater.from(requireContext()) detected. Consider using getLayoutInflater() instead [UseGetLayoutInflater]
        LayoutInflater li = LayoutInflater.from(requireContext());
                            ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectWarningCount(1)
            .expectFixDiffs(
                """
                    Fix for src/foo/TestFragment.java line 16: Replace with getLayoutInflater():
                    @@ -16 +16
                    -         LayoutInflater li = LayoutInflater.from(requireContext());
                    +         LayoutInflater li = getLayoutInflater();
                """.trimIndent()
            )
    }

    @Test
    fun `java expect clean non dialog fragment`() {
        lint().files(fragmentStubJava, DIALOG_FRAGMENT)
            .run()
            .expectClean()
    }

    @Test
    fun `java expect clean dialog fragment`() {
        lint().files(dialogFragmentCorrectImplementationStubJava, DIALOG_FRAGMENT)
            .run()
            .expectClean()
    }

    @Test
    fun `kotlin expect fail dialog fragment`() {
        lint().files(dialogFragmentStubKotlin, DIALOG_FRAGMENT)
            .run()
            .expect(
                """
src/foo/TestFragment.kt:13: Warning: Use of LayoutInflater.from(Context) detected. Consider using layoutInflater instead [UseGetLayoutInflater]
        val li = LayoutInflater.from(requireContext())
                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectWarningCount(1)
    }

    @Test
    fun `kotlin expect clean non dialog fragment`() {
        lint().files(fragmentStubKotlin, DIALOG_FRAGMENT)
            .run()
            .expectClean()
    }

    @Test
    fun `kotlin expect clean dialog fragment`() {
        lint().files(dialogFragmentCorrectImplementationStubKotlin, DIALOG_FRAGMENT)
            .run()
            .expectClean()
    }
}
