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
@file:Suppress("DEPRECATION")

package androidx.lifecycle

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Fragment
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.os.Bundle
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@RunWith(JUnit4::class)
class DispatcherActivityCallbackTest {

    @Test
    fun onCreateFrameworkActivity() {
        val callback = LifecycleDispatcher.DispatcherActivityCallback()
        val activity = mock(Activity::class.java)
        checkReportFragment(callback, activity)
    }

    @Suppress("deprecation")
    @SuppressLint("CommitTransaction")
    private fun checkReportFragment(
        callback: LifecycleDispatcher.DispatcherActivityCallback,
        activity: Activity
    ) {
        val fm = mock(FragmentManager::class.java)
        val transaction = mock(FragmentTransaction::class.java)
        `when`(activity.fragmentManager).thenReturn(fm)
        `when`(fm.beginTransaction()).thenReturn(transaction)
        `when`(transaction.add(any(Fragment::class.java), anyString()))
            .thenReturn(transaction)
        callback.onActivityCreated(activity, mock(Bundle::class.java))
        verify(activity).fragmentManager
        verify(fm).beginTransaction()
        verify(transaction).add(any(ReportFragment::class.java), anyString())
        verify(transaction).commit()
    }
}
