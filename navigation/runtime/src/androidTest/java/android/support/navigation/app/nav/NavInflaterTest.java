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

package android.support.navigation.app.nav;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;

import com.android.support.navigation.test.R;

import org.junit.Test;

@SmallTest
public class NavInflaterTest {

    @Test
    public void testInflateSimple() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavInflater navInflater = new NavInflater(context, new TestNavigatorProvider(context));
        NavGraph graph = navInflater.inflate(R.xml.nav_simple);

        assertThat(graph, is(notNullValue(NavGraph.class)));
        assertThat(graph.getStartDestination(), is(R.id.start_test));
    }

    @Test
    public void testDefaultArgumentsInteger() {
        Bundle defaultArguments = inflateDefaultArgumentsFromGraph();

        assertThat(defaultArguments.getInt("test_int"), is(12));
    }

    @Test
    public void testDefaultArgumentsDimen() {
        Bundle defaultArguments = inflateDefaultArgumentsFromGraph();
        Context context = InstrumentationRegistry.getTargetContext();
        int expectedValue = context.getResources().getDimensionPixelSize(R.dimen.test_dimen_arg);

        assertThat(defaultArguments.getInt("test_dimen"), is(expectedValue));
    }

    @Test
    public void testDefaultArgumentsFloat() {
        Bundle defaultArguments = inflateDefaultArgumentsFromGraph();

        assertThat(defaultArguments.getFloat("test_float"), is(3.14f));
    }

    @Test
    public void testDefaultArgumentsReference() {
        Bundle defaultArguments = inflateDefaultArgumentsFromGraph();
        Context context = InstrumentationRegistry.getTargetContext();
        int expectedValue = context.getColor(R.color.test_reference_arg);

        assertThat(defaultArguments.getInt("test_reference"), is(expectedValue));
    }

    private Bundle inflateDefaultArgumentsFromGraph() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavInflater navInflater = new NavInflater(context, new TestNavigatorProvider(context));
        NavGraph graph = navInflater.inflate(R.xml.nav_default_arguments);

        NavDestination startDestination = graph.findNode(graph.getStartDestination());
        Bundle defaultArguments = startDestination.getDefaultArguments();

        assertThat(defaultArguments, is(notNullValue(Bundle.class)));
        return defaultArguments;
    }
}
