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

package androidx.navigation;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.support.annotation.IdRes;
import android.support.test.filters.SmallTest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
@SmallTest
public class NavGraphTest {
    @IdRes
    private static final int FIRST_DESTINATION_ID = 1;
    @IdRes
    private static final int SECOND_DESTINATION_ID = 2;

    private NavGraphNavigator mNavGraphNavigator;

    @Before
    public void setup() {
        mNavGraphNavigator = new NavGraphNavigator(mock(Context.class));
    }

    private NavDestination createFirstDestination() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.setId(FIRST_DESTINATION_ID);
        return destination;
    }

    private NavDestination createSecondDestination() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.setId(SECOND_DESTINATION_ID);
        return destination;
    }

    private NavGraph createGraphWithDestination(NavDestination destination) {
        NavGraph graph = mNavGraphNavigator.createDestination();
        graph.addDestination(destination);
        return graph;
    }

    private NavGraph createGraphWithDestinations(NavDestination... destinations) {
        NavGraph graph = mNavGraphNavigator.createDestination();
        graph.addDestinations(destinations);
        return graph;
    }

    @Test(expected = IllegalArgumentException.class)
    public void addDestinationWithoutId() {
        NavGraph graph = mNavGraphNavigator.createDestination();
        NavDestination destination = new NavDestination(mock(Navigator.class));
        graph.addDestination(destination);
    }

    @Test
    public void addDestination() {
        NavDestination destination = createFirstDestination();
        NavGraph graph = createGraphWithDestination(destination);

        assertThat(destination.getParent(), is(graph));
        assertThat(graph.findNode(FIRST_DESTINATION_ID), is(destination));
    }

    @Test
    public void addDestinationsAsCollection() {
        NavGraph graph = mNavGraphNavigator.createDestination();
        NavDestination destination = createFirstDestination();
        NavDestination secondDestination = createSecondDestination();
        graph.addDestinations(Arrays.asList(destination, secondDestination));

        assertThat(destination.getParent(), is(graph));
        assertThat(graph.findNode(FIRST_DESTINATION_ID), is(destination));
        assertThat(secondDestination.getParent(), is(graph));
        assertThat(graph.findNode(SECOND_DESTINATION_ID), is(secondDestination));
    }

    @Test
    public void addDestinationsAsVarArgs() {
        NavDestination destination = createFirstDestination();
        NavDestination secondDestination = createSecondDestination();
        NavGraph graph = createGraphWithDestinations(destination, secondDestination);

        assertThat(destination.getParent(), is(graph));
        assertThat(graph.findNode(FIRST_DESTINATION_ID), is(destination));
        assertThat(secondDestination.getParent(), is(graph));
        assertThat(graph.findNode(SECOND_DESTINATION_ID), is(secondDestination));
    }

    @Test
    public void addReplacementDestination() {
        NavDestination destination = createFirstDestination();
        NavGraph graph = createGraphWithDestination(destination);

        NavDestination replacementDestination = new NavDestination(mock(Navigator.class));
        replacementDestination.setId(FIRST_DESTINATION_ID);
        graph.addDestination(replacementDestination);

        assertThat(destination.getParent(), nullValue());
        assertThat(replacementDestination.getParent(), is(graph));
        assertThat(graph.findNode(FIRST_DESTINATION_ID), is(replacementDestination));
    }

    @Test(expected = IllegalStateException.class)
    public void addDestinationWithExistingParent() {
        NavDestination destination = createFirstDestination();
        createGraphWithDestination(destination);

        NavGraph other = mNavGraphNavigator.createDestination();
        other.addDestination(destination);
    }

    @Test
    public void addAll() {
        NavDestination destination = createFirstDestination();
        NavGraph other = createGraphWithDestination(destination);

        NavGraph graph = mNavGraphNavigator.createDestination();
        graph.addAll(other);

        assertThat(destination.getParent(), is(graph));
        assertThat(graph.findNode(FIRST_DESTINATION_ID), is(destination));
        assertThat(other.findNode(FIRST_DESTINATION_ID), nullValue());
    }

    @Test
    public void removeDestination() {
        NavDestination destination = createFirstDestination();
        NavGraph graph = createGraphWithDestination(destination);

        graph.remove(destination);

        assertThat(destination.getParent(), nullValue());
        assertThat(graph.findNode(FIRST_DESTINATION_ID), nullValue());
    }

    @Test
    public void iterator() {
        NavDestination destination = createFirstDestination();
        NavDestination secondDestination = createSecondDestination();
        NavGraph graph = createGraphWithDestinations(destination, secondDestination);

        Iterator<NavDestination> iterator = graph.iterator();
        assertTrue(iterator.hasNext());
        assertThat(iterator.next(), anyOf(is(destination), is(secondDestination)));
        assertTrue(iterator.hasNext());
        assertThat(iterator.next(), anyOf(is(destination), is(secondDestination)));
        assertFalse(iterator.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void iteratorNoSuchElement() {
        NavDestination destination = createFirstDestination();
        NavGraph graph = createGraphWithDestination(destination);

        Iterator<NavDestination> iterator = graph.iterator();
        iterator.next();
        iterator.next();
    }

    @Test
    public void iteratorRemove() {
        NavDestination destination = createFirstDestination();
        NavGraph graph = createGraphWithDestination(destination);

        Iterator<NavDestination> iterator = graph.iterator();
        NavDestination value = iterator.next();
        iterator.remove();
        assertThat(value.getParent(), nullValue());
        assertThat(graph.findNode(value.getId()), nullValue());
    }

    @Test
    public void iteratorDoubleRemove() {
        NavDestination destination = createFirstDestination();
        NavDestination secondDestination = createSecondDestination();
        NavGraph graph = createGraphWithDestinations(destination, secondDestination);

        Iterator<NavDestination> iterator = graph.iterator();
        iterator.next();
        iterator.remove();
        NavDestination value = iterator.next();
        iterator.remove();
        assertThat(value.getParent(), nullValue());
        assertThat(graph.findNode(value.getId()), nullValue());
    }

    @Test(expected = IllegalStateException.class)
    public void iteratorDoubleRemoveWithoutNext() {
        NavDestination destination = createFirstDestination();
        NavDestination secondDestination = createSecondDestination();
        NavGraph graph = createGraphWithDestinations(destination, secondDestination);

        Iterator<NavDestination> iterator = graph.iterator();
        iterator.next();
        iterator.remove();
        iterator.remove();
    }

    @Test
    public void clear() {
        NavDestination destination = createFirstDestination();
        NavDestination secondDestination = createSecondDestination();
        NavGraph graph = createGraphWithDestinations(destination, secondDestination);

        graph.clear();
        assertThat(destination.getParent(), nullValue());
        assertThat(graph.findNode(FIRST_DESTINATION_ID), nullValue());
        assertThat(secondDestination.getParent(), nullValue());
        assertThat(graph.findNode(SECOND_DESTINATION_ID), nullValue());
    }
}
