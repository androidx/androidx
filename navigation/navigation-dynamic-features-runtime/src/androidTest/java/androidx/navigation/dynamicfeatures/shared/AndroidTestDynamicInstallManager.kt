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

package androidx.navigation.dynamicfeatures.shared

import android.content.Context
import androidx.navigation.dynamicfeatures.DynamicInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManager
import org.mockito.Mockito.mock

/**
 * A dynamic install manager used for testing.
 */
public class AndroidTestDynamicInstallManager(
    context: Context,
    public val splitInstallManager: SplitInstallManager = mock(SplitInstallManager::class.java)
) : DynamicInstallManager(context, splitInstallManager)
