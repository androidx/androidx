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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;

import androidx.navigation.test.R;
import androidx.navigation.testing.TestNavigatorProvider;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class NavInflaterTest {
    private Instrumentation mInstrumentation;

    @Before
    public void getInstrumentation() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    @Test
    public void testInflateSimple() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavInflater navInflater = new NavInflater(context, new TestNavigatorProvider(context));
        NavGraph graph = navInflater.inflate(R.navigation.nav_simple);

        assertThat(graph, is(notNullValue(NavGraph.class)));
        assertThat(graph.getStartDestination(), is(R.id.start_test));
    }

    @Test
    public void testInflateDeepLinkWithApplicationId() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavInflater navInflater = new NavInflater(context, new TestNavigatorProvider(context));
        NavGraph graph = navInflater.inflate(R.navigation.nav_simple);

        assertThat(graph, is(notNullValue(NavGraph.class)));
        Uri expectedUri = Uri.parse("android-app://"
                + mInstrumentation.getTargetContext().getPackageName() + "/test");
        Pair<NavDestination, Bundle> result = graph.matchDeepLink(expectedUri);
        assertThat(result, is(notNullValue()));
        assert result != null;
        assertThat(result.first, is(notNullValue(NavDestination.class)));
        assertThat(result.first.getId(), is(R.id.second_test));
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
        int expectedValue = ContextCompat.getColor(context, R.color.test_reference_arg);

        assertThat(defaultArguments.getInt("test_reference"), is(expectedValue));
    }

    private Bundle inflateDefaultArgumentsFromGraph() {
        Context context = InstrumentationRegistry.getTargetContext();
        NavInflater navInflater = new NavInflater(context, new TestNavigatorProvider(context));
        NavGraph graph = navInflater.inflate(R.navigation.nav_default_arguments);

        NavDestination startDestination = graph.findNode(graph.getStartDestination());
        Bundle defaultArguments = startDestination.getDefaultArguments();

        assertThat(defaultArguments, is(notNullValue(Bundle.class)));
        return defaultArguments;
    }
}
