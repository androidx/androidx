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

package androidx.activity

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.activity.test.R
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.lifecycle.ViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ContentViewTest {

    @Test
    fun testLifecycleObserver() {
        with(ActivityScenario.launch(ContentViewActivity::class.java)) {
            val inflatedTextView: TextView = withActivity { findViewById(R.id.inflated_text_view) }
            assertThat(inflatedTextView)
                .isNotNull()
        }
    }

    @Test
    fun testViewTreeInflation() {
        with(ActivityScenario.launch(ContentViewActivity::class.java)) {
            val inflatedTextView: TextView = withActivity { findViewById(R.id.inflated_text_view) }

            withActivity {
                assertWithMessage("inflated view has correct ViewTreeLifecycleOwner")
                    .that(ViewTreeLifecycleOwner.get(inflatedTextView))
                    .isSameInstanceAs(this@withActivity)
                assertWithMessage("inflated view has correct ViewTreeViewModelStoreOwner")
                    .that(ViewTreeViewModelStoreOwner.get(inflatedTextView))
                    .isSameInstanceAs(this@withActivity)
                assertWithMessage("inflated view has correct ViewTreeSavedStateRegistryOwner")
                    .that(inflatedTextView.findViewTreeSavedStateRegistryOwner())
                    .isSameInstanceAs(this@withActivity)
            }
        }
    }

    @Test
    fun testViewTreeAttachment() {
        runAttachTest("setContentView view only") { setContentView(it) }
        runAttachTest("setContentView with LayoutParams") {
            setContentView(it, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
        runAttachTest("addContentView") {
            addContentView(it, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
        }
    }

    private fun runAttachTest(
        message: String,
        attach: ComponentActivity.(View) -> Unit
    ) {
        with(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            withActivity {
                val view = View(this)

                var attachedLifecycleOwner: Any? = "did not attach"
                var attachedViewModelStoreOwner: Any? = "did not attach"
                var attachedSavedStateRegistryOwner: Any? = "did not attach"
                view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewDetachedFromWindow(v: View?) {
                        // Do nothing
                    }

                    override fun onViewAttachedToWindow(v: View?) {
                        attachedLifecycleOwner = ViewTreeLifecycleOwner.get(view)
                        attachedViewModelStoreOwner = ViewTreeViewModelStoreOwner.get(view)
                        attachedSavedStateRegistryOwner = view.findViewTreeSavedStateRegistryOwner()
                    }
                })
                attach(view)
                assertWithMessage("$message: ViewTreeLifecycleOwner was set correctly")
                    .that(attachedLifecycleOwner).isSameInstanceAs(this)
                assertWithMessage("$message: ViewTreeViewModelStoreOwner was set correctly")
                    .that(attachedViewModelStoreOwner).isSameInstanceAs(this)
                assertWithMessage("$message: ViewTreeSavedStateRegistryOwner was set correctly")
                    .that(attachedSavedStateRegistryOwner).isSameInstanceAs(this)
            }
        }
    }
}

class ContentViewActivity : ComponentActivity(R.layout.activity_inflates_res)
class EmptyContentActivity : ComponentActivity()
