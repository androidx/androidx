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

package android.support.v17.leanback.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.support.v17.leanback.test.R;
import android.text.InputType;
import android.view.ContextThemeWrapper;
import android.widget.FrameLayout;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class GuidedActionStylistTest {
    private static final int DEFAULT_MAX_LINES = 0;

    // Simulate Android Context
    private Context mContext;

    // The GuidedActionStylist for testing purpose
    private GuidedActionsStylist mGuidedActionsStylist;

    // Mocked view holder, required by the parameter of onBindViewHolder method
    private GuidedActionsStylist.ViewHolder mViewHolder;

    // Mocked action object, required by the parameter of onBindViewHolder method
    private GuidedAction mGuidedAction;

    @Before
    public void setUp() throws Exception {

        // Get context from instrumentation registry firstly
        mContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // Then apply the theme on the context
        mContext = new ContextThemeWrapper(mContext,
                R.style.Widget_Leanback_GuidedSubActionsListStyle);

        // Prepare GuidedActionStylist object
        mGuidedActionsStylist = new GuidedActionsStylist();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                // Create view holder object programmatically
                mViewHolder = mGuidedActionsStylist.onCreateViewHolder(new FrameLayout(mContext));
            }
        });
    }

    /**
     * Test cases when multilineDescription is set to true
     */
    @Test
    public void testWhenMultiLineDescriptionSetToTrue() {

        // Create a action and set multilineDescription to true
        mGuidedAction = new GuidedAction.Builder(mContext).multilineDescription(true).build();

        // Execute onBindViewHolder method so we can monitor the internal state
        mGuidedActionsStylist.onBindViewHolder(mViewHolder, mGuidedAction);

        // double check if multilineDescription option has been enabled
        assertTrue(mGuidedAction.hasMultilineDescription());

        // currently the title view and description view using the same flag. So when we execute
        // multilineDescription(true) method, the multi line mode should be enabled both for title
        // view and description view.
        // Test cases should be updated when we change this behavior
        assertTrue((mViewHolder.mTitleView.getInputType() & InputType.TYPE_TEXT_FLAG_MULTI_LINE)
                == InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        assertTrue((mViewHolder.mDescriptionView.getInputType()
                & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    }

    /**
     * Test cases when multilineDescription is set to false
     */
    @Test
    public void testWhenMultiLineDescriptionSetToFalse() {

        // Create a action and set multilineDescription to false
        mGuidedAction = new GuidedAction.Builder(mContext).multilineDescription(false).build();

        // Execute onBindViewHolder method so we can monitor the internal state
        mGuidedActionsStylist.onBindViewHolder(mViewHolder, mGuidedAction);

        // double check if multilineDescription option has been disabled
        assertFalse(mGuidedAction.hasMultilineDescription());

        // currently the title view and description view using the same flag. So when we execute
        // multilineDescription(true) method, the multi line mode should be disabled both for title
        // view and description view.
        // Test cases should be updated when we change this behavior
        assertEquals(mViewHolder.mTitleView.getMaxLines(), DEFAULT_MAX_LINES);
        assertEquals(mViewHolder.mDescriptionView.getMaxLines(), DEFAULT_MAX_LINES);
    }
}
