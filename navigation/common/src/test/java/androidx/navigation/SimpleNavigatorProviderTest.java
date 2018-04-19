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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import android.support.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
@SmallTest
public class SimpleNavigatorProviderTest {
    @Test
    public void addWithMissingAnnotationName() {
        SimpleNavigatorProvider provider = new SimpleNavigatorProvider();
        Navigator navigator = new NoNameNavigator();
        try {
            provider.addNavigator(navigator);
            fail("Adding a provider with no @Navigator.Name should cause an "
                    + "IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void addWithMissingAnnotationNameGetWithExplicitName() {
        SimpleNavigatorProvider provider = new SimpleNavigatorProvider();
        Navigator navigator = new NoNameNavigator();
        provider.addNavigator("name", navigator);
        assertThat(provider.getNavigator("name"), is(navigator));
    }

    @Test
    public void addWithExplicitNameGetWithExplicitName() {
        SimpleNavigatorProvider provider = new SimpleNavigatorProvider();
        Navigator navigator = new EmptyNavigator();
        provider.addNavigator("name", navigator);
        assertThat(provider.getNavigator("name"), is(navigator));
        try {
            provider.getNavigator(EmptyNavigator.class);
            fail("getNavigator(Class) with an invalid name should cause an IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void addWithExplicitNameGetWithMissingAnnotationName() {
        SimpleNavigatorProvider provider = new SimpleNavigatorProvider();
        Navigator navigator = new NoNameNavigator();
        provider.addNavigator("name", navigator);
        try {
            provider.getNavigator(NoNameNavigator.class);
            fail("getNavigator(Class) with no @Navigator.Name should cause "
                    + "an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void addWithAnnotationNameGetWithAnnotationName() {
        SimpleNavigatorProvider provider = new SimpleNavigatorProvider();
        Navigator navigator = new EmptyNavigator();
        provider.addNavigator(navigator);
        assertThat(provider.getNavigator(EmptyNavigator.class), is(navigator));
    }

    @Test
    public void addWithAnnotationNameGetWithExplicitName() {
        SimpleNavigatorProvider provider = new SimpleNavigatorProvider();
        Navigator navigator = new EmptyNavigator();
        provider.addNavigator(navigator);
        assertThat(provider.getNavigator(EmptyNavigator.NAME), is(navigator));
    }
}
