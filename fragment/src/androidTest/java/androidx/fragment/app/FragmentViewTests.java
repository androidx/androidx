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
package androidx.fragment.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Instrumentation;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.test.FragmentTestActivity;
import androidx.fragment.test.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class FragmentViewTests {
    @Rule
    public ActivityTestRule<FragmentTestActivity> mActivityRule =
            new ActivityTestRule<FragmentTestActivity>(FragmentTestActivity.class);

    private Instrumentation mInstrumentation;

    @Before
    public void setupInstrumentation() {
        mInstrumentation = InstrumentationRegistry.getInstrumentation();
    }

    // Test that adding a fragment adds the Views in the proper order. Popping the back stack
    // should remove the correct Views.
    @Test
    public void addFragments() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1);

        // Add another on top
        final StrictViewFragment fragment2 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment2).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1, fragment2);

        // Now add two in one transaction:
        final StrictViewFragment fragment3 = new StrictViewFragment();
        final StrictViewFragment fragment4 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment3)
                .add(R.id.fragmentContainer, fragment4)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1, fragment2, fragment3, fragment4);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1, fragment2);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(1, container.getChildCount());
        FragmentTestUtil.assertChildren(container, fragment1);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container);
    }

    // Add fragments to multiple containers in the same transaction. Make sure that
    // they pop correctly, too.
    @Test
    public void addTwoContainers() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.double_container);
        ViewGroup container1 = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer1);
        ViewGroup container2 = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer2);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer1, fragment1).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container1, fragment1);

        final StrictViewFragment fragment2 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer2, fragment2).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container2, fragment2);

        final StrictViewFragment fragment3 = new StrictViewFragment();
        final StrictViewFragment fragment4 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer1, fragment3)
                .add(R.id.fragmentContainer2, fragment4)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container1, fragment1, fragment3);
        FragmentTestUtil.assertChildren(container2, fragment2, fragment4);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container1, fragment1);
        FragmentTestUtil.assertChildren(container2, fragment2);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container1, fragment1);
        FragmentTestUtil.assertChildren(container2);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertEquals(0, container1.getChildCount());
    }

    // When you add a fragment that's has already been added, it should throw.
    @Test
    public void doubleAdd() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    fm.beginTransaction()
                            .add(R.id.fragmentContainer, fragment1)
                            .addToBackStack(null)
                            .commit();
                    fm.executePendingTransactions();
                    fail("Adding a fragment that is already added should be an error");
                } catch (IllegalStateException e) {
                    // expected
                }
            }
        });
    }

    // Make sure that removed fragments remove the right Views. Popping the back stack should
    // add the Views back properly
    @Test
    public void removeFragments() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        final StrictViewFragment fragment2 = new StrictViewFragment();
        final StrictViewFragment fragment3 = new StrictViewFragment();
        final StrictViewFragment fragment4 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .add(R.id.fragmentContainer, fragment2, "2")
                .add(R.id.fragmentContainer, fragment3, "3")
                .add(R.id.fragmentContainer, fragment4, "4")
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1, fragment2, fragment3, fragment4);

        // Remove a view
        fm.beginTransaction().remove(fragment4).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        assertEquals(3, container.getChildCount());
        FragmentTestUtil.assertChildren(container, fragment1, fragment2, fragment3);

        // remove another one
        fm.beginTransaction().remove(fragment2).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1, fragment3);

        // Now remove the remaining:
        fm.beginTransaction()
                .remove(fragment3)
                .remove(fragment1)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        final Fragment replacement1 = fm.findFragmentByTag("1");
        final Fragment replacement3 = fm.findFragmentByTag("3");
        FragmentTestUtil.assertChildren(container, replacement1, replacement3);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        final Fragment replacement2 = fm.findFragmentByTag("2");
        FragmentTestUtil.assertChildren(container, replacement1, replacement3, replacement2);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        final Fragment replacement4 = fm.findFragmentByTag("4");
        FragmentTestUtil.assertChildren(container, replacement1, replacement3, replacement2,
                replacement4);
    }

    // Removing a hidden fragment should remove the View and popping should bring it back hidden
    @Test
    public void removeHiddenView() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "1").hide(fragment1).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1);
        assertTrue(fragment1.isHidden());

        fm.beginTransaction().remove(fragment1).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        final Fragment replacement1 = fm.findFragmentByTag("1");
        FragmentTestUtil.assertChildren(container, replacement1);
        assertTrue(replacement1.isHidden());
        assertEquals(View.GONE, replacement1.getView().getVisibility());
    }

    // Removing a detached fragment should do nothing to the View and popping should bring
    // the Fragment back detached
    @Test
    public void removeDetatchedView() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .detach(fragment1)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment1.isDetached());

        fm.beginTransaction().remove(fragment1).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        final Fragment replacement1 = fm.findFragmentByTag("1");
        FragmentTestUtil.assertChildren(container);
        assertTrue(replacement1.isDetached());
    }

    // Unlike adding the same fragment twice, you should be able to add and then remove and then
    // add the same fragment in one transaction.
    @Test
    public void addRemoveAdd() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .remove(fragment)
                .add(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container);
    }

    // Removing a fragment that isn't in should not throw
    @Test
    public void removeNothThere() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction().remove(fragment).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Hide a fragment and its View should be GONE. Then pop it and the View should be VISIBLE
    @Test
    public void hideFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());

        fm.beginTransaction().hide(fragment).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertTrue(fragment.isHidden());
        assertEquals(View.GONE, fragment.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertFalse(fragment.isHidden());
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());
    }

    // Hiding a hidden fragment should not throw
    @Test
    public void doubleHide() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .hide(fragment)
                .hide(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Hiding a non-existing fragment should not throw
    @Test
    public void hideUnAdded() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .hide(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Show a hidden fragment and its View should be VISIBLE. Then pop it and the View should be
    // GONE.
    @Test
    public void showFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertTrue(fragment.isHidden());
        assertEquals(View.GONE, fragment.getView().getVisibility());

        fm.beginTransaction().show(fragment).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertFalse(fragment.isHidden());
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertTrue(fragment.isHidden());
        assertEquals(View.GONE, fragment.getView().getVisibility());
    }

    // Showing a shown fragment should not throw
    @Test
    public void showShown() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .show(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Showing a non-existing fragment should not throw
    @Test
    public void showUnAdded() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .show(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Detaching a fragment should remove the View from the hierarchy. Then popping it should
    // bring it back VISIBLE
    @Test
    public void detachFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertFalse(fragment.isDetached());
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());

        fm.beginTransaction().detach(fragment).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment.isDetached());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertFalse(fragment.isDetached());
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());
    }

    // Detaching a hidden fragment should remove the View from the hierarchy. Then popping it should
    // bring it back hidden
    @Test
    public void detachHiddenFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).hide(fragment).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertFalse(fragment.isDetached());
        assertTrue(fragment.isHidden());
        assertEquals(View.GONE, fragment.getView().getVisibility());

        fm.beginTransaction().detach(fragment).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment.isHidden());
        assertTrue(fragment.isDetached());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertTrue(fragment.isHidden());
        assertFalse(fragment.isDetached());
        assertEquals(View.GONE, fragment.getView().getVisibility());
    }

    // Detaching a detached fragment should not throw
    @Test
    public void detachDetatched() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .detach(fragment)
                .detach(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Detaching a non-existing fragment should not throw
    @Test
    public void detachUnAdded() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .detach(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Attaching a fragment should add the View back into the hierarchy. Then popping it should
    // remove it again
    @Test
    public void attachFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment).detach(fragment).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment.isDetached());

        fm.beginTransaction().attach(fragment).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertFalse(fragment.isDetached());
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment.isDetached());
    }

    // Attaching a hidden fragment should add the View as GONE the hierarchy. Then popping it should
    // remove it again.
    @Test
    public void attachHiddenFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .hide(fragment)
                .detach(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment.isDetached());
        assertTrue(fragment.isHidden());

        fm.beginTransaction().attach(fragment).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertTrue(fragment.isHidden());
        assertFalse(fragment.isDetached());
        assertEquals(View.GONE, fragment.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
        assertTrue(fragment.isDetached());
        assertTrue(fragment.isHidden());
    }

    // Attaching an attached fragment should not throw
    @Test
    public void attachAttached() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment)
                .attach(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Attaching a non-existing fragment should not throw
    @Test
    public void attachUnAdded() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .attach(fragment)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
    }

    // Simple replace of one fragment in a container. Popping should replace it back again
    @Test
    public void replaceOne() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1, "1").commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment1);

        final StrictViewFragment fragment2 = new StrictViewFragment();
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment2);
        assertEquals(View.VISIBLE, fragment2.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        Fragment replacement1 = fm.findFragmentByTag("1");
        assertNotNull(replacement1);
        FragmentTestUtil.assertChildren(container, replacement1);
        assertFalse(replacement1.isHidden());
        assertTrue(replacement1.isAdded());
        assertFalse(replacement1.isDetached());
        assertEquals(View.VISIBLE, replacement1.getView().getVisibility());
    }

    // Replace of multiple fragments in a container. Popping should replace it back again
    @Test
    public void replaceTwo() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        final StrictViewFragment fragment2 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .add(R.id.fragmentContainer, fragment2, "2")
                .hide(fragment2)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment1, fragment2);

        final StrictViewFragment fragment3 = new StrictViewFragment();
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment3);
        assertEquals(View.VISIBLE, fragment3.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        Fragment replacement1 = fm.findFragmentByTag("1");
        Fragment replacement2 = fm.findFragmentByTag("2");
        assertNotNull(replacement1);
        assertNotNull(replacement2);
        FragmentTestUtil.assertChildren(container, replacement1, replacement2);
        assertFalse(replacement1.isHidden());
        assertTrue(replacement1.isAdded());
        assertFalse(replacement1.isDetached());
        assertEquals(View.VISIBLE, replacement1.getView().getVisibility());

        // fragment2 was hidden, so it should be returned hidden
        assertTrue(replacement2.isHidden());
        assertTrue(replacement2.isAdded());
        assertFalse(replacement2.isDetached());
        assertEquals(View.GONE, replacement2.getView().getVisibility());
    }

    // Replace of empty container. Should act as add and popping should just remove the fragment
    @Test
    public void replaceZero() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final StrictViewFragment fragment = new StrictViewFragment();
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment);
        assertEquals(View.VISIBLE, fragment.getView().getVisibility());

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container);
    }

    // Replace a fragment that exists with itself
    @Test
    public void replaceExisting() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictViewFragment fragment1 = new StrictViewFragment();
        final StrictViewFragment fragment2 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1, "1")
                .add(R.id.fragmentContainer, fragment2, "2")
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment1, fragment2);

        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment1);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        final Fragment replacement1 = fm.findFragmentByTag("1");
        final Fragment replacement2 = fm.findFragmentByTag("2");

        assertSame(fragment1, replacement1);
        FragmentTestUtil.assertChildren(container, replacement1, replacement2);
    }

    // Have two replace operations in the same transaction to ensure that they
    // don't interfere with each other
    @Test
    public void replaceReplace() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.double_container);
        ViewGroup container1 = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer1);
        ViewGroup container2 = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer2);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final StrictViewFragment fragment1 = new StrictViewFragment();
        final StrictViewFragment fragment2 = new StrictViewFragment();
        final StrictViewFragment fragment3 = new StrictViewFragment();
        final StrictViewFragment fragment4 = new StrictViewFragment();
        final StrictViewFragment fragment5 = new StrictViewFragment();
        fm.beginTransaction()
                .add(R.id.fragmentContainer1, fragment1)
                .add(R.id.fragmentContainer2, fragment2)
                .replace(R.id.fragmentContainer1, fragment3)
                .replace(R.id.fragmentContainer2, fragment4)
                .replace(R.id.fragmentContainer1, fragment5)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        assertChildren(container1, fragment5);
        assertChildren(container2, fragment4);

        fm.popBackStack();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        assertChildren(container1);
        assertChildren(container2);
    }

    // Test to prevent regressions in FragmentManager fragment replace method. See b/24693644
    @Test
    public void testReplaceFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        StrictViewFragment fragmentA = new StrictViewFragment();
        fragmentA.setLayoutId(R.layout.text_a);

        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragmentA)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);

        assertNotNull(findViewById(R.id.textA));
        assertNull(findViewById(R.id.textB));
        assertNull(findViewById(R.id.textC));

        StrictViewFragment fragmentB = new StrictViewFragment();
        fragmentB.setLayoutId(R.layout.text_b);
        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragmentB)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertNotNull(findViewById(R.id.textA));
        assertNotNull(findViewById(R.id.textB));
        assertNull(findViewById(R.id.textC));

        StrictViewFragment fragmentC = new StrictViewFragment();
        fragmentC.setLayoutId(R.layout.text_c);
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragmentC)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        assertNull(findViewById(R.id.textA));
        assertNull(findViewById(R.id.textB));
        assertNotNull(findViewById(R.id.textC));
    }

    // Test that adding a fragment with invisible or gone views does not end up with the view
    // being visible
    @Test
    public void addInvisibleAndGoneFragments() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final StrictViewFragment fragment1 = new InvisibleFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1);

        assertEquals(View.INVISIBLE, fragment1.getView().getVisibility());

        final InvisibleFragment fragment2 = new InvisibleFragment();
        fragment2.visibility = View.GONE;
        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack(null)
                .commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment2);

        assertEquals(View.GONE, fragment2.getView().getVisibility());
    }

    // Test to ensure that popping and adding a fragment properly track the fragments added
    // and removed.
    @Test
    public void popAdd() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        // One fragment with a view
        final StrictViewFragment fragment1 = new StrictViewFragment();
        fm.beginTransaction().add(R.id.fragmentContainer, fragment1).addToBackStack(null).commit();
        FragmentTestUtil.executePendingTransactions(mActivityRule);
        FragmentTestUtil.assertChildren(container, fragment1);

        final StrictViewFragment fragment2 = new StrictViewFragment();
        final StrictViewFragment fragment3 = new StrictViewFragment();
        mInstrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                fm.popBackStack();
                fm.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment2)
                        .addToBackStack(null)
                        .commit();
                fm.executePendingTransactions();
                fm.popBackStack();
                fm.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment3)
                        .addToBackStack(null)
                        .commit();
                fm.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(container, fragment3);
    }

    // Ensure that ordered transactions are executed individually rather than together.
    // This forces references from one fragment to another that should be executed earlier
    // to work.
    @Test
    public void orderedOperationsTogether() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final StrictViewFragment fragment1 = new StrictViewFragment();
        fragment1.setLayoutId(R.layout.scene1);
        final StrictViewFragment fragment2 = new StrictViewFragment();
        fragment2.setLayoutId(R.layout.fragment_a);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fm.beginTransaction()
                        .add(R.id.fragmentContainer, fragment1)
                        .setReorderingAllowed(false)
                        .addToBackStack(null)
                        .commit();
                fm.beginTransaction()
                        .add(R.id.squareContainer, fragment2)
                        .setReorderingAllowed(false)
                        .addToBackStack(null)
                        .commit();
                fm.executePendingTransactions();
            }
        });
        FragmentTestUtil.assertChildren(container, fragment1);
        assertNotNull(findViewById(R.id.textA));
    }

    // Ensure that there is no problem if the child fragment manager is used before
    // the View has been added.
    @Test
    public void childFragmentManager() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final StrictViewFragment fragment1 = new ParentFragment();
        fragment1.setLayoutId(R.layout.double_container);

        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .addToBackStack(null)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule);

        FragmentTestUtil.assertChildren(container, fragment1);
        ViewGroup innerContainer = (ViewGroup)
                fragment1.getView().findViewById(R.id.fragmentContainer1);

        Fragment fragment2 = fragment1.getChildFragmentManager().findFragmentByTag("inner");
        FragmentTestUtil.assertChildren(innerContainer, fragment2);
    }

    // Popping the backstack with ordered fragments should execute the operations together.
    // When a non-backstack fragment will be raised, it should not be destroyed.
    @Test
    public void popToNonBackStackFragment() throws Throwable {
        FragmentTestUtil.setContentView(mActivityRule, R.layout.simple_container);
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();

        final SimpleViewFragment fragment1 = new SimpleViewFragment();

        fm.beginTransaction()
                .add(R.id.fragmentContainer, fragment1)
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule);

        final SimpleViewFragment fragment2 = new SimpleViewFragment();

        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment2)
                .addToBackStack("two")
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule);

        final SimpleViewFragment fragment3 = new SimpleViewFragment();

        fm.beginTransaction()
                .replace(R.id.fragmentContainer, fragment3)
                .addToBackStack("three")
                .commit();

        FragmentTestUtil.executePendingTransactions(mActivityRule);

        assertEquals(1, fragment1.onCreateViewCount);
        assertEquals(1, fragment2.onCreateViewCount);
        assertEquals(1, fragment3.onCreateViewCount);

        FragmentTestUtil.popBackStackImmediate(mActivityRule, "two",
                FragmentManager.POP_BACK_STACK_INCLUSIVE);

        ViewGroup container = (ViewGroup)
                mActivityRule.getActivity().findViewById(R.id.fragmentContainer);

        FragmentTestUtil.assertChildren(container, fragment1);

        assertEquals(2, fragment1.onCreateViewCount);
        assertEquals(1, fragment2.onCreateViewCount);
        assertEquals(1, fragment3.onCreateViewCount);
    }

    private View findViewById(int viewId) {
        return mActivityRule.getActivity().findViewById(viewId);
    }

    private void assertChildren(ViewGroup container, Fragment... fragments) {
        final int numFragments = fragments == null ? 0 : fragments.length;
        assertEquals("There aren't the correct number of fragment Views in its container",
                numFragments, container.getChildCount());
        for (int i = 0; i < numFragments; i++) {
            assertEquals("Wrong Fragment View order for [" + i + "]", container.getChildAt(i),
                    fragments[i].getView());
        }
    }

    public static class InvisibleFragment extends StrictViewFragment {
        public int visibility = View.INVISIBLE;

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            view.setVisibility(visibility);
            super.onViewCreated(view, savedInstanceState);
        }
    }

    public static class ParentFragment extends StrictViewFragment {
        public ParentFragment() {
            setLayoutId(R.layout.double_container);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = super.onCreateView(inflater, container, savedInstanceState);
            final StrictViewFragment fragment2 = new StrictViewFragment();
            fragment2.setLayoutId(R.layout.fragment_a);

            getChildFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer1, fragment2, "inner")
                    .addToBackStack(null)
                    .commit();
            getChildFragmentManager().executePendingTransactions();
            return view;
        }
    }

    public static class SimpleViewFragment extends Fragment {
        public int onCreateViewCount;

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                @Nullable Bundle savedInstanceState) {
            onCreateViewCount++;
            return inflater.inflate(R.layout.fragment_a, container, false);
        }
    }
}
