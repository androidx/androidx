/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation

import android.content.Context
import android.support.annotation.IdRes
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import java.util.Arrays
import java.util.NoSuchElementException

@RunWith(JUnit4::class)
@SmallTest
class NavGraphTest {

    companion object {
        @IdRes
        private const val FIRST_DESTINATION_ID = 1
        @IdRes
        private const val SECOND_DESTINATION_ID = 2
    }

    private lateinit var provider: NavigatorProvider
    private lateinit var noOpNavigator: NoOpNavigator
    private lateinit var navGraphNavigator: NavGraphNavigator

    @Before
    fun setup() {
        provider = NavigatorProvider().apply {
            addNavigator(NoOpNavigator().also { noOpNavigator = it })
            addNavigator(NavGraphNavigator(mock(Context::class.java)).also {
                navGraphNavigator = it
            })
        }
    }

    private fun createFirstDestination() = noOpNavigator.createDestination().apply {
        id = FIRST_DESTINATION_ID
    }

    private fun createSecondDestination() = noOpNavigator.createDestination().apply {
        id = SECOND_DESTINATION_ID
    }

    private fun createGraphWithDestination(destination: NavDestination) =
            navGraphNavigator.createDestination().apply {
                addDestination(destination)
            }

    private fun createGraphWithDestinations(vararg destinations: NavDestination) =
            navGraphNavigator.createDestination().apply {
                addDestinations(*destinations)
            }

    @Test(expected = IllegalArgumentException::class)
    fun addDestinationWithoutId() {
        val graph = navGraphNavigator.createDestination()
        val destination = noOpNavigator.createDestination()
        graph.addDestination(destination)
    }

    @Test
    fun addDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        assertThat(destination.parent).isEqualTo(graph)
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isEqualTo(destination)
    }

    @Test
    fun addDestinationsAsCollection() {
        val graph = navGraphNavigator.createDestination()
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        graph.addDestinations(Arrays.asList(destination, secondDestination))

        assertThat(destination.parent).isEqualTo(graph)
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isEqualTo(destination)
        assertThat(secondDestination.parent).isEqualTo(graph)
        assertThat(graph.findNode(SECOND_DESTINATION_ID)).isEqualTo(secondDestination)
    }

    @Test
    fun addDestinationsAsVarArgs() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        assertThat(destination.parent).isEqualTo(graph)
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isEqualTo(destination)
        assertThat(secondDestination.parent).isEqualTo(graph)
        assertThat(graph.findNode(SECOND_DESTINATION_ID)).isEqualTo(secondDestination)
    }

    @Test
    fun addReplacementDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        val replacementDestination = noOpNavigator.createDestination()
        replacementDestination.id = FIRST_DESTINATION_ID
        graph.addDestination(replacementDestination)

        assertThat(destination.parent).isNull()
        assertThat(replacementDestination.parent).isEqualTo(graph)
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isEqualTo(replacementDestination)
    }

    @Test(expected = IllegalStateException::class)
    fun addDestinationWithExistingParent() {
        val destination = createFirstDestination()
        createGraphWithDestination(destination)

        val other = navGraphNavigator.createDestination()
        other.addDestination(destination)
    }

    @Test
    fun addAll() {
        val destination = createFirstDestination()
        val other = createGraphWithDestination(destination)

        val graph = navGraphNavigator.createDestination()
        graph.addAll(other)

        assertThat(destination.parent).isEqualTo(graph)
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isEqualTo(destination)
        assertThat(other.findNode(FIRST_DESTINATION_ID)).isNull()
    }

    @Test
    fun removeDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        graph.remove(destination)

        assertThat(destination.parent).isNull()
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isNull()
    }

    @Test
    operator fun iterator() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        assertThat(graph.iterator().asSequence().asIterable())
            .containsExactly(destination, secondDestination)
    }

    @Test(expected = NoSuchElementException::class)
    fun iteratorNoSuchElement() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        val iterator = graph.iterator()
        iterator.next()
        iterator.next()
    }

    @Test
    fun iteratorRemove() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        val iterator = graph.iterator()
        val value = iterator.next()
        iterator.remove()
        assertThat(value.parent).isNull()
        assertThat(graph.findNode(value.id)).isNull()
    }

    @Test
    fun iteratorDoubleRemove() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        val iterator = graph.iterator()
        iterator.next()
        iterator.remove()
        val value = iterator.next()
        iterator.remove()
        assertThat(value.parent).isNull()
        assertThat(graph.findNode(value.id)).isNull()
    }

    @Test(expected = IllegalStateException::class)
    fun iteratorDoubleRemoveWithoutNext() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        val iterator = graph.iterator()
        iterator.next()
        iterator.remove()
        iterator.remove()
    }

    @Test
    fun clear() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        graph.clear()
        assertThat(destination.parent).isNull()
        assertThat(graph.findNode(FIRST_DESTINATION_ID)).isNull()
        assertThat(secondDestination.parent).isNull()
        assertThat(graph.findNode(SECOND_DESTINATION_ID)).isNull()
    }
}
