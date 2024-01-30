/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.fragment.app

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(sdk = [Build.VERSION_CODES.O_MR1], manifest = Config.NONE)
class FragmentRoboAnimTest {

    @Test
    fun fragmentAnim() {
        val activityController = Robolectric.buildActivity(FragmentActivity::class.java).setup()
        val activity = activityController.get()

        val container = FrameLayout(activity).apply {
            id = 1
        }
        activity.setContentView(container)

        activity.supportFragmentManager
            .beginTransaction()
            .replace(container.id, ViewFragment())
            .commitNow()

        activity.supportFragmentManager
            .beginTransaction()
            .replace(container.id, ViewFragment())
            .commitNow()

        assertThat(container.childCount).isEqualTo(1)

        activityController.destroy()
    }
}

class ViewFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return View(activity)
    }

    override fun onCreateAnimation(transit: Int, enter: Boolean, nextAnim: Int): Animation? {
        if (!enter) {
            return AlphaAnimation(0.0f, 1.0f).apply { duration = 0 }
        }
        return null
    }
}
