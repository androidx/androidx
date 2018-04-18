/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package androidx.leanback.app.wizard;

import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.KeyEvent;

import androidx.leanback.app.GuidedStepFragment;
import androidx.leanback.test.R;
import androidx.leanback.widget.GuidanceStylist;
import androidx.leanback.widget.GuidedAction;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class GuidedStepAttributesTest {
    static final long TRANSITION_LENGTH = 1000;

    static final String TAG = "GuidedStepAttributesTest";

    @Rule
    public ActivityTestRule<GuidedStepAttributesTestActivity> activityTestRule =
            new ActivityTestRule<>(GuidedStepAttributesTestActivity.class, false, false);

    GuidedStepAttributesTestActivity mActivity;

    private void initActivity(Intent intent) {
        mActivity = activityTestRule.launchActivity(intent);
        try {
            Thread.sleep(2000);
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    Context mContext;
    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();;
    }

    public static void sendKey(int keyCode) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(keyCode);
    }

    @Test
    public void testFocusDisabledOnActions() throws Throwable {

        Intent intent = new Intent();
        Resources res = mContext.getResources();

        final int NUM_SEARCH_ACTIONS = 10;
        final List<Integer> ACTIONS_WITH_DISABLED_FOCUS = new ArrayList<>(
                Arrays.asList(1, 3, 4, 5, 8));
        final int ACTION_ID_SEARCH = 1;
        List<Integer> EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT = new ArrayList<>();

        // we will traverse actions from top to bottom and then back to the top
        for(int i = 0; i < NUM_SEARCH_ACTIONS; i++) {
            if (!ACTIONS_WITH_DISABLED_FOCUS.contains(i))
                EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT.add(i);
        }
        for(int i = EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT.size(); i-- != 0;) {
            EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT.add(EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT.get(i));
        }


        String title = "Guided Actions Focusable Test";
        String breadcrumb = "Focusable Test Demo";
        String description = "";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();
        for (int i = 0; i < NUM_SEARCH_ACTIONS; i++ ) {
            actionList.add(new GuidedAction.Builder(mContext)
                    .id(ACTION_ID_SEARCH)
                    .title(res.getString(R.string.search) + "" + i)
                    .description(res.getString(R.string.search_description) + i)
                    .build()
            );
        }
        for(int action_id : ACTIONS_WITH_DISABLED_FOCUS )
            actionList.get(action_id).setFocusable(false);

        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);

        int lastSelectedActionId = -1;
        int selectIndex = 0;
        GuidedStepFragment mFragment = (GuidedStepFragment) mActivity.getGuidedStepTestFragment();
        int prevSelectedActionPosition = -1;
        int nextSelectedActionPosition = mFragment.getSelectedActionPosition();
        while ( nextSelectedActionPosition != prevSelectedActionPosition ) {
            lastSelectedActionId = mFragment.getSelectedActionPosition();
            assertTrue(res.getString(R.string.focusable_test_error_message,
                    actionList.get(lastSelectedActionId).getTitle()),
                    lastSelectedActionId == EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT.get(selectIndex));
            selectIndex++;
            sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
            prevSelectedActionPosition = nextSelectedActionPosition;
            nextSelectedActionPosition = mFragment.getSelectedActionPosition();
            Thread.sleep(TRANSITION_LENGTH);
        }

        prevSelectedActionPosition = -1;
        while ( nextSelectedActionPosition != prevSelectedActionPosition ) {
            lastSelectedActionId = mFragment.getSelectedActionPosition();
            assertTrue(res.getString(R.string.focusable_test_error_message,
                    actionList.get(lastSelectedActionId).getTitle()),
                    lastSelectedActionId == EXPECTED_ACTIONS_ID_AFTER_EACH_SELECT.get(selectIndex));
            selectIndex++;
            sendKey(KeyEvent.KEYCODE_DPAD_UP);
            prevSelectedActionPosition = nextSelectedActionPosition;
            nextSelectedActionPosition = mFragment.getSelectedActionPosition();
            Thread.sleep(TRANSITION_LENGTH);
        }

    }

    // Note: do not remove final or sRevertCallback gets null from 2nd test on!
     static final GuidedStepAttributesTestFragment.Callback sRevertCallback = new
            GuidedStepAttributesTestFragment.Callback() {
        @Override
        public void onActionClicked(GuidedStepFragment fragment, long id) {
            List<GuidedAction> allActions = fragment.getActions();
            for(int i = 1; i < allActions.size(); i++) {
                GuidedAction action = allActions.get(i);
                action.setEnabled(!action.isEnabled());
                fragment.notifyActionChanged(fragment.findActionPositionById(action.getId()));
            }
        }
    };

    /**
     * Creates a number of enabled and disable actions and tests whether the flag is correctly set
     * by clicking on each individual action and checking whether the click event is triggered.
     * @throws Throwable
     */
    @Test
    public void testDisabledActions() throws Throwable {

        Intent intent = new Intent();
        Resources res = mContext.getResources();

        final int NUM_SEARCH_ACTIONS = 10;
        final List<Integer> DISABLED_ACTIONS = new ArrayList<>(
                Arrays.asList(1, 3, 5, 7));
        final int ACTION_ID_REVERT_BUTTON = 0;
        final int ACTION_ID_SEARCH_BEGIN = ACTION_ID_REVERT_BUTTON + 1;
        int ACTION_ID_SEARCH_END = ACTION_ID_SEARCH_BEGIN;

        // sequence of clicked actions simulated in the test
        List<Integer> CLICK_SEQUENCE = new ArrayList<>();

        // Expected Clicked sequence can be different from focused ones since some of the actions
        // are disabled hence not clickable
        List<Integer> EXPECTED_FOCUSED_SEQUENCE = new ArrayList<>();
        List<Integer> EXPECTED_CLICKED_SEQUENCE = new ArrayList<>();
        // Expected actions state according to list of DISABLED_ACTIONS: false for disabled actions
        List<Boolean> EXPECTED_ACTIONS_STATE = new ArrayList<>(
                Arrays.asList(new Boolean[NUM_SEARCH_ACTIONS])
        );
        Collections.fill(EXPECTED_ACTIONS_STATE, Boolean.TRUE);

        for(int i = 0; i < NUM_SEARCH_ACTIONS; i++) {
            CLICK_SEQUENCE.add(i + 1);
        }
        for(int clickedActionId : CLICK_SEQUENCE) {
            EXPECTED_FOCUSED_SEQUENCE.add(clickedActionId);
            if (!DISABLED_ACTIONS.contains(clickedActionId - 1))
                EXPECTED_CLICKED_SEQUENCE.add(clickedActionId);
            else
                EXPECTED_CLICKED_SEQUENCE.add(-1);
        }

        String title = "Guided Actions Enabled Test";
        String breadcrumb = "Enabled Test Demo";
        String description = "";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();
        actionList.add(new GuidedAction.Builder(mContext)
                .id(ACTION_ID_REVERT_BUTTON)
                .title(res.getString(R.string.invert_title))
                .description(res.getString(R.string.revert_description))
                .build()
        );

        for (int i = 0; i < NUM_SEARCH_ACTIONS; i++ ) {
            actionList.add(new GuidedAction.Builder(mContext)
                    .id(ACTION_ID_SEARCH_END++)
                    .title(res.getString(R.string.search) + "" + i)
                    .description(res.getString(R.string.search_description) + i)
                    .build()
            );
        }
        for(int action_id : DISABLED_ACTIONS ) {
            if ( action_id >= 0 && action_id < NUM_SEARCH_ACTIONS ) {
                actionList.get(action_id + 1).setEnabled(false);
                EXPECTED_ACTIONS_STATE.set(action_id, Boolean.FALSE);
            }
        }

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;
        GuidedStepAttributesTestFragment.setActionClickCallback(ACTION_ID_REVERT_BUTTON,
                sRevertCallback);

        initActivity(intent);

        examineEnabledAndDisabledActions(actionList, CLICK_SEQUENCE, EXPECTED_FOCUSED_SEQUENCE,
                EXPECTED_CLICKED_SEQUENCE);
    }

    /**
     * Toggles Enabled flags in oll the actions of the prior test, and tests whether they are
     * correctly reverted.
     */
    @Test
    public void testToggleEnabledFlags() throws Throwable {

        Intent intent = new Intent();
        Resources res = mContext.getResources();

        final int NUM_SEARCH_ACTIONS = 10;
        final List<Integer> DISABLED_ACTIONS = new ArrayList<>(
                Arrays.asList(1, 3, 5, 7));
        final int ACTION_ID_REVERT_BUTTON = 0;
        final int ACTION_ID_SEARCH_BEGIN = ACTION_ID_REVERT_BUTTON + 1;
        int ACTION_ID_SEARCH_END = ACTION_ID_SEARCH_BEGIN;

        // sequence of clicked actions simulated in the test
        List<Integer> CLICK_SEQUENCE = new ArrayList<>();

        // Expected Clicked sequence can be different from focused ones since some of the actions
        // are disabled hence not clickable
        List<Integer> EXPECTED_FOCUSED_SEQUENCE = new ArrayList<>();
        List<Integer> EXPECTED_CLICKED_SEQUENCE = new ArrayList<>();
        // Expected actions state according to list of DISABLED_ACTIONS: false for disabled actions
        List<Boolean> EXPECTED_ACTIONS_STATE = new ArrayList<>(
                Arrays.asList(new Boolean[NUM_SEARCH_ACTIONS])
        );
        Collections.fill(EXPECTED_ACTIONS_STATE, Boolean.FALSE);

        for(int i = 0; i < NUM_SEARCH_ACTIONS; i++) {
            CLICK_SEQUENCE.add(i + 1);
        }
        for(int clickedActionId : CLICK_SEQUENCE) {
            EXPECTED_FOCUSED_SEQUENCE.add(clickedActionId);
            if (DISABLED_ACTIONS.contains(clickedActionId - 1))
                EXPECTED_CLICKED_SEQUENCE.add(clickedActionId);
            else
                EXPECTED_CLICKED_SEQUENCE.add(-1);
        }

        String title = "Guided Actions Enabled Test";
        String breadcrumb = "Toggle Enabled Flag Test Demo";
        String description = "";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();
        actionList.add(new GuidedAction.Builder(mContext)
                .id(ACTION_ID_REVERT_BUTTON)
                .title(res.getString(R.string.invert_title))
                .description(res.getString(R.string.revert_description))
                .build()
        );

        for (int i = 0; i < NUM_SEARCH_ACTIONS; i++ ) {
            actionList.add(new GuidedAction.Builder(mContext)
                    .id(ACTION_ID_SEARCH_END++)
                    .title(res.getString(R.string.search) + "" + i)
                    .description(res.getString(R.string.search_description) + i)
                    .build()
            );
        }
        for(int action_id : DISABLED_ACTIONS ) {
            if ( action_id >= 0 && action_id < NUM_SEARCH_ACTIONS ) {
                actionList.get(action_id + 1).setEnabled(false);
                EXPECTED_ACTIONS_STATE.set(action_id, Boolean.TRUE);
            }
        }

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;
        GuidedStepAttributesTestFragment.setActionClickCallback(ACTION_ID_REVERT_BUTTON,
                sRevertCallback);

        initActivity(intent);

        final GuidedStepFragment mFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();

        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFragment.setSelectedActionPosition(0);
            }
        });
        Thread.sleep(TRANSITION_LENGTH);
        sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
        Thread.sleep(TRANSITION_LENGTH);
        examineEnabledAndDisabledActions(actionList, CLICK_SEQUENCE, EXPECTED_FOCUSED_SEQUENCE,
                EXPECTED_CLICKED_SEQUENCE);
    }

    private void examineEnabledAndDisabledActions(
            List<GuidedAction> actionList, List<Integer> CLICK_SEQUENCE,
                                List<Integer> EXPECTED_FOCUSED_SEQUENCE,
                                List<Integer> EXPECTED_CLICKED_SEQUENCE)
            throws Throwable {

        final GuidedStepFragment mFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();

        for(int i = 0; i < CLICK_SEQUENCE.size(); i++) {
            GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID =
                    GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID = -1;
            final int id = CLICK_SEQUENCE.get(i);
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFragment.setSelectedActionPosition(id);
                }
            });
            Thread.sleep(TRANSITION_LENGTH);

            sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            Thread.sleep(TRANSITION_LENGTH);

            assertTrue(mContext.getResources().getString(
                    R.string.enabled_test_wrong_focus_error_message),
                    GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID
                            == EXPECTED_FOCUSED_SEQUENCE.get(i)
            );
            assertTrue(mContext.getResources().getString(
                    R.string.enabled_test_wrong_click_error_message),
                    GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID
                            == EXPECTED_CLICKED_SEQUENCE.get(i)
            );
            assertTrue(mContext.getResources().getString(
                    R.string.enabled_test_wrong_flag_error_message),
                    (GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID == -1)
                            ? !actionList.get(id).isEnabled()
                            : actionList.get(id).isEnabled()
            );
        }
    }

    @Test
    public void testCheckedActions() throws Throwable {

        Intent intent = new Intent();
        Resources res = mContext.getResources();

        final int NUM_RADIO_ACTIONS = 3;
        final int NUM_CHECK_BOX_ACTIONS = 3;
        final int INITIALLY_CHECKED_RADIO_ACTION = 0;
        final List<Integer> INITIALLY_CHECKED_CHECKBOX_ACTIONS = new ArrayList<>(
                Arrays.asList(1, 2)
        );

        List<Integer> CLICK_SEQUENCE = new ArrayList<>();
        for(int i = 0; i < NUM_RADIO_ACTIONS + NUM_CHECK_BOX_ACTIONS; i++) {
            CLICK_SEQUENCE.add(i);
        }

        List<Boolean> EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK = new ArrayList<>(
                Arrays.asList(new Boolean[CLICK_SEQUENCE.size()])
        );
        Collections.fill(EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK, Boolean.FALSE);

        // initial state of actions before any clicks happen
        EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.set(INITIALLY_CHECKED_RADIO_ACTION, true);
        for(int checkedCheckBox : INITIALLY_CHECKED_CHECKBOX_ACTIONS) {
            EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.set(NUM_RADIO_ACTIONS + checkedCheckBox, true);
        }

        String title = "Guided Actions Checked Test";
        String breadcrumb = "Checked Test Demo";
        String description = "";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();
        actionList.add(new GuidedAction.Builder(mContext)
                .title(res.getString(R.string.radio_actions_info_title))
                .description(res.getString(R.string.radio_actions_info_desc))
                .infoOnly(true)
                .enabled(true)
                .focusable(false)
                .build()
        );

        int firstRadioActionIndex = actionList.size();
        for(int i = 0; i < NUM_RADIO_ACTIONS; i++) {
            actionList.add(new GuidedAction.Builder(mContext)
                    .title(res.getString(R.string.checkbox_title) + i)
                    .description(res.getString(R.string.checkbox_desc) + i)
                    .checkSetId(GuidedAction.DEFAULT_CHECK_SET_ID)
                    .build()
            );
            if (i == INITIALLY_CHECKED_RADIO_ACTION)
                actionList.get(firstRadioActionIndex + i).setChecked(true);
        }

        actionList.add(new GuidedAction.Builder(mContext)
                .title(res.getString(R.string.checkbox_actions_info_title))
                .description(res.getString(R.string.checkbox_actions_info_desc))
                .infoOnly(true)
                .enabled(true)
                .focusable(false)
                .build()
        );
        int firstCheckBoxActionIndex = actionList.size();
        for(int i = 0; i < NUM_CHECK_BOX_ACTIONS; i++) {
            actionList.add(new GuidedAction.Builder(mContext)
                    .title(res.getString(R.string.checkbox_title) + i)
                    .description(res.getString(R.string.checkbox_desc) + i)
                    .checkSetId(GuidedAction.CHECKBOX_CHECK_SET_ID)
                    .build()
            );
        }
        for(int i = 0; i < INITIALLY_CHECKED_CHECKBOX_ACTIONS.size(); i++ ) {
            actionList.get(firstCheckBoxActionIndex + INITIALLY_CHECKED_CHECKBOX_ACTIONS.get(i))
                    .setChecked(true);
        }

        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;
        initActivity(intent);

        examineCheckedAndUncheckedActions(actionList, EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK,
                NUM_RADIO_ACTIONS, NUM_CHECK_BOX_ACTIONS);
    }

    private void updateExpectedActionsStateAfterClick(
            List<Boolean> EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK, int NUM_RADIO_ACTIONS,
            int NUM_CHECK_BOX_ACTIONS, int clickedActionIndex) {

        if (clickedActionIndex < NUM_RADIO_ACTIONS) {
            for(int i = 0; i < NUM_RADIO_ACTIONS; i++)
                EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.set(i, false);
            EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.set(clickedActionIndex, true);
        }
        else if (clickedActionIndex < NUM_RADIO_ACTIONS + NUM_CHECK_BOX_ACTIONS) {
            EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.set(clickedActionIndex,
                    !EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.get(clickedActionIndex));
        }
    }

    private void verifyIfActionsStateIsCorrect(List<GuidedAction> actionList,
            List<Boolean> EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK) {

        int actionIndex = 0;
        for(GuidedAction checkAction : actionList) {
            if (checkAction.infoOnly())
                continue;
            assertTrue("Action " + actionIndex + " is " + (!checkAction.isChecked() ? "un-" : "")
                    + "checked while it shouldn't be!", checkAction.isChecked()
                            == EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK.get(actionIndex));
            actionIndex++;
        }
    }

    private void examineCheckedAndUncheckedActions(List<GuidedAction> actionList,
                                List<Boolean> EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK,
                                int NUM_RADIO_ACTIONS,
                                int NUM_CHECK_BOX_ACTIONS) throws Throwable {

        final GuidedStepFragment guidedStepCheckedFragment = (GuidedStepFragment)
                mActivity.getGuidedStepTestFragment();
        final int firstRadioActionIndex = 1;
        final int firstCheckBoxActionIndex = firstRadioActionIndex + NUM_RADIO_ACTIONS + 1;
        for(int actionId = 0; actionId < NUM_RADIO_ACTIONS; actionId++) {
            final int id = actionId;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    guidedStepCheckedFragment
                            .setSelectedActionPosition(firstRadioActionIndex + id);
                }
            });
            Thread.sleep(TRANSITION_LENGTH);

            sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            Thread.sleep(TRANSITION_LENGTH);
            updateExpectedActionsStateAfterClick(EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK,
                    NUM_RADIO_ACTIONS, NUM_CHECK_BOX_ACTIONS, actionId);
            verifyIfActionsStateIsCorrect(actionList, EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK);
        }

        for(int actionId = 0; actionId < NUM_CHECK_BOX_ACTIONS; actionId++) {
            final int id = actionId;
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    guidedStepCheckedFragment
                            .setSelectedActionPosition(firstCheckBoxActionIndex + id);
                }
            });
            Thread.sleep(TRANSITION_LENGTH);

            sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            Thread.sleep(TRANSITION_LENGTH);
            updateExpectedActionsStateAfterClick(EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK,
                    NUM_RADIO_ACTIONS, NUM_CHECK_BOX_ACTIONS, NUM_RADIO_ACTIONS + actionId);
            verifyIfActionsStateIsCorrect(actionList, EXPECTED_ACTIONS_STATE_AFTER_EACH_CLICK);
        }
    }

    @Test
    public void testActionWithTwoSubActions() throws Throwable {
        ExpectedSubActionResult result = setUpActionsForSubActionsTest();

        final int actionPos = 0;
        final GuidedAction selectedAction = result.actionList.get(actionPos);
        List<Integer> expectedFocusedSeq = result.expectedFocusedSeq.get(actionPos);
        List<Integer> expectedClickedSeq = result.expectedClickedSeq.get(actionPos);

        traverseSubActionsAndVerifyFocusAndClickEvents(selectedAction, actionPos, expectedFocusedSeq,
                expectedClickedSeq);
    }

    @Test
    public void testActionWithOneSubAction() throws Throwable {
        ExpectedSubActionResult result = setUpActionsForSubActionsTest();

        final int actionPos = 1;
        final GuidedAction selectedAction = result.actionList.get(actionPos);
        List<Integer> expectedFocusedSeq = result.expectedFocusedSeq.get(actionPos);
        List<Integer> expectedClickedSeq = result.expectedClickedSeq.get(actionPos);

        traverseSubActionsAndVerifyFocusAndClickEvents(selectedAction, actionPos, expectedFocusedSeq,
                expectedClickedSeq);
    }

    @Test
    public void testActionWithZeroSubActions() throws Throwable {
        ExpectedSubActionResult result = setUpActionsForSubActionsTest();

        final int actionPos = 2;
        final GuidedAction selectedAction = result.actionList.get(actionPos);
        List<Integer> expectedFocusedSeq = result.expectedFocusedSeq.get(actionPos);
        List<Integer> expectedClickedSeq = result.expectedClickedSeq.get(actionPos);

        traverseSubActionsAndVerifyFocusAndClickEvents(selectedAction, actionPos, expectedFocusedSeq,
                expectedClickedSeq);
    }

    @Test
    public void testActionWithThreeSubActions() throws Throwable {
        ExpectedSubActionResult result = setUpActionsForSubActionsTest();

        final int actionPos = 3;
        final GuidedAction selectedAction = result.actionList.get(actionPos);
        List<Integer> expectedFocusedSeq = result.expectedFocusedSeq.get(actionPos);
        List<Integer> expectedClickedSeq = result.expectedClickedSeq.get(actionPos);

        traverseSubActionsAndVerifyFocusAndClickEvents(selectedAction, actionPos, expectedFocusedSeq,
                expectedClickedSeq);
    }

    /**
     * Traverses the list of sub actions of a gudied action. It also verifies the correct action
     * or sub action is focused or clicked as the traversal is performed.
     * @param selectedAction The action of interest
     * @param actionPos The position of selectedAction within the array of guidedactions
     * @param expectedFocusedSeq The actual actions IDs used as a reference to verify focused actions
     * @param expectedClickedSeq The actual action IDs used as a reference to verify clicked actions
     * @throws Throwable
     */
    private void traverseSubActionsAndVerifyFocusAndClickEvents(GuidedAction selectedAction,
                                                                int actionPos,
                                                                List<Integer> expectedFocusedSeq,
                                                                List<Integer> expectedClickedSeq)
            throws Throwable{

        final GuidedStepFragment mFragment =
                (GuidedStepFragment) mActivity.getGuidedStepTestFragment();
        int focusStep = 0, clickStep = 0;
        GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID =
                GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID = -1;


        final int pos = actionPos;
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mFragment.setSelectedActionPosition(pos);
            }
        });
        Thread.sleep(TRANSITION_LENGTH);

        if (mFragment.getSelectedActionPosition() != actionPos) {
            assertTrue(mContext.getResources().getString(
                    R.string.subaction_test_wrong_focus_error_message),
                    GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID
                            == expectedFocusedSeq.get(focusStep++)
            );
        } else {
            // If the currently focused position is the same as the position of the action of interest,
            // then GuidedStepFragment won't received onGuidedActionFocused callback. Since the first
            // element in the expectedFocusSeq is always the id of this action, we need to move focusStep
            // one step forward.
            focusStep++;
        }
        if (selectedAction.hasSubActions()) {
            // Following for loop clicks on a specific action and scrolls & clicks through
            // all its subactions
            for (int j = 0; j < selectedAction.getSubActions().size(); j++) {
                sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
                Thread.sleep(TRANSITION_LENGTH);
                assertTrue(mContext.getResources().getString(
                        R.string.subaction_test_wrong_focus_error_message),
                        GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID
                                == expectedFocusedSeq.get(focusStep++)
                );
                assertTrue(mContext.getResources().getString(
                        R.string.subaction_test_wrong_click_error_message),
                        GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID
                                == expectedClickedSeq.get(clickStep++)
                );

                for (int k = 0; k < j; k++) {
                    sendKey(KeyEvent.KEYCODE_DPAD_DOWN);
                    Thread.sleep(TRANSITION_LENGTH);
                    assertTrue(mContext.getResources().getString(
                            R.string.subaction_test_wrong_focus_error_message),
                            GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID
                                    == expectedFocusedSeq.get(focusStep++)
                    );
                }
                sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
                Thread.sleep(TRANSITION_LENGTH);

                assertTrue(mContext.getResources().getString(
                        R.string.subaction_test_wrong_focus_error_message),
                        GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID
                                == expectedFocusedSeq.get(focusStep++)
                );
                assertTrue(mContext.getResources().getString(
                        R.string.subaction_test_wrong_click_error_message),
                        GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID
                                == expectedClickedSeq.get(clickStep++)
                );
            }
        } else {
            sendKey(KeyEvent.KEYCODE_DPAD_CENTER);
            Thread.sleep(TRANSITION_LENGTH);
            assertTrue(mContext.getResources().getString(
                    R.string.subaction_test_wrong_focus_error_message),
                    GuidedStepAttributesTestFragment.LAST_SELECTED_ACTION_ID
                            == expectedFocusedSeq.get(focusStep++)
            );
            assertTrue(mContext.getResources().getString(
                    R.string.subaction_test_wrong_click_error_message),
                    GuidedStepAttributesTestFragment.LAST_CLICKED_ACTION_ID
                            == expectedClickedSeq.get(clickStep++)
            );
        }
    }

    static class ExpectedSubActionResult {
        List<List<Integer>> expectedFocusedSeq; // Expected sequence of action (or subaction) ids to receive focus events;
        // Each entry corresponds to an action item in the guidedactions pane
        List<List<Integer>> expectedClickedSeq; // Expected sequence of action (or subaction) ids to receive click events;
        // Each entry corresponds to an action item in the guidedactions pane
        List<GuidedAction> actionList;          // List of GuidedActions in the guidedactions pane
    }

    /**
     * Populates a sample list of actions and subactions in the guidedactions pane.
     * @return  An object holding the expected sequence of action and subactions IDs that receive
     * focus and click events as well as the list of GuidedActions.
     */
    private ExpectedSubActionResult setUpActionsForSubActionsTest() {
        Intent intent = new Intent();
        Resources res = mContext.getResources();

        ExpectedSubActionResult result = new ExpectedSubActionResult();
        result.expectedFocusedSeq = new ArrayList<>();
        result.expectedClickedSeq = new ArrayList<>();

        final int NUM_REGULAR_ACTIONS = 4;
        final int[] NUM_SUBACTIONS_PER_ACTION = {2, 1, 0, 3};
        final int[] REGULAR_ACTIONS_INDEX =  new int[NUM_REGULAR_ACTIONS];
        final int[] BEGIN_SUBACTION_INDEX_PER_ACTION = new int[NUM_REGULAR_ACTIONS];
        final int[] END_SUBACTION_INDEX_PER_ACTION = new int[NUM_REGULAR_ACTIONS];
        // Actions and SubActions are assigned unique sequential IDs
        int lastIndex = 0;
        for(int i = 0; i < NUM_REGULAR_ACTIONS; i++) {
            REGULAR_ACTIONS_INDEX[i] = lastIndex;
            lastIndex++;
            BEGIN_SUBACTION_INDEX_PER_ACTION[i] = lastIndex;
            END_SUBACTION_INDEX_PER_ACTION[i] = (lastIndex += NUM_SUBACTIONS_PER_ACTION[i]);
        }

        for (int i = 0; i < NUM_REGULAR_ACTIONS; i++) {
            List<Integer> expectedFocusSeqForEachAction = new ArrayList<>();
            List<Integer> expectedClickedSeqForEachAction = new ArrayList<>();
            expectedFocusSeqForEachAction.add(REGULAR_ACTIONS_INDEX[i]);

            if (NUM_SUBACTIONS_PER_ACTION[i] > 0) {
                for (int j = BEGIN_SUBACTION_INDEX_PER_ACTION[i];
                        j < END_SUBACTION_INDEX_PER_ACTION[i]; j++) {
                    expectedClickedSeqForEachAction.add(REGULAR_ACTIONS_INDEX[i]);
                    for (int k = BEGIN_SUBACTION_INDEX_PER_ACTION[i]; k <= j; k++) {
                        expectedFocusSeqForEachAction.add(k);
                    }
                    expectedClickedSeqForEachAction.add(j);
                    expectedFocusSeqForEachAction.add(REGULAR_ACTIONS_INDEX[i]);
                }
            } else {
                expectedClickedSeqForEachAction.add(REGULAR_ACTIONS_INDEX[i]);
                expectedFocusSeqForEachAction.add(REGULAR_ACTIONS_INDEX[i]);
            }
            result.expectedFocusedSeq.add(expectedFocusSeqForEachAction);
            result.expectedClickedSeq.add(expectedClickedSeqForEachAction);
        }

        String title = "Guided SubActions Test";
        String breadcrumb = "SubActions Test Demo";
        String description = "";
        GuidanceStylist.Guidance guidance = new GuidanceStylist.Guidance(title, description,
                breadcrumb, null);

        List<GuidedAction> actionList = new ArrayList<>();

        lastIndex = 0;
        for (int i = 0; i < NUM_REGULAR_ACTIONS; i++ ) {
            GuidedAction action = new GuidedAction.Builder(mContext)
                    .id(lastIndex++)
                    .title(res.getString(R.string.dropdown_action_title, i))
                    .description(res.getString(R.string.dropdown_action_desc, i))
                    .build();
            if (NUM_SUBACTIONS_PER_ACTION[i] > 0) {
                List<GuidedAction> subActions = new ArrayList<>();
                action.setSubActions(subActions);
                for(int j = 0; j < NUM_SUBACTIONS_PER_ACTION[i]; j++) {
                    subActions.add(new GuidedAction.Builder(mContext)
                            .id(lastIndex++)
                            .title(res.getString(R.string.subaction_title, j))
                            .description("")
                            .build()
                    );
                }
            }
            actionList.add(action);
        }
        result.actionList = actionList;

        GuidedStepAttributesTestFragment.clear();
        GuidedStepAttributesTestFragment.GUIDANCE = guidance;
        GuidedStepAttributesTestFragment.ACTION_LIST = actionList;

        initActivity(intent);
        return result;
    }
}
