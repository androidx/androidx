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

package androidx.viewpager2.widget.swipe

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.junit.Assert.assertThat
import java.util.concurrent.atomic.AtomicInteger

private const val ARG_KEY = "key"

class FragmentAdapter(
    fragmentActivity: FragmentActivity,
    private val items: List<String>
) : FragmentStateAdapter(fragmentActivity), SelfChecking {
    private val attachCount = AtomicInteger(0)
    private val destroyCount = AtomicInteger(0)

    override fun getItem(position: Int): Fragment = PageFragment().apply {
        arguments = Bundle(1).apply { putString(ARG_KEY, items[position]) }
        onAttachListener = { attachCount.incrementAndGet() }
        onDestroyListener = { destroyCount.incrementAndGet() }
    }

    override fun getItemCount(): Int = items.size

    /** easy way of dynamically overriding [getItemCount] and [containsItem] */
    var positionToItemId: (Int) -> Long = { position -> super.getItemId(position) }
    var itemIdToContains: (Long) -> Boolean = { itemId -> super.containsItem(itemId) }
    override fun getItemId(position: Int): Long = positionToItemId(position)
    override fun containsItem(itemId: Long): Boolean = itemIdToContains(itemId)

    override fun selfCheck() =
        /** Detects [Fragment] 'memory leaks'. Core premise of [FragmentStateAdapter] is to keep
         * only a handful of [Fragment]s alive and handle the rest via state save/restore. */
        assertThat(
            "Number of alive fragments must be between 0 and 4",
            attachCount.get() - destroyCount.get(),
            allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(4))
        )
}

class PageFragment : Fragment() {
    var onAttachListener: () -> Unit = {}
    var onDestroyListener: () -> Unit = {}

    override fun onCreateView(
        layoutInflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = PageView.inflatePage(layoutInflater, container)

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
