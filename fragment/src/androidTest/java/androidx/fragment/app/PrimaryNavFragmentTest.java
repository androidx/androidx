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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import androidx.fragment.app.test.EmptyFragmentTestActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@MediumTest
public class PrimaryNavFragmentTest {
    @Rule
    public ActivityTestRule<EmptyFragmentTestActivity> mActivityRule =
            new ActivityTestRule<EmptyFragmentTestActivity>(EmptyFragmentTestActivity.class);

    @Test
    public void delegateBackToPrimaryNav() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment strictFragment = new StrictFragment();

        fm.beginTransaction().add(strictFragment, null).setPrimaryNavigationFragment(strictFragment)
                .commit();
        executePendingTransactions(fm);

        assertSame("new fragment is not primary nav fragment", strictFragment,
                fm.getPrimaryNavigationFragment());

        final StrictFragment child = new StrictFragment();
        FragmentManager cfm = strictFragment.getChildFragmentManager();
        cfm.beginTransaction().add(child, null).addToBackStack(null).commit();
        executePendingTransactions(cfm);

        assertEquals("child transaction not on back stack", 1, cfm.getBackStackEntryCount());

        // Should execute the pop for the child fragmentmanager
        assertTrue("popBackStackImmediate returned no action performed",
                popBackStackImmediate(fm));

        assertEquals("child transaction still on back stack", 0, cfm.getBackStackEntryCount());
    }

    @Test
    public void popPrimaryNav() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment strictFragment1 = new StrictFragment();

        fm.beginTransaction().add(strictFragment1, null)
                .setPrimaryNavigationFragment(strictFragment1)
                .commit();
        executePendingTransactions(fm);

        assertSame("new fragment is not primary nav fragment", strictFragment1,
                fm.getPrimaryNavigationFragment());

        fm.beginTransaction().remove(strictFragment1).addToBackStack(null).commit();
        executePendingTransactions(fm);

        assertNull("primary nav fragment is not null after remove",
                fm.getPrimaryNavigationFragment());

        popBackStackImmediate(fm);

        assertSame("primary nav fragment was not restored on pop", strictFragment1,
                fm.getPrimaryNavigationFragment());

        final StrictFragment strictFragment2 = new StrictFragment();
        fm.beginTransaction().remove(strictFragment1).add(strictFragment2, null)
                .setPrimaryNavigationFragment(strictFragment2).addToBackStack(null).commit();
        executePendingTransactions(fm);

        assertSame("primary nav fragment not updated to new fragment", strictFragment2,
                fm.getPrimaryNavigationFragment());

        popBackStackImmediate(fm);

        assertSame("primary nav fragment not restored on pop", strictFragment1,
                fm.getPrimaryNavigationFragment());

        fm.beginTransaction().setPrimaryNavigationFragment(strictFragment1)
                .addToBackStack(null).commit();
        executePendingTransactions(fm);

        assertSame("primary nav fragment not retained when set again in new transaction",
                strictFragment1, fm.getPrimaryNavigationFragment());
        popBackStackImmediate(fm);

        assertSame("same primary nav fragment not retained when set primary nav transaction popped",
                strictFragment1, fm.getPrimaryNavigationFragment());
    }

    @Test
    public void replacePrimaryNav() throws Throwable {
        final FragmentManager fm = mActivityRule.getActivity().getSupportFragmentManager();
        final StrictFragment strictFragment1 = new StrictFragment();

        fm.beginTransaction().add(android.R.id.content, strictFragment1)
                .setPrimaryNavigationFragment(strictFragment1).commit();
        executePendingTransactions(fm);

        assertSame("new fragment is not primary nav fragment", strictFragment1,
                fm.getPrimaryNavigationFragment());

        final StrictFragment strictFragment2 = new StrictFragment();
        fm.beginTransaction().replace(android.R.id.content, strictFragment2)
                .addToBackStack(null).commit();

        executePendingTransactions(fm);

        assertNull("primary nav fragment not null after replace",
                fm.getPrimaryNavigationFragment());

        popBackStackImmediate(fm);

        assertSame("primary nav fragment not restored after popping replace", strictFragment1,
                fm.getPrimaryNavigationFragment());

        fm.beginTransaction().setPrimaryNavigationFragment(null).commit();
        executePendingTransactions(fm);

        assertNull("primary nav fragment not null after explicit set to null",
                fm.getPrimaryNavigationFragment());

        fm.beginTransaction().replace(android.R.id.content, strictFragment2)
                .setPrimaryNavigationFragment(strictFragment2).addToBackStack(null).commit();
        executePendingTransactions(fm);

        assertSame("primary nav fragment not set correctly after replace", strictFragment2,
                fm.getPrimaryNavigationFragment());

        popBackStackImmediate(fm);

        assertNull("primary nav fragment not null after popping replace",
                fm.getPrimaryNavigationFragment());
    }

    private void executePendingTransactions(final FragmentManager fm) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fm.executePendingTransactions();
            }
        });
    }

    private boolean popBackStackImmediate(final FragmentManager fm) throws Throwable {
        final boolean[] result = new boolean[1];
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                result[0] = fm.popBackStackImmediate();
            }
        });
        return result[0];
    }
}
