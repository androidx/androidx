/*
 * Copyright 2019 The Android Open Source Project
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

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Class representing the different ways of dismissing a [DialogFragment]
 */
sealed class Operation {
    abstract fun run(dialogFragment: DialogFragment)

    override fun toString(): String = this.javaClass.simpleName
}

object ActivityFinish : Operation() {
    override fun run(dialogFragment: DialogFragment) {
        dialogFragment.requireActivity().finish()
    }
}

object FragmentDismiss : Operation() {
    override fun run(dialogFragment: DialogFragment) {
        dialogFragment.dismiss()
    }
}

object DialogDismiss : Operation() {
    override fun run(dialogFragment: DialogFragment) {
        dialogFragment.requireDialog().dismiss()
    }
}

object DialogCancel : Operation() {
    override fun run(dialogFragment: DialogFragment) {
        dialogFragment.requireDialog().cancel()
    }
}

@LargeTest
@RunWith(Parameterized::class)
class DialogFragmentDismissTest(
    private val operation: Operation,
    private val mainThread: Boolean
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "operation={0}, mainThread={1}")
        fun data() = mutableListOf<Array<Any>>().apply {
            arrayOf(
                ActivityFinish,
                FragmentDismiss,
                DialogDismiss,
                DialogCancel
            ).forEach { operation ->
                // Run the operation on the main thread
                add(arrayOf(operation, true))
                // Run the operation off the main thread
                add(arrayOf(operation, false))
            }
        }
    }

    @get:Rule
    val activityTestRule = ActivityTestRule(EmptyFragmentTestActivity::class.java)

    @Test
    fun testDialogFragmentDismiss() {
        val fragment = TestDialogFragment()
        activityTestRule.runOnUiThread {
            fragment.showNow(activityTestRule.activity.supportFragmentManager, null)
        }

        assertWithMessage("Dialog was not being shown")
            .that(fragment.dialog?.isShowing)
            .isTrue()

        var dialogIsNonNull = false
        var isShowing = false
        var onDismissCalledCount = 0
        val countDownLatch = CountDownLatch(3)
        activityTestRule.runOnUiThread {
            fragment.lifecycle.addObserver(GenericLifecycleObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    val dialog = fragment.dialog
                    dialogIsNonNull = dialog != null
                    isShowing = dialog != null && dialog.isShowing
                    countDownLatch.countDown()
                } else if (event == Lifecycle.Event.ON_DESTROY) {
                    onDismissCalledCount = fragment.onDismissCalledCount
                    countDownLatch.countDown()
                }
            })
        }
        var dismissOnMainThread = false
        var dismissCalled = false
        fragment.dismissCallback = {
            dismissCalled = true
            dismissOnMainThread = Looper.myLooper() == Looper.getMainLooper()
            countDownLatch.countDown()
        }

        if (mainThread) {
            activityTestRule.runOnUiThread {
                operation.run(fragment)
            }
        } else {
            operation.run(fragment)
        }

        assertWithMessage("Timed out waiting for ON_DESTROY")
            .that(countDownLatch.await(5, TimeUnit.SECONDS))
            .isTrue()

        assertWithMessage("Dialog should be dismissed")
            .that(dismissCalled)
            .isTrue()
        assertWithMessage("Dismiss should always be called on the main thread")
            .that(dismissOnMainThread)
            .isTrue()
        assertWithMessage("onDismiss() should be called before onDestroy()")
            .that(onDismissCalledCount)
            .isEqualTo(1)
        assertWithMessage("Dialog should not be null in onStop()")
            .that(dialogIsNonNull)
            .isTrue()

        if (operation is ActivityFinish) {
            assertWithMessage("Dialog should still be showing in onStop() during " +
                    "the normal lifecycle")
                .that(isShowing)
                .isTrue()
        } else {
            assertWithMessage("Dialog should not be showing in onStop() when manually dismissed")
                .that(isShowing)
                .isFalse()

            assertWithMessage("Dialog should be null after dismiss()")
                .that(fragment.dialog)
                .isNull()
        }
    }

    class TestDialogFragment : DialogFragment() {

        var onDismissCalledCount = 0
        var dismissCallback: () -> Unit = {}

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return AlertDialog.Builder(context)
                .setTitle("Test")
                .setMessage("Message")
                .setPositiveButton("Button", null)
                .create()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            onDismissCalledCount++
            dismissCallback.invoke()
        }
    }
}
