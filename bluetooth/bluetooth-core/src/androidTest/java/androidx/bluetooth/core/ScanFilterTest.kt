package androidx.bluetooth.core

import android.bluetooth.le.ScanFilter as FwkScanFilter
import android.os.Build
import android.os.ParcelUuid
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScanFilterTest {

    companion object {
        val TEST_DEVICE_NAME = "test_device_name"
        val TEST_DEVICE_ADDRESS = "11:22:33:44:55:66"
        val TEST_SERVICE_UUID = ParcelUuid.fromString("FFFFFFF0-FFFF-FFFF-FFFF-FFFFFFFFFFFF")
        val TEST_SERVICE_DATA_UUID = ParcelUuid.fromString("DDDDDDD0-DDDD-DDDD-DDDD-DDDDDDDDDDDD")
        val TEST_SERVICE_DATA = "SERVICE-DATA".toByteArray()
        val TEST_MANUFACTURER_ID = 1000
        val TEST_MANUFACTURER_DATA = "MANUFACTURER-DATA".toByteArray()
        val TEST_SERVICE_SOLICITATION_UUID =
            ParcelUuid.fromString("CCCCCCC0-CCCC-CCCC-CCCC-CCCCCCCCCCCC")
    }

    @Test
    fun constructorWithValues_createsFrameworkInstanceCorrectly() {
        val scanFilter = ScanFilter(
            deviceName = TEST_DEVICE_NAME,
            deviceAddress = TEST_DEVICE_ADDRESS,
            serviceUuid = TEST_SERVICE_UUID,
            serviceDataUuid = TEST_SERVICE_DATA_UUID,
            serviceData = TEST_SERVICE_DATA,
            manufacturerId = TEST_MANUFACTURER_ID,
            manufacturerData = TEST_MANUFACTURER_DATA,
            serviceSolicitationUuid = TEST_SERVICE_SOLICITATION_UUID
        )
        val fwkScanFilter = (scanFilter.impl as ScanFilterImpl).fwkInstance
        assertThat(fwkScanFilter.deviceName).isEqualTo(TEST_DEVICE_NAME)
        assertThat(fwkScanFilter.deviceAddress).isEqualTo(TEST_DEVICE_ADDRESS)
        assertThat(fwkScanFilter.serviceUuid).isEqualTo(TEST_SERVICE_UUID)
        assertThat(fwkScanFilter.serviceDataUuid).isEqualTo(TEST_SERVICE_DATA_UUID)
        assertThat(fwkScanFilter.serviceData).isEqualTo(TEST_SERVICE_DATA)
        assertThat(fwkScanFilter.manufacturerId).isEqualTo(TEST_MANUFACTURER_ID)
        assertThat(fwkScanFilter.manufacturerData).isEqualTo(TEST_MANUFACTURER_DATA)
        if (Build.VERSION.SDK_INT > 29) {
            assertThat(fwkScanFilter.serviceSolicitationUuid)
                .isEqualTo(TEST_SERVICE_SOLICITATION_UUID)
        }
    }

    @Test
    fun constructorWithFwkInstance_createsScanFilterCorrectly() {
        val fwkScanFilterBuilder = FwkScanFilter.Builder()
            .setDeviceName(TEST_DEVICE_NAME)
            .setDeviceAddress(TEST_DEVICE_ADDRESS)
            .setServiceUuid(TEST_SERVICE_UUID)
            .setServiceData(TEST_SERVICE_DATA_UUID, TEST_SERVICE_DATA)
            .setManufacturerData(TEST_MANUFACTURER_ID, TEST_MANUFACTURER_DATA)
        if (Build.VERSION.SDK_INT > 29) {
            fwkScanFilterBuilder.setServiceSolicitationUuid(TEST_SERVICE_SOLICITATION_UUID)
        }

        val scanFilter = ScanFilter(fwkScanFilterBuilder.build())

        assertThat(scanFilter.deviceName).isEqualTo(TEST_DEVICE_NAME)
        assertThat(scanFilter.deviceAddress).isEqualTo(TEST_DEVICE_ADDRESS)
        assertThat(scanFilter.serviceUuid).isEqualTo(TEST_SERVICE_UUID)
        assertThat(scanFilter.serviceDataUuid).isEqualTo(TEST_SERVICE_DATA_UUID)
        assertThat(scanFilter.serviceData).isEqualTo(TEST_SERVICE_DATA)
        assertThat(scanFilter.manufacturerId).isEqualTo(TEST_MANUFACTURER_ID)
        assertThat(scanFilter.manufacturerData).isEqualTo(TEST_MANUFACTURER_DATA)
        if (Build.VERSION.SDK_INT > 29) {
            assertThat(scanFilter.serviceSolicitationUuid)
                .isEqualTo(TEST_SERVICE_SOLICITATION_UUID)
        }
    }

    @Test
    fun scanFilterBundleable() {
        val scanFilter = ScanFilter(
            deviceName = TEST_DEVICE_NAME,
            deviceAddress = TEST_DEVICE_ADDRESS,
            serviceUuid = TEST_SERVICE_UUID,
            serviceDataUuid = TEST_SERVICE_DATA_UUID,
            serviceData = TEST_SERVICE_DATA,
            manufacturerId = TEST_MANUFACTURER_ID,
            manufacturerData = TEST_MANUFACTURER_DATA,
            serviceSolicitationUuid = TEST_SERVICE_SOLICITATION_UUID
        )
        val bundle = scanFilter.toBundle()

        val scanFilterFromBundle = ScanFilter.CREATOR.fromBundle(bundle)
        assertThat(scanFilterFromBundle.deviceName).isEqualTo(TEST_DEVICE_NAME)
        assertThat(scanFilterFromBundle.deviceAddress).isEqualTo(TEST_DEVICE_ADDRESS)
        assertThat(scanFilterFromBundle.serviceUuid).isEqualTo(TEST_SERVICE_UUID)
        assertThat(scanFilterFromBundle.serviceDataUuid).isEqualTo(TEST_SERVICE_DATA_UUID)
        assertThat(scanFilterFromBundle.serviceData).isEqualTo(TEST_SERVICE_DATA)
        assertThat(scanFilterFromBundle.manufacturerId).isEqualTo(TEST_MANUFACTURER_ID)
        assertThat(scanFilterFromBundle.manufacturerData).isEqualTo(TEST_MANUFACTURER_DATA)
        assertThat(scanFilterFromBundle.serviceSolicitationUuid)
            .isEqualTo(TEST_SERVICE_SOLICITATION_UUID)
    }
}
