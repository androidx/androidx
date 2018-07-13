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
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.support.annotation.IdRes;

import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

@SuppressWarnings("unchecked")
@RunWith(JUnit4.class)
@SmallTest
public class NavDestinationTest {
    @IdRes
    private static final int INVALID_ACTION_ID = 0;
    @IdRes
    private static final int ACTION_ID = 1;
    @IdRes
    private static final int DESTINATION_ID = 1;

    @Test
    public void parseClassFromNameAbsolute() {
        Context context = mock(Context.class);
        Class clazz = NavDestination.parseClassFromName(context,
                "java.lang.String", Object.class);
        assertThat(clazz, not(is(nullValue())));
        assertThat(clazz.getName(), is(String.class.getName()));
    }

    @Test
    public void parseClassFromNameAbsoluteInvalid() {
        Context context = mock(Context.class);
        try {
            NavDestination.parseClassFromName(context,
                    "definitely.not.found", Object.class);
            fail("Invalid type should cause an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void parseClassFromNameAbsoluteWithType() {
        Context context = mock(Context.class);
        Class clazz = NavDestination.parseClassFromName(context,
                "java.lang.String", String.class);
        assertThat(clazz, not(is(nullValue())));
        assertThat(clazz.getName(), is(String.class.getName()));
    }

    @Test
    public void parseClassFromNameAbsoluteWithIncorrectType() {
        Context context = mock(Context.class);
        try {
            NavDestination.parseClassFromName(context,
                    "java.lang.String", List.class);
            fail("Incorrect type should cause an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void parseClassFromNameRelative() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("java.lang");
        Class clazz = NavDestination.parseClassFromName(context,
                ".String", Object.class);
        assertThat(clazz, not(is(nullValue())));
        assertThat(clazz.getName(), is(String.class.getName()));
    }

    @Test
    public void parseClassFromNameRelativeInvalid() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("java.lang");
        try {
            NavDestination.parseClassFromName(context,
                    ".definitely.not.found", Object.class);
            fail("Invalid type should cause an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void parseClassFromNameRelativeWithType() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("java.lang");
        Class clazz = NavDestination.parseClassFromName(context,
                ".String", String.class);
        assertThat(clazz, not(is(nullValue())));
        assertThat(clazz.getName(), is(String.class.getName()));
    }

    @Test
    public void parseClassFromNameRelativeWithIncorrectType() {
        Context context = mock(Context.class);
        when(context.getPackageName()).thenReturn("java.lang");
        try {
            NavDestination.parseClassFromName(context,
                    ".String", List.class);
            fail("Incorrect type should cause an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void buildDeepLinkIds() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.setId(DESTINATION_ID);
        int parentId = 2;
        NavGraph parent = new NavGraph(mock(Navigator.class));
        parent.setId(parentId);
        destination.setParent(parent);
        int[] deepLinkIds = destination.buildDeepLinkIds();
        assertThat(deepLinkIds.length, is(2));
        assertThat(deepLinkIds[0], is(parentId));
        assertThat(deepLinkIds[1], is(DESTINATION_ID));
    }

    @Test
    public void putActionByDestinationId() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.putAction(ACTION_ID, DESTINATION_ID);

        assertThat(destination.getAction(ACTION_ID), not(nullValue()));
        assertThat(destination.getAction(ACTION_ID).getDestinationId(), is(DESTINATION_ID));
    }

    @Test(expected = IllegalArgumentException.class)
    public void putActionWithInvalidDestinationId() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        destination.putAction(INVALID_ACTION_ID, DESTINATION_ID);
    }

    @Test
    public void putAction() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        NavAction action = new NavAction(DESTINATION_ID);
        destination.putAction(ACTION_ID, action);

        assertThat(destination.getAction(ACTION_ID), is(action));
    }

    @Test
    public void removeAction() {
        NavDestination destination = new NavDestination(mock(Navigator.class));
        NavAction action = new NavAction(DESTINATION_ID);
        destination.putAction(ACTION_ID, action);

        assertThat(destination.getAction(ACTION_ID), is(action));

        destination.removeAction(ACTION_ID);

        assertThat(destination.getAction(ACTION_ID), nullValue());
    }
}
