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
package androidx.fragment.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.fragment.test.R

/**
 * Counts the number of onCreateView, onHiddenChanged (onHide, onShow), onAttach, and onDetach
 * calls.
 */
class CountCallsFragment(
    @LayoutRes contentLayoutId: Int = R.layout.strict_view_fragment
) : StrictViewFragment(contentLayoutId) {
    var onCreateViewCount = 0
    var onDestroyViewCount = 0
    var onHideCount = 0
    var onShowCount = 0
    var onAttachCount = 0
    var onDetachCount = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        onCreateViewCount++
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        if (hidden) {
            onHideCount++
        } else {
            onShowCount++
        }
        super.onHiddenChanged(hidden)
    }

    override fun onAttach(context: Context) {
        onAttachCount++
        super.onAttach(context)
    }

    override fun onDetach() {
        onDetachCount++
        super.onDetach()
    }

    override fun onDestroyView() {
        onDestroyViewCount++
        super.onDestroyView()
    }
}
