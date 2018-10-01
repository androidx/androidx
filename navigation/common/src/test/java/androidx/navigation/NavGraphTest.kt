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
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    private lateinit var navGraphNavigator: NavGraphNavigator

    @Before
    fun setup() {
        navGraphNavigator = NavGraphNavigator(mock(Context::class.java))
    }

    private fun createFirstDestination() = NavDestination(mock(Navigator::class.java)).apply {
        id = FIRST_DESTINATION_ID
    }

    private fun createSecondDestination() = NavDestination(mock(Navigator::class.java)).apply {
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
        val destination = NavDestination(mock(Navigator::class.java))
        graph.addDestination(destination)
    }

    @Test
    fun addDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        assertEquals(graph, destination.parent)
        assertEquals(destination, graph.findNode(FIRST_DESTINATION_ID))
    }

    @Test
    fun addDestinationsAsCollection() {
        val graph = navGraphNavigator.createDestination()
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        graph.addDestinations(Arrays.asList(destination, secondDestination))

        assertEquals(graph, destination.parent)
        assertEquals(destination, graph.findNode(FIRST_DESTINATION_ID))
        assertEquals(graph, secondDestination.parent)
        assertEquals(secondDestination, graph.findNode(SECOND_DESTINATION_ID))
    }

    @Test
    fun addDestinationsAsVarArgs() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        assertEquals(graph, destination.parent)
        assertEquals(destination, graph.findNode(FIRST_DESTINATION_ID))
        assertEquals(graph, secondDestination.parent)
        assertEquals(secondDestination, graph.findNode(SECOND_DESTINATION_ID))
    }

    @Test
    fun addReplacementDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        val replacementDestination = NavDestination(mock(Navigator::class.java))
        replacementDestination.id = FIRST_DESTINATION_ID
        graph.addDestination(replacementDestination)

        assertNull(destination.parent)
        assertEquals(graph, replacementDestination.parent)
        assertEquals(replacementDestination, graph.findNode(FIRST_DESTINATION_ID))
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

        assertEquals(graph, destination.parent)
        assertEquals(destination, graph.findNode(FIRST_DESTINATION_ID))
        assertNull(other.findNode(FIRST_DESTINATION_ID))
    }

    @Test
    fun removeDestination() {
        val destination = createFirstDestination()
        val graph = createGraphWithDestination(destination)

        graph.remove(destination)

        assertNull(destination.parent)
        assertNull(graph.findNode(FIRST_DESTINATION_ID))
    }

    @Test
    operator fun iterator() {
        val destination = createFirstDestination()
        val secondDestination = createSecondDestination()
        val graph = createGraphWithDestinations(destination, secondDestination)

        val iterator = graph.iterator()
        assertTrue(iterator.hasNext())
        assertThat(iterator.next(), anyOf(`is`(destination), `is`(secondDestination)))
        assertTrue(iterator.hasNext())
        assertThat(iterator.next(), anyOf(`is`(destination), `is`(secondDestination)))
        assertFalse(iterator.hasNext())
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
        assertNull(value.parent)
        assertNull(graph.findNode(value.id))
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
        assertNull(value.parent)
        assertNull(graph.findNode(value.id))
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
        assertNull(destination.parent)
        assertNull(graph.findNode(FIRST_DESTINATION_ID))
        assertNull(secondDestination.parent)
        assertNull(graph.findNode(SECOND_DESTINATION_ID))
    }
}
