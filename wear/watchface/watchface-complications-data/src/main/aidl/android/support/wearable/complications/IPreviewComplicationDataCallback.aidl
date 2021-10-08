package android.support.wearable.complications;

import android.support.wearable.complications.ComplicationData;

/**
 * Callback for IProviderInfoService.requestPreviewComplicationData to fetch
 * preview {@link ComplicationData} from a provider.
 *
 * @hide
 */
oneway interface IPreviewComplicationDataCallback {
    /**
     * Updates complication data for the given complication.
     */
    void updateComplicationData(in ComplicationData data);
}
