package android.support.wearable.complications;

import android.content.ComponentName;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.IPreviewComplicationDataCallback;

/**
 * Interface for a service that allows provider info to be retrieved from the system.
 *
 * @hide
 */
interface IProviderInfoService {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 3

    /**
    * API version number. This should be incremented every time a new method is added.
    */
    const int API_VERSION = 1;

    /**
     * Returns provider info for each of the provided ids, in an array in the same order as the ids.
     * For each id, if no provider is configured then null will be added at that index of the
     * result.
     */
    ComplicationProviderInfo[] getProviderInfos(
        in ComponentName watchFaceComponent, in int[] ids) = 0;

    /**
     * Returns the version number for this API which the client can use to determine which methods
     * are available.
     *
     * @since API version 1.  Note this will correctly return 0 for API level 0 due to the way AIDL
     * is implemented.
     */
    int getApiVersion() = 1;

    /**
     * Requests preview complication data for the specified provder component and complication type.
     * Returns true if the result will be delivered by previewComplicationDataCallback or false if
     * there was an error and no callback will be issued.
     *
     * @param providerComponent The {@link ComponentName} of the provider we want preview data for
     * @param complicationType The type of the complication, see
     *      {@link ComplicationData.ComplicationType}
     * @param previewComplicationDataCallback The {@link IPreviewComplicationDataCallback} on which
     *      any preview data will be reported
     * @since API version 1.
     */
    boolean requestPreviewComplicationData(
        in ComponentName providerComponent,
        int complicationType,
        in IPreviewComplicationDataCallback previewComplicationDataCallback) = 2;
}
