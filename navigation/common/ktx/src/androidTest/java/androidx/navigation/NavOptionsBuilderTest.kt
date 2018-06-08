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

package androidx.navigation

import android.support.test.filters.SmallTest
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class NavOptionsTest {

    @Test
    fun launchSingleTop() {
        val navOptions = navOptions {
            launchSingleTop = true
        }
        assertTrue("NavOptions should have launchSingleTop set",
                navOptions.shouldLaunchSingleTop())
    }

    @Suppress("DEPRECATION")
    @Test
    fun launchDocument() {
        val navOptions = navOptions {
            launchDocument = true
        }
        assertTrue("NavOptions should have launchDocument set",
                navOptions.shouldLaunchDocument())
    }

    @Suppress("DEPRECATION")
    @Test
    fun clearTask() {
        val navOptions = navOptions {
            clearTask = true
        }
        assertTrue("NavOptions should have clearTask set",
                navOptions.shouldClearTask())
    }

    @Test
    fun popUpTo() {
        val navOptions = navOptions {
            popUpTo = DESTINATION_ID
        }
        assertEquals("NavOptions should have popUpTo destination id set",
                DESTINATION_ID, navOptions.popUpTo)
        assertFalse("NavOptions should have isPopUpToInclusive false by default",
                navOptions.isPopUpToInclusive)
    }

    @Test
    fun popUpToInclusive() {
        val navOptions = navOptions {
            popUpTo(DESTINATION_ID) {
                inclusive = true
            }
        }
        assertEquals("NavOptions should have popUpTo destination id set",
                DESTINATION_ID, navOptions.popUpTo)
        assertTrue("NavOptions should have isPopUpToInclusive set",
                navOptions.isPopUpToInclusive)
    }

    @Test
    fun anim() {
        val navOptions = navOptions {
            anim {
                enter = ENTER_ANIM_ID
                exit = EXIT_ANIM_ID
                popEnter = POP_ENTER_ANIM_ID
                popExit = POP_EXIT_ANIM_ID
            }
        }
        assertEquals("NavOptions should have enter animation set",
                ENTER_ANIM_ID, navOptions.enterAnim)
        assertEquals("NavOptions should have exit animation set",
                EXIT_ANIM_ID, navOptions.exitAnim)
        assertEquals("NavOptions should have pop enter animation set",
                POP_ENTER_ANIM_ID, navOptions.popEnterAnim)
        assertEquals("NavOptions should have pop exit animation set",
                POP_EXIT_ANIM_ID, navOptions.popExitAnim)
    }
}

private const val DESTINATION_ID = 1

private const val ENTER_ANIM_ID = 10
private const val EXIT_ANIM_ID = 11
private const val POP_ENTER_ANIM_ID = 12
private const val POP_EXIT_ANIM_ID = 13
