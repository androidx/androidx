/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.appcompat.graphics.drawable

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class DrawableContainerCompatTest {
    /**
     * Regression test for b/171913944 where DrawableContainer fails to copy when there are no
     * children in the constant state.
     */
    @Test
    fun testDrawableContainerCopyNoChildren() {
        val orig = MyDrawableContainerState()

        // This shouldn't crash.
        MyDrawableContainerState(orig)
    }

    @Test
    fun testSetTint() {
        val container = MyDrawableContainer()
        container.setTint(Color.RED)

        assertTrue(container.calledSetTintList)
    }

    internal class MyDrawableContainerState : DrawableContainerCompat.DrawableContainerState {
        constructor() : super(null, null, null)
        constructor(orig: DrawableContainerCompat.DrawableContainerState?) : super(orig, null, null)

        init {
            addChild(ColorDrawable(Color.WHITE))
        }

        override fun newDrawable(): Drawable {
            return DrawableContainerCompat()
        }
    }

    internal class MyDrawableContainer : DrawableContainerCompat() {
        var calledSetTintList = false

        init {
            setConstantState(MyDrawableContainerState())
            currentIndex = 0
        }

        override fun setTintList(tint: ColorStateList?) {
            super.setTintList(tint)

            calledSetTintList = true
        }
    }
}
