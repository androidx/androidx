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

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class OnCreateDialogIncorrectCallbackDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = OnCreateDialogIncorrectCallbackDetector()

    override fun getIssues(): MutableList<Issue> {
        return mutableListOf(OnCreateDialogIncorrectCallbackDetector.ISSUE)
    }

    private val dialogFragmentCorrectImplementationStubJava = java(
        """
            package foo;
            import android.app.Dialog;
            import android.content.DialogInterface;
            import android.os.Bundle;
            import androidx.appcompat.app.AlertDialog;
            import androidx.fragment.app.DialogFragment;
            public class TestFragment extends DialogFragment {
                @NonNull
                @Override
                public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
                    Dialog dialog = AlertDialog.Builder(requireActivity());
                    return dialog.create();
                }
            }
            """
    ).indented()

    private val dialogFragmentCorrectImplementationStubKotlin = kotlin(
        """
            package foo
            import android.app.Dialog
            import android.content.DialogInterface
            import android.os.Bundle
            import androidx.appcompat.app.AlertDialog
            import androidx.fragment.app.DialogFragment
            class TestDialog : DialogFragment() {
                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                    val dialog = AlertDialog.Builder(requireActivity())
                    return dialog.create()
                }
                override fun onCancel(dialog: DialogInterface) {
                    super.onCancel(dialog)
                }

                override fun onDismiss(dialog: DialogInterface) {
                    super.onDismiss(dialog)
                }
            }
            """
    ).indented()

    private val dialogFragmentStubJavaWithCancelListener = java(
        """
            package foo;
            import android.app.Dialog;
            import android.content.DialogInterface;
            import android.os.Bundle;
            import androidx.appcompat.app.AlertDialog;
            import androidx.fragment.app.DialogFragment;
            public class TestFragment extends DialogFragment {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    Dialog dialog = AlertDialog.Builder(requireActivity());
                    dialog.setOnCancelListener({ });
                    return dialog.create();
                }
            }
            """
    ).indented()

    private val dialogFragmentStubJavaWithDismissListener = java(
        """
            package foo;
            import android.app.Dialog;
            import android.content.DialogInterface;
            import android.os.Bundle;
            import androidx.appcompat.app.AlertDialog;
            import androidx.fragment.app.DialogFragment;
            public class TestFragment extends DialogFragment {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    Dialog dialog = AlertDialog.Builder(requireActivity());
                    dialog.setOnDismissListener({ });
                    return dialog.create();
                }
            }
            """
    ).indented()

    private val dialogFragmentStubKotlinWithCancelListener = kotlin(
        """
            package foo
            import android.app.Dialog
            import android.content.DialogInterface
            import android.os.Bundle
            import androidx.appcompat.app.AlertDialog
            import androidx.fragment.app.DialogFragment
            class TestDialog : DialogFragment() {
                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                    val dialog = AlertDialog.Builder(requireActivity())
                    dialog.setOnCancelListener { }
                    return dialog.create()
                }

                override fun onDismiss(dialog: DialogInterface) {
                    super.onDismiss(dialog)
                }
            }
            """
    ).indented()

    private val dialogFragmentStubKotlinWithDismissListener = kotlin(
        """
            package foo
            import android.app.Dialog
            import android.content.DialogInterface
            import android.os.Bundle
            import androidx.appcompat.app.AlertDialog
            import androidx.fragment.app.DialogFragment
            class TestDialog : DialogFragment() {
                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                    val dialog = AlertDialog.Builder(requireActivity())
                    dialog.setOnDismissListener { }
                    return dialog.create()
                }

                override fun onCancel(dialog: DialogInterface) {
                    super.onCancel(dialog)
                }
            }
            """
    ).indented()

    private val dialogFragmentStubKotlinWithDismissAndCancelListeners = kotlin(
        """
            package foo
            import android.app.Dialog
            import android.content.DialogInterface
            import android.os.Bundle
            import androidx.appcompat.app.AlertDialog
            import androidx.fragment.app.DialogFragment
            class TestDialog : DialogFragment() {
                override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
                    val dialog = AlertDialog.Builder(requireActivity())
                    dialog.setOnDismissListener { }
                    dialog.setOnCancelListener { }
                    return dialog.create()
                }
            }
            """
    ).indented()

    @Test
    fun `java expect fail dialog fragment with cancel listener`() {
        lint().files(dialogFragmentStubJavaWithCancelListener)
            .run()
            .expect(
                """
src/foo/TestFragment.java:11: Warning: Use onCancel() instead of calling setOnCancelListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnCancelListener({ });
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectWarningCount(1)
    }

    @Test
    fun `java expect fail dialog fragment with dismiss listener`() {
        lint().files(dialogFragmentStubJavaWithDismissListener)
            .run()
            .expect(
                """
src/foo/TestFragment.java:11: Warning: Use onDismiss() instead of calling setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnDismissListener({ });
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectWarningCount(1)
    }

    @Test
    fun `java expect clean dialog fragment`() {
        lint().files(dialogFragmentCorrectImplementationStubJava)
            .run()
            .expectClean()
    }

    @Test
    fun `kotlin expect fail dialog fragment with cancel listener`() {
        lint().files(dialogFragmentStubKotlinWithCancelListener)
            .run()
            .expect(
                """
src/foo/TestDialog.kt:10: Warning: Use onCancel() instead of calling setOnCancelListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnCancelListener { }
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectWarningCount(1)
    }

    @Test
    fun `kotlin expect fail dialog fragment with dismiss listener`() {
        lint().files(dialogFragmentStubKotlinWithDismissListener)
            .run()
            .expect(
                """
src/foo/TestDialog.kt:10: Warning: Use onDismiss() instead of calling setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnDismissListener { }
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 1 warnings
            """
            )
            .expectWarningCount(1)
    }

    @Test
    fun `kotlin expect fail dialog fragment with dismiss and cancel listeners`() {
        lint().files(dialogFragmentStubKotlinWithDismissAndCancelListeners)
            .run()
            .expect(
                """
src/foo/TestDialog.kt:10: Warning: Use onDismiss() instead of calling setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnDismissListener { }
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/TestDialog.kt:11: Warning: Use onCancel() instead of calling setOnCancelListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnCancelListener { }
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
            .expectWarningCount(2)
    }

    @Test
    fun `kotlin expect clean dialog fragment`() {
        lint().files(dialogFragmentCorrectImplementationStubKotlin)
            .run()
            .expectClean()
    }

    @Test
    fun `kotlin empty interface clean`() {
        lint().files(
            kotlin(
                """
            package com.example

            class Foo
            """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun `java empty interface clean`() {
        lint().files(
            java(
                """
            package com.example;

            public class Foo {

            }
            """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun `kotlin anonymous object`() {
        lint().files(
            kotlin(
                """
            package com.example

            val foo = object : Foo() {
                override fun test() { }
            }
            """
            ).indented()
        )
            .run()
            .expectClean()
    }

    @Test
    fun `java anonymous object`() {
        lint().files(
            java(
                """
            package com.example;

            interface Foo {
                fun test();
            }

            class Bar {
                Foo foo = (com.example.Foo)()-> { };
            }
            """
            ).indented()
        )
            .run()
            .expectClean()
    }
}
