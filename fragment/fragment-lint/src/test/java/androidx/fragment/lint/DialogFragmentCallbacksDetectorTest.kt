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
class DialogFragmentCallbacksDetectorTest : LintDetectorTest() {

    override fun getDetector(): Detector = DialogFragmentCallbacksDetector()

    override fun getIssues(): MutableList<Issue> {
        return mutableListOf(DialogFragmentCallbacksDetector.ISSUE)
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

    private val dialogFragmentStubJava = java(
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
                    dialog.setOnDismissListener({ });
                    return dialog.create();
                }
                
                @Override
                public onDismiss(DialogInterface dialog) {
                    super.onDismiss(dialog);
                }
            }
            """
    ).indented()

    private val dialogFragmentStubKotlin = kotlin(
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
                    dialog.setOnDismissListener { }
                    return dialog.create()
                }

                override fun onDismiss(dialog: DialogInterface) {
                    super.onDismiss(dialog)
                }
            }
            """
    ).indented()

    @Test
    fun `java expect fail dialog fragment`() {
        lint().files(dialogFragmentStubJava)
            .run()
            .expect(
                """
src/foo/TestFragment.java:11: Warning: Use onCancel() and onDismiss() callbacks to get the instead of calling setOnCancelListener() and setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnCancelListener({ });
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/TestFragment.java:12: Warning: Use onCancel() and onDismiss() callbacks to get the instead of calling setOnCancelListener() and setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnDismissListener({ });
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
            .expectWarningCount(2)
    }

    @Test
    fun `java expect clean dialog fragment`() {
        lint().files(dialogFragmentCorrectImplementationStubJava)
            .run()
            .expectClean()
    }

    @Test
    fun `kotlin expect fail dialog fragment`() {
        lint().files(dialogFragmentStubKotlin)
            .run()
            .expect(
                """
src/foo/TestDialog.kt:10: Warning: Use onCancel() and onDismiss() callbacks to get the instead of calling setOnCancelListener() and setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnCancelListener { }
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/foo/TestDialog.kt:11: Warning: Use onCancel() and onDismiss() callbacks to get the instead of calling setOnCancelListener() and setOnDismissListener() from onCreateDialog() [DialogFragmentCallbacksDetector]
        dialog.setOnDismissListener { }
        ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
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
}
