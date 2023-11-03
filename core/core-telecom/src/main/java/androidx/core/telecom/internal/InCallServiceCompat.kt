/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.core.telecom.internal

import android.content.Context
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.telecom.util.ExperimentalAppActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

/**
 * This class defines the Jetpack ICS layer which will be leveraged as part of supporting VOIP app
 * actions.
 */
@ExperimentalAppActions
@RequiresApi(Build.VERSION_CODES.O)
internal open class InCallServiceCompat() : InCallService() {
    internal lateinit var mContext: Context
    private lateinit var mScope: CoroutineScope
    val mCallCompats = mutableListOf<CallCompat>()

    companion object {
        private val TAG = InCallServiceCompat::class.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        mScope = CoroutineScope(Dispatchers.IO)
    }

    override fun onDestroy() {
        super.onDestroy()
        mScope.cancel()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCallAdded(@NonNull call: Call) {
        super.onCallAdded(call)
        val callCompat = CallCompat(call, mContext, mScope, this)
        callCompat.processCallAdded()
    }
}
