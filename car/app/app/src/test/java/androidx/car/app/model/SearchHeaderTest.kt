/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.car.app.model

import androidx.car.app.OnDoneCallback
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class SearchHeaderTest {
    private val mockCallback = mock(SearchCallback::class.java)

    @Test
    fun build_mandatoryCallback() {
        val searchHeader = SearchHeader.Builder(mockCallback).build()

        assertThat(searchHeader.searchCallbackDelegate).isNotNull()
    }

    @Test
    fun setInitialSearchText() {
        val initialSearchText = "initial text"
        val searchHeader =
            SearchHeader.Builder(mockCallback).setInitialSearchText(initialSearchText).build()

        assertThat(searchHeader.initialSearchText).isEqualTo(initialSearchText)
    }

    @Test
    fun setSearchHint() {
        val searchHint = "hint"
        val searchHeader = SearchHeader.Builder(mockCallback).setSearchHint(searchHint).build()

        assertThat(searchHeader.searchHint).isEqualTo(searchHint)
    }

    @Test
    fun setShowKeyboardByDefault() {
        val searchHeader =
            SearchHeader.Builder(mockCallback).setShowKeyboardByDefault(false).build()

        assertThat(searchHeader.isShowKeyboardByDefault).isFalse()
    }

    @Test
    fun setStartHeaderAction() {
        val searchHeader =
            SearchHeader.Builder(mockCallback).setStartHeaderAction(Action.BACK).build()

        assertThat(searchHeader.startHeaderAction).isEqualTo(Action.BACK)
    }

    @Test
    fun setStartHeaderAction_invalidAction_throws() {
        val action = Action.Builder().setTitle("Invalid").build() // No icon, not allowed in header

        assertThrows(IllegalArgumentException::class.java) {
            SearchHeader.Builder(mockCallback).setStartHeaderAction(action)
        }
    }

    @Test
    fun setEndHeaderActions() {
        val action1 = Action.Builder().setIcon(CarIcon.APP_ICON).build()
        val action2 = Action.Builder().setIcon(CarIcon.BACK).build()
        val actions = listOf(action1, action2)
        val searchHeader = SearchHeader.Builder(mockCallback).setEndHeaderActions(actions).build()

        assertThat(searchHeader.endHeaderActions).containsExactly(action1, action2).inOrder()
    }

    @Test
    fun sendSearchTextChanged() {
        val searchHeader = SearchHeader.Builder(mockCallback).build()
        val onDoneCallback = mock(OnDoneCallback::class.java)
        val searchText = "text"

        searchHeader.searchCallbackDelegate.sendSearchTextChanged(searchText, onDoneCallback)

        verify(mockCallback).onSearchTextChanged(searchText)
        verify(onDoneCallback).onSuccess(null)
    }

    @Test
    fun sendSearchSubmitted() {
        val searchHeader = SearchHeader.Builder(mockCallback).build()
        val onDoneCallback = mock(OnDoneCallback::class.java)
        val searchText = "text"

        searchHeader.searchCallbackDelegate.sendSearchSubmitted(searchText, onDoneCallback)

        verify(mockCallback).onSearchSubmitted(searchText)
        verify(onDoneCallback).onSuccess(null)
    }

    @Test
    fun equals() {
        val searchHeader1 =
            SearchHeader.Builder(mockCallback)
                .setInitialSearchText("text")
                .setSearchHint("hint")
                .setShowKeyboardByDefault(true)
                .setStartHeaderAction(Action.BACK)
                .setEndHeaderActions(listOf(Action.APP_ICON))
                .build()
        val searchHeader2 =
            SearchHeader.Builder(mockCallback)
                .setInitialSearchText("text")
                .setSearchHint("hint")
                .setShowKeyboardByDefault(true)
                .setStartHeaderAction(Action.BACK)
                .setEndHeaderActions(listOf(Action.APP_ICON))
                .build()

        assertThat(searchHeader1).isEqualTo(searchHeader2)
        assertThat(searchHeader1.hashCode()).isEqualTo(searchHeader2.hashCode())
    }

    @Test
    fun notEquals() {
        val searchHeader =
            SearchHeader.Builder(mockCallback)
                .setInitialSearchText("text")
                .setSearchHint("hint")
                .setShowKeyboardByDefault(true)
                .setStartHeaderAction(Action.BACK)
                .build()

        assertThat(searchHeader)
            .isNotEqualTo(
                SearchHeader.Builder(mockCallback)
                    .setInitialSearchText("different")
                    .setSearchHint("hint")
                    .setShowKeyboardByDefault(true)
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
        assertThat(searchHeader)
            .isNotEqualTo(
                SearchHeader.Builder(mockCallback)
                    .setInitialSearchText("text")
                    .setSearchHint("different")
                    .setShowKeyboardByDefault(true)
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
        assertThat(searchHeader)
            .isNotEqualTo(
                SearchHeader.Builder(mockCallback)
                    .setInitialSearchText("text")
                    .setSearchHint("hint")
                    .setShowKeyboardByDefault(false)
                    .setStartHeaderAction(Action.BACK)
                    .build()
            )
        assertThat(searchHeader)
            .isNotEqualTo(
                SearchHeader.Builder(mockCallback)
                    .setInitialSearchText("text")
                    .setSearchHint("hint")
                    .setShowKeyboardByDefault(true)
                    .setStartHeaderAction(Action.APP_ICON)
                    .build()
            )
        assertThat(searchHeader)
            .isNotEqualTo(
                SearchHeader.Builder(mockCallback)
                    .setInitialSearchText("text")
                    .setSearchHint("hint")
                    .setShowKeyboardByDefault(true)
                    .setStartHeaderAction(Action.BACK)
                    .setEndHeaderActions(listOf(Action.APP_ICON))
                    .build()
            )
    }
}
