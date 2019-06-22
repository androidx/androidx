/*
 * Copyright 2018 The Android Open Source Project
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
package androidx.fragment.app.test

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.test.R
import java.util.concurrent.CountDownLatch

/**
 * A simple activity used for Fragment Transitions and lifecycle event ordering
 */
class FragmentTestActivity : FragmentActivity(R.layout.activity_content) {

    val finishCountDownLatch = CountDownLatch(1)

    override fun finish() {
        super.finish()
        finishCountDownLatch.countDown()
    }

    class ParentFragment : Fragment() {
        var wasAttachedInTime: Boolean = false

        var retainChildInstance: Boolean = false

        val childFragment: ChildFragment
            get() = childFragmentManager.findFragmentByTag(CHILD_FRAGMENT_TAG) as ChildFragment

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            if (childFragmentManager.findFragmentByTag(CHILD_FRAGMENT_TAG) == null) {
                childFragmentManager.beginTransaction()
                    .add(ChildFragment().apply {
                        if (retainChildInstance) {
                            retainInstance = true
                        }
                    }, CHILD_FRAGMENT_TAG)
                    .commitNow()
            }
            wasAttachedInTime = childFragment.attached
        }

        companion object {
            internal const val CHILD_FRAGMENT_TAG = "childFragment"
        }
    }

    class ChildFragment : Fragment() {
        var onAttachListener: (context: Context) -> Unit = {}

        var attached: Boolean = false
        var onActivityResultCalled: Boolean = false
        var onActivityResultRequestCode: Int = 0
        var onActivityResultResultCode: Int = 0

        override fun onAttach(context: Context) {
            super.onAttach(context)
            attached = true
            onAttachListener.invoke(context)
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            onActivityResultCalled = true
            onActivityResultRequestCode = requestCode
            onActivityResultResultCode = resultCode
        }
    }
}
