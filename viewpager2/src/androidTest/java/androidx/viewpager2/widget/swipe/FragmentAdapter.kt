/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.util.concurrent.atomic.AtomicInteger

private const val ARG_KEY = "key"

class FragmentAdapter(
    fragmentManager: FragmentManager,
    private val items: List<String>
) : FragmentStateAdapter(fragmentManager) {
    val attachCount = AtomicInteger(0)
    val destroyCount = AtomicInteger(0)

    override fun getItem(position: Int): Fragment = PageFragment().apply {
        arguments = Bundle(1).apply { putString(ARG_KEY, items[position]) }
        onAttachListener = { attachCount.incrementAndGet() }
        onDestroyListener = { destroyCount.incrementAndGet() }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemId(position: Int): Long {
        // more than position can represent, so a good test if ids are used consistently
        // TODO:
        // in tests, use both:
        // - default implementation (position)
        // - the below
        return position + MORE_THAN_INT32_OFFSET
    }

    override fun containsItem(itemId: Long): Boolean {
        val position = itemId - MORE_THAN_INT32_OFFSET
        return position in 0..(itemCount - 1)
    }
}

private const val MORE_THAN_INT32_OFFSET = 3L * Int.MAX_VALUE

class PageFragment : Fragment() {
    var onAttachListener: () -> Unit = {}
    var onDestroyListener: () -> Unit = {}

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = PageView.inflatePage(container!!)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setValue(when {
            savedInstanceState != null -> savedInstanceState.getString(ARG_KEY)
            arguments != null -> arguments!!.getString(ARG_KEY)
            else -> throw IllegalStateException()
        })
    }

    fun setValue(value: String) {
        PageView.setPageText(view!!, value)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ARG_KEY, PageView.getPageText(view!!))
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        onAttachListener()
    }

    override fun onDestroy() {
        onDestroyListener()
        super.onDestroy()
    }
}
