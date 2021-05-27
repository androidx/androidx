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

package androidx.navigation.dynamicfeatures

import androidx.navigation.Navigator
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
public class DynamicExtrasTest {

    public class TestNavigatorExtras : Navigator.Extras

    @Test
    public fun build_withMonitorAndExtras() {
        val monitor = DynamicInstallMonitor()
        val navExtras = TestNavigatorExtras()
        val extras = DynamicExtras(monitor, navExtras)
        assertThat(extras.destinationExtras).isNotNull()
        assertThat(extras.installMonitor).isNotNull()
    }

    @Test
    public fun build_withoutMonitorOrExtras() {
        val extras = DynamicExtras()
        assertThat(extras.destinationExtras).isNull()
        assertThat(extras.installMonitor).isNull()
    }
}
