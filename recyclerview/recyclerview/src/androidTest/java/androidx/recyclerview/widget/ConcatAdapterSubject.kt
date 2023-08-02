/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.recyclerview.widget

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.ThrowableSubject
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage

/**
 * Helper subject to write nicer looking ConcatAdapter tests.
 */
internal class ConcatAdapterSubject(
    metadata: FailureMetadata,
    private val adapter: ConcatAdapter
) : Subject(
    metadata,
    adapter
) {
    fun hasItemCount(itemCount: Int) {
        assertThat(adapter.itemCount).isEqualTo(itemCount)
    }

    fun hasStateRestorationPolicy(policy: RecyclerView.Adapter.StateRestorationPolicy) {
        assertThat(adapter.stateRestorationPolicy).isEqualTo(policy)
    }

    fun bindView(
        recyclerView: RecyclerView,
        globalPosition: Int
    ): BindingSubject {
        if (recyclerView.adapter == null) {
            recyclerView.adapter = adapter
        } else {
            check(recyclerView.adapter == adapter) {
                "recyclerview is bound to another adapter"
            }
        }
        // clear state
        recyclerView.mState.apply {
            mItemCount = adapter.itemCount
            mLayoutStep = RecyclerView.State.STEP_LAYOUT
        }
        return assertAbout(
            BindingSubject.Factory(
                recyclerView = recyclerView
            )
        ).that(globalPosition)
    }

    fun canRestoreState() {
        assertThat(adapter.canRestoreState()).isTrue()
    }

    fun cannotRestoreState() {
        assertThat(adapter.canRestoreState()).isFalse()
    }

    fun throwsException(block: (ConcatAdapter) -> Unit): ThrowableSubject {
        val result = runCatching {
            block(adapter)
        }.exceptionOrNull()
        assertWithMessage("expected an exception").that(result).isNotNull()
        return assertThat(result)
    }

    fun hasItemIds(expectedIds: IntRange) = hasItemIds(expectedIds.toList())

    fun hasItemIds(expectedIds: Collection<Int>) {
        val existingIds = (0 until adapter.itemCount).map {
            adapter.getItemId(it)
        }
        assertThat(existingIds).containsExactlyElementsIn(expectedIds.map { it.toLong() }).inOrder()
    }

    fun hasStableIds() {
        assertWithMessage("should have stable ids").that(adapter.hasStableIds()).isTrue()
    }

    fun doesNotHaveStableIds() {
        assertWithMessage("should not have stable ids").that(adapter.hasStableIds()).isFalse()
    }

    object Factory : Subject.Factory<ConcatAdapterSubject, ConcatAdapter> {
        override fun createSubject(metadata: FailureMetadata, actual: ConcatAdapter):
            ConcatAdapterSubject {
                return ConcatAdapterSubject(
                    metadata = metadata,
                    adapter = actual
                )
            }
    }

    companion object {
        fun assertThat(concatAdapter: ConcatAdapter) =
            assertAbout(Factory).that(concatAdapter)
    }

    class BindingSubject(
        metadata: FailureMetadata,
        recyclerView: RecyclerView,
        globalPosition: Int
    ) : Subject(
        metadata,
        globalPosition
    ) {
        private val viewHolder by lazy {
            val view = recyclerView.mRecycler.getViewForPosition(globalPosition)
            val layoutParams = view.layoutParams
            check(layoutParams is RecyclerView.LayoutParams)
            val viewHolder = layoutParams.mViewHolder
            viewHolder as ConcatAdapterTest.ConcatAdapterViewHolder
        }

        internal fun verifyBoundTo(
            adapter: ConcatAdapterTest.NestedTestAdapter,
            localPosition: Int
        ) {
            assertThat(viewHolder.boundItem()).isEqualTo(adapter.getItemAt(localPosition))
            assertThat(viewHolder.boundLocalPosition()).isEqualTo(localPosition)
            assertThat(viewHolder.boundAdapter()).isSameInstanceAs(adapter)
        }

        class Factory(
            private val recyclerView: RecyclerView
        ) : Subject.Factory<BindingSubject, Int> {
            override fun createSubject(
                metadata: FailureMetadata,
                globalPosition: Int
            ):
                BindingSubject {
                    return BindingSubject(
                        metadata = metadata,
                        recyclerView = recyclerView,
                        globalPosition = globalPosition
                    )
                }
        }
    }
}
