/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appcompat.widget

import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class AppCompatIconsTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(AppCompatIconsActivity::class.java)

    private lateinit var activityScenario: ActivityScenario<AppCompatIconsActivity>

    @Before
    fun setup() {
        activityScenario = ActivityScenario.launch(AppCompatIconsActivity::class.java)
    }

    @Test
    fun testActionModeIconsOnImageView() {
        // Tests that the app:scrCompat attribute set to ?attr/actionModeXYZDrawable gets
        // resolved to non-null drawables
        activityScenario.onActivity {
            val imageViewCut = it.findViewById<AppCompatImageView>(R.id.icon_cut)
            val imageViewCopy = it.findViewById<AppCompatImageView>(R.id.icon_copy)
            val imageViewPaste = it.findViewById<AppCompatImageView>(R.id.icon_paste)
            val imageViewSelectAll = it.findViewById<AppCompatImageView>(R.id.icon_selectall)
            val imageViewShare = it.findViewById<AppCompatImageView>(R.id.icon_share)

            assertNotNull(imageViewCut.drawable)
            assertNotNull(imageViewCopy.drawable)
            assertNotNull(imageViewPaste.drawable)
            assertNotNull(imageViewSelectAll.drawable)
            assertNotNull(imageViewShare.drawable)
        }
    }

    @Test
    fun testActionModeIconsFromAppCompatResources() {
        // Tests that we can resolve R.attr.actionModeXYZDrawable to a non-null drawable
        // from our activity's theme

        activityScenario.onActivity {
            // Resolve ?attr/actionModeCutDrawable
            val typedValueCut = TypedValue()
            it.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionModeCutDrawable, typedValueCut, true
            )
            val drawableCut = AppCompatResources.getDrawable(it, typedValueCut.resourceId)
            assertNotNull(drawableCut)

            // Resolve ?attr/actionModeCopyDrawable
            val typedValueCopy = TypedValue()
            it.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionModeCopyDrawable, typedValueCopy, true
            )
            val drawableCopy = AppCompatResources.getDrawable(it, typedValueCopy.resourceId)
            assertNotNull(drawableCopy)

            // Resolve ?attr/actionModePasteDrawable
            val typedValuePaste = TypedValue()
            it.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionModePasteDrawable, typedValuePaste, true
            )
            val drawablePaste = AppCompatResources.getDrawable(it, typedValuePaste.resourceId)
            assertNotNull(drawablePaste)

            // Resolve ?attr/actionModeSelectAllDrawable
            val typedValueSelectAll = TypedValue()
            it.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionModeSelectAllDrawable, typedValueSelectAll, true
            )
            val drawableSelectAll =
                AppCompatResources.getDrawable(it, typedValueSelectAll.resourceId)
            assertNotNull(drawableSelectAll)

            // Resolve ?attr/actionModeShareDrawable
            val typedValueShare = TypedValue()
            it.theme.resolveAttribute(
                androidx.appcompat.R.attr.actionModeShareDrawable, typedValueShare, true
            )
            val drawableShare = AppCompatResources.getDrawable(it, typedValueShare.resourceId)
            assertNotNull(drawableShare)
        }
    }
}
