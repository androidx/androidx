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

package com.example.androidx.viewpager2.test

import android.view.View
import androidx.test.espresso.matcher.BoundedMatcher
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.Matcher

fun withRotation(): Matcher<View> {
    return WithRotationMatcher(not(0f))
}

fun withTranslation(): Matcher<View> {
    return WithTranslationMatcher(not(0f))
}

fun withScale(): Matcher<View> {
    return WithScaleMatcher(not(0f))
}

private class WithRotationMatcher(private val rotation: Matcher<Float>) :
    BoundedMatcher<View, View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("with rotation: ")
        rotation.describeTo(description)
    }

    override fun matchesSafely(item: View): Boolean {
        return rotation.matches(item.rotation)
    }
}

private class WithTranslationMatcher(private val translation: Matcher<Float>) :
    BoundedMatcher<View, View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("with translation: ")
        translation.describeTo(description)
    }

    override fun matchesSafely(item: View): Boolean {
        return translation.matches(item.translationX) ||
                translation.matches(item.translationY) ||
                translation.matches(item.translationZ)
    }
}

private class WithScaleMatcher(private val scale: Matcher<Float>) :
    BoundedMatcher<View, View>(View::class.java) {
    override fun describeTo(description: Description) {
        description.appendText("with scale: ")
        scale.describeTo(description)
    }

    override fun matchesSafely(item: View): Boolean {
        return scale.matches(item.scaleX) ||
                scale.matches(item.scaleY)
    }
}
