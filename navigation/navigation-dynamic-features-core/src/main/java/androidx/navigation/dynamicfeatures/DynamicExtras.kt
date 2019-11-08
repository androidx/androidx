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

/**
 * Extras for dynamic navigators.
 *
 * You can set a [DynamicInstallMonitor] here to be notified of install state changes.
 */
class DynamicExtras internal constructor(
    /**
     * @return The [DynamicInstallMonitor] used.
     */
    val installMonitor: DynamicInstallMonitor?,
    /**
     * @return [Navigator.Extras] associated with these [DynamicExtras].
     */
    val destinationExtras: Navigator.Extras?
) : Navigator.Extras {

    /**
     * Builder that enables creation of [DynamicExtras].
     */
    class Builder {

        private var monitor: DynamicInstallMonitor? = null
        private var destinationExtras: Navigator.Extras? = null

        /**
         * Set the [DynamicInstallMonitor].
         *
         * @param monitor The [DynamicInstallMonitor] to set.
         * @return This [Builder].
         */
        fun setInstallMonitor(monitor: DynamicInstallMonitor): Builder {
            this.monitor = monitor
            return this
        }

        /**
         * Set the [Navigator.Extras].
         *
         * @param destinationExtras The [Navigator.Extras] to set.
         * @return This [Builder].
         */
        fun setDestinationExtras(destinationExtras: Navigator.Extras): Builder {
            this.destinationExtras = destinationExtras
            return this
        }

        /**
         * Build [DynamicExtras].
         *
         * @return A new instance of [DynamicExtras] with all attributes set in the builder.
         */
        fun build(): DynamicExtras {
            return DynamicExtras(monitor, destinationExtras)
        }
    }
}
