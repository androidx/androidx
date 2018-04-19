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

package android.arch.navigation.integration.flavor.foo;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.arch.navigation.safeargs.testapp.R;
import android.arch.navigation.safeargs.testapp.foo.MainFragmentDirections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * this is teeeeeeeeeest
 */
@RunWith(JUnit4.class)
public class FlavorDestinationTest {

    /**
     * teeeest
     */
    @Test
    public void destinationsTest() {
        MainFragmentDirections.Foo directions = MainFragmentDirections.foo("foo", "some");
        assertThat(directions.getDestinationId(), is(R.id.foo_fragment));
    }
}
