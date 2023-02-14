/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.bluetooth.core

// TODO(ofy) Implement Bundleable instead of Parcelable
/**
 * Bluetooth LE scan settings are passed to {@link BluetoothLeScanner#startScan} to define the
 * parameters for the scan.
 * @hide
 */
class ScanSettings {

    companion object {
        /**
         * A special Bluetooth LE scan mode. Applications using this scan mode will passively
         * listen for other scan results without starting BLE scans themselves.
         */
        const val SCAN_MODE_OPPORTUNISTIC = -1

        /**
         * Perform Bluetooth LE scan in low power mode. This is the default scan mode as it
         * consumes the least power. This mode is enforced if the scanning application is not in
         * foreground.
         */
        const val SCAN_MODE_LOW_POWER = 0

        /**
         * Perform Bluetooth LE scan in balanced power mode. Scan results are returned at a rate
         * that provides a good trade-off between scan frequency and power consumption.
         */
        const val SCAN_MODE_BALANCED = 1

        /**
         * Scan using highest duty cycle. It's recommended to only use this mode when the
         * application is running in the foreground.
         */
        const val SCAN_MODE_LOW_LATENCY = 2

        /**
         * Perform Bluetooth LE scan in ambient discovery mode. This mode has lower duty cycle
         * and more aggressive scan interval than balanced mode that provides a good trade-off
         * between scan latency and power consumption.
         *
         * @hide
         */
        val SCAN_MODE_AMBIENT_DISCOVERY = 3

        /**
         * Default Bluetooth LE scan mode when the screen is off.
         * This mode has the low duty cycle and long scan interval which results in the lowest
         * power consumption among all modes. It is for the framework internal use only.
         *
         * @hide
         */
        const val SCAN_MODE_SCREEN_OFF = 4

        /**
         * Balanced Bluetooth LE scan mode for foreground service when the screen is off.
         * It is for the framework internal use only.
         *
         * @hide
         */
        const val SCAN_MODE_SCREEN_OFF_BALANCED = 5

        /**
         * Trigger a callback for every Bluetooth advertisement found that matches the filter
         * criteria. If no filter is active, all advertisement packets are reported.
         */
        const val CALLBACK_TYPE_ALL_MATCHES = 1

        /**
         * A result callback is only triggered for the first advertisement packet received that
         * matches the filter criteria.
         */
        const val CALLBACK_TYPE_FIRST_MATCH = 2

        /**
         * Receive a callback when advertisements are no longer received from a device that has
         * been previously reported by a first match callback.
         */
        const val CALLBACK_TYPE_MATCH_LOST = 4
        /**
         * Determines how many advertisements to match per filter, as this is scarce hw resource
         */
        /**
         * Match one advertisement per filter
         */
        const val MATCH_NUM_ONE_ADVERTISEMENT = 1

        /**
         * Match few advertisement per filter, depends on current capability and availability of
         * the resources in hw
         */
        const val MATCH_NUM_FEW_ADVERTISEMENT = 2

        /**
         * Match as many advertisement per filter as hw could allow, depends on current
         * capability and availability of the resources in hw
         */
        const val MATCH_NUM_MAX_ADVERTISEMENT = 3

        /**
         * In Aggressive mode, hw will determine a match sooner even with feeble signal strength
         * and few number of sightings/match in a duration.
         */
        const val MATCH_MODE_AGGRESSIVE = 1

        /**
         * For sticky mode, higher threshold of signal strength and sightings is required
         * before reporting by hw
         */
        const val MATCH_MODE_STICKY = 2

        /**
         * Request full scan results which contain the device, rssi, advertising data, scan
         * response as well as the scan timestamp.
         *
         * @hide
         */
        val SCAN_RESULT_TYPE_FULL = 0

        /**
         * Request abbreviated scan results which contain the device, rssi and scan timestamp.
         *
         *
         * **Note:** It is possible for an application to get more scan results than it asked
         * for, if there are multiple apps using this type.
         *
         * @hide
         */
        val SCAN_RESULT_TYPE_ABBREVIATED = 1

        /**
         * Use all supported PHYs for scanning.
         * This will check the controller capabilities, and start
         * the scan on 1Mbit and LE Coded PHYs if supported, or on
         * the 1Mbit PHY only.
         */
        const val PHY_LE_ALL_SUPPORTED = 255
    }

    // TODO(ofy) Add remainder of ScanSettings
    // ...
}
