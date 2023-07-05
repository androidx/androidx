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

package androidx.credentials.playservices

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentActivity

/**
 * This is a test activity used by the Robolectric Activity Scenario tests. It acts as a calling
 * activity in our test cases. This activity uses fragments.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
class TestCredentialsFragmentActivity : FragmentActivity()
