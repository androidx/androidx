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

package androidx.preference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;

/**
 * Test for InitialExpandedChildrenCount in {@link androidx.preference.PreferenceGroup}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class PreferenceGroupInitialExpandedChildrenCountTest {

    private static final int INITIAL_EXPANDED_COUNT = 5;
    private static final int TOTAL_PREFERENCE = 10;
    private static final String PREFERENCE_TITLE_PREFIX = "Preference_";
    private static final String PREFERENCE_KEY = "testing";

    private Context mContext;
    private PreferenceManager mPreferenceManager;
    private PreferenceScreen mScreen;
    private Handler mHandler;
    private List<Preference> mPreferenceList;

    @Before
    @UiThreadTest
    public void setup() {
        MockitoAnnotations.initMocks(this);
        mContext = InstrumentationRegistry.getTargetContext();
        mPreferenceManager = new PreferenceManager(mContext);
        mScreen = mPreferenceManager.createPreferenceScreen(mContext);
        mScreen.setKey(PREFERENCE_KEY);

        // Add 10 preferences to the screen and to the cache
        mPreferenceList = new ArrayList<>();
        createTestPreferences(mScreen, mPreferenceList, TOTAL_PREFERENCE);

        // Execute the handler task immediately
        mHandler = spy(new Handler());
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Message message = (Message) args[0];
                mHandler.dispatchMessage(message);
                return null;
            }
        }).when(mHandler).sendMessageDelayed(any(Message.class), anyLong());
    }

    /**
     * Verifies that PreferenceGroupAdapter is showing the preferences on the screen correctly with
     * and without the collapsed child count set.
     */
    @Test
    @UiThreadTest
    public void createPreferenceGroupAdapter_displayTopLevelPreferences() {
        // No limit, should display all 10 preferences
        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);
        assertPreferencesAreExpanded(preferenceGroupAdapter);

        // Limit > child count, should display all 10 preferences
        mScreen.setInitialExpandedChildrenCount(TOTAL_PREFERENCE + 4);
        preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);
        assertPreferencesAreExpanded(preferenceGroupAdapter);

        // Limit = child count, should display all 10 preferences
        mScreen.setInitialExpandedChildrenCount(TOTAL_PREFERENCE);
        preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);
        assertPreferencesAreExpanded(preferenceGroupAdapter);

        // Limit < child count, should display up to the limit + expand button
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);
        assertPreferencesAreCollapsed(preferenceGroupAdapter);
        for (int i = 0; i < INITIAL_EXPANDED_COUNT; i++) {
            assertEquals(mPreferenceList.get(i), preferenceGroupAdapter.getItem(i));
        }
        assertEquals(CollapsiblePreferenceGroupController.ExpandButton.class,
                preferenceGroupAdapter.getItem(INITIAL_EXPANDED_COUNT).getClass());
    }

    /**
     * Verifies that PreferenceGroupAdapter is showing nested preferences on the screen correctly
     * with and without the collapsed child count set.
     */
    @Test
    @UiThreadTest
    public void createPreferenceGroupAdapter_displayNestedPreferences() {
        final PreferenceScreen screen = mPreferenceManager.createPreferenceScreen(mContext);
        screen.setKey(PREFERENCE_KEY);
        final List<Preference> preferenceList = new ArrayList<>();

        // Add 2 preferences and 2 categories to screen
        createTestPreferences(screen, preferenceList, 2);
        createTestPreferencesCategory(screen, preferenceList, 4);
        createTestPreferencesCategory(screen, preferenceList, 4);

        // No limit, should display all 10 preferences + 2 categories
        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(screen);
        assertEquals(TOTAL_PREFERENCE + 2, preferenceGroupAdapter.getItemCount());

        // Limit > child count, should display all 10 preferences + 2 categories
        screen.setInitialExpandedChildrenCount(TOTAL_PREFERENCE + 4);
        preferenceGroupAdapter = new PreferenceGroupAdapter(screen);
        assertEquals(TOTAL_PREFERENCE + 2, preferenceGroupAdapter.getItemCount());

        // Limit = child count, should display all 10 preferences + 2 categories
        screen.setInitialExpandedChildrenCount(TOTAL_PREFERENCE);
        preferenceGroupAdapter = new PreferenceGroupAdapter(screen);
        assertEquals(TOTAL_PREFERENCE + 2, preferenceGroupAdapter.getItemCount());

        // Limit < child count, should display 2 preferences and the first 3 preference in the
        // category + expand button
        screen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        preferenceGroupAdapter = new PreferenceGroupAdapter(screen);
        assertEquals(INITIAL_EXPANDED_COUNT + 2, preferenceGroupAdapter.getItemCount());
        for (int i = 0; i <= INITIAL_EXPANDED_COUNT; i++) {
            assertEquals(preferenceList.get(i), preferenceGroupAdapter.getItem(i));
        }
        assertEquals(CollapsiblePreferenceGroupController.ExpandButton.class,
                preferenceGroupAdapter.getItem(INITIAL_EXPANDED_COUNT + 1).getClass());
    }

    /**
     * Verifies that correct summary is set for the expand button.
     */
    @Test
    @UiThreadTest
    public void createPreferenceGroupAdapter_setExpandButtonSummary() {
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);
        // Preference 5 to Preference 9 are collapsed
        CharSequence summary = mPreferenceList.get(INITIAL_EXPANDED_COUNT).getTitle();
        for (int i = INITIAL_EXPANDED_COUNT + 1; i < TOTAL_PREFERENCE; i++) {
            summary = mContext.getString(R.string.summary_collapsed_preference_list,
                    summary, mPreferenceList.get(i).getTitle());
        }
        final Preference expandButton = preferenceGroupAdapter.getItem(INITIAL_EXPANDED_COUNT);
        assertEquals(summary, expandButton.getSummary());
    }

    /**
     * Verifies that summary for the expand button only lists visible preferences.
     */
    @Test
    @UiThreadTest
    public void createPreferenceGroupAdapter_expandButtonSummaryShouldListVisiblePreferencesOnly() {
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        mPreferenceList.get(INITIAL_EXPANDED_COUNT + 1).setVisible(false);
        mPreferenceList.get(INITIAL_EXPANDED_COUNT + 4).setVisible(false);
        PreferenceGroupAdapter preferenceGroupAdapter = new PreferenceGroupAdapter(mScreen);
        // Preference 5 to Preference 9 are collapsed, only preferences 5, 7, 8 are visible
        CharSequence summary = mPreferenceList.get(INITIAL_EXPANDED_COUNT).getTitle();
        summary = mContext.getString(R.string.summary_collapsed_preference_list,
                summary, mPreferenceList.get(INITIAL_EXPANDED_COUNT + 2).getTitle());
        summary = mContext.getString(R.string.summary_collapsed_preference_list,
                summary, mPreferenceList.get(INITIAL_EXPANDED_COUNT + 3).getTitle());
        final Preference expandButton = preferenceGroupAdapter.getItem(INITIAL_EXPANDED_COUNT);
        assertEquals(summary, expandButton.getSummary());
    }

    /**
     * Verifies that clicking the expand button will show all preferences.
     */
    @Test
    @UiThreadTest
    public void clickExpandButton_shouldShowAllPreferences() {
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);

        // First showing 5 preference with expand button
        PreferenceGroupAdapter preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        assertPreferencesAreCollapsed(preferenceGroupAdapter);

        // Click the expand button, should review all preferences
        final Preference expandButton = preferenceGroupAdapter.getItem(INITIAL_EXPANDED_COUNT);
        expandButton.performClick();
        assertPreferencesAreExpanded(preferenceGroupAdapter);
    }

    /**
     * Verifies that when preference visibility changes, it will sync the preferences only if some
     * preferences are collapsed.
     */
    @Test
    @UiThreadTest
    public void onPreferenceVisibilityChange_shouldSyncPreferencesIfCollapsed() {
        // No limit set, should not sync preference
        PreferenceGroupAdapter preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        preferenceGroupAdapter.onPreferenceVisibilityChange(mPreferenceList.get(3));
        verify(mHandler, never()).sendMessageDelayed(any(Message.class), anyLong());

        // Has limit set, should sync preference
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        preferenceGroupAdapter.onPreferenceVisibilityChange(mPreferenceList.get(3));
        verify(mHandler).sendMessageDelayed(any(Message.class), anyLong());

        // Preferences expanded already, should not sync preference
        final Preference expandButton = preferenceGroupAdapter.getItem(INITIAL_EXPANDED_COUNT);
        expandButton.performClick();
        reset(mHandler);
        preferenceGroupAdapter.onPreferenceVisibilityChange(mPreferenceList.get(3));
        verify(mHandler, never()).sendMessageDelayed(any(Message.class), anyLong());
    }

    /**
     * Verifies that the correct maximum number of preferences to show is being saved in the
     * instance state.
     */
    @Test
    @UiThreadTest
    public void saveInstanceState_shouldSaveMaxNumberOfChildrenToShow() {
        // No limit set, should save max value
        Parcelable state = mScreen.onSaveInstanceState();
        assertEquals(PreferenceGroup.SavedState.class, state.getClass());
        assertEquals(Integer.MAX_VALUE,
                ((PreferenceGroup.SavedState) state).mInitialExpandedChildrenCount);

        // Has limit set, should save limit
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        state = mScreen.onSaveInstanceState();
        assertEquals(PreferenceGroup.SavedState.class, state.getClass());
        assertEquals(INITIAL_EXPANDED_COUNT,
                ((PreferenceGroup.SavedState) state).mInitialExpandedChildrenCount);
    }

    /**
     * Verifies that if we restore to the same number of preferences to show, it will not update
     * anything.
     */
    @Test
    @UiThreadTest
    public void restoreInstanceState_noChange_shouldDoNothing() {
        PreferenceGroup.SavedState state;

        // Initialized as expanded, restore as expanded, should remain expanded
        state = new PreferenceGroup.SavedState(
                Preference.BaseSavedState.EMPTY_STATE, Integer.MAX_VALUE);
        PreferenceGroupAdapter preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        mScreen.onRestoreInstanceState(state);
        assertPreferencesAreExpanded(preferenceGroupAdapter);
        verify(mHandler, never()).sendMessageDelayed(any(Message.class), anyLong());

        // Initialized as collapsed, restore as collapsed, should remain collapsed
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        state = new PreferenceGroup.SavedState(
                Preference.BaseSavedState.EMPTY_STATE, INITIAL_EXPANDED_COUNT);
        preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        mScreen.onRestoreInstanceState(state);
        assertPreferencesAreCollapsed(preferenceGroupAdapter);
        verify(mHandler, never()).sendMessageDelayed(any(Message.class), anyLong());
    }

    /**
     * Verifies that if the children is collapsed previously, they should be collapsed after the
     * state is being restored.
     */
    @Test
    @UiThreadTest
    public void restoreHierarchyState_previouslyCollapsed_shouldRestoreToCollapsedState() {
        PreferenceGroup.SavedState state =
                new PreferenceGroup.SavedState(
                        Preference.BaseSavedState.EMPTY_STATE, Integer.MAX_VALUE);
        // Initialized as expanded, restore as collapsed, should collapse
        state.mInitialExpandedChildrenCount = INITIAL_EXPANDED_COUNT;
        mScreen.setInitialExpandedChildrenCount(Integer.MAX_VALUE);
        PreferenceGroupAdapter preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        mScreen.onRestoreInstanceState(state);
        verify(mHandler).sendMessageDelayed(any(Message.class), anyLong());
        assertPreferencesAreCollapsed(preferenceGroupAdapter);
    }

    /**
     * Verifies that if the children is expanded previously, they should be expanded after the
     * state is being restored.
     */
    @Test
    @UiThreadTest
    public void restoreHierarchyState_previouslyExpanded_shouldRestoreToExpandedState() {
        PreferenceGroup.SavedState state =
                new PreferenceGroup.SavedState(
                        Preference.BaseSavedState.EMPTY_STATE, Integer.MAX_VALUE);
        // Initialized as collapsed, restore as expanded, should expand
        state.mInitialExpandedChildrenCount = Integer.MAX_VALUE;
        mScreen.setInitialExpandedChildrenCount(INITIAL_EXPANDED_COUNT);
        PreferenceGroupAdapter preferenceGroupAdapter =
                PreferenceGroupAdapter.createInstanceWithCustomHandler(mScreen, mHandler);
        mScreen.onRestoreInstanceState(state);
        verify(mHandler).sendMessageDelayed(any(Message.class), anyLong());
        assertPreferencesAreExpanded(preferenceGroupAdapter);
    }

    // assert that the preferences are all expanded
    private void assertPreferencesAreExpanded(PreferenceGroupAdapter adapter) {
        assertEquals(TOTAL_PREFERENCE, adapter.getItemCount());
    }

    // assert that the preferences exceeding the limit are collapsed
    private void assertPreferencesAreCollapsed(PreferenceGroupAdapter adapter) {
        // list shows preferences up to the limit and the expand button
        assertEquals(INITIAL_EXPANDED_COUNT + 1, adapter.getItemCount());
    }

    // create the number of preference in the corresponding preference group and add it to the cache
    private void createTestPreferences(PreferenceGroup preferenceGroup,
            List<Preference> preferenceList, int numPreference) {
        for (int i = 0; i < numPreference; i++) {
            final Preference preference = new Preference(mContext);
            preference.setTitle(PREFERENCE_TITLE_PREFIX + i);
            preferenceGroup.addPreference(preference);
            preferenceList.add(preference);
        }
    }

    // add a preference category and add the number of preference to it and the cache
    private void createTestPreferencesCategory(PreferenceGroup preferenceGroup,
            List<Preference> preferenceList, int numPreference) {
        PreferenceCategory category = new PreferenceCategory(mContext);
        preferenceGroup.addPreference(category);
        preferenceList.add(category);
        createTestPreferences(category, preferenceList, numPreference);
    }

}
