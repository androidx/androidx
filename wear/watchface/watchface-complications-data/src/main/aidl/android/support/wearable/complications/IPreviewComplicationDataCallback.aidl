package android.support.wearable.complications;

import android.support.wearable.complications.ComplicationData;

/**
 * Callback for IProviderInfoService.requestPreviewComplicationData to fetch
 * preview {@link ComplicationData} from a provider.
 *
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
oneway interface IPreviewComplicationDataCallback {
    /**
     * Updates complication data for the given complication.
     */
    void updateComplicationData(in ComplicationData data);
}
