package android.support.wearable.complications;

import android.support.wearable.complications.ComplicationData;

/**
 * Interface for a service that allows data providers to receive information.
 *
 * @hide
 */
interface IComplicationProvider {
    // IMPORTANT NOTE: All methods must be given an explicit transaction id that must never change
    // in the future to remain binary backwards compatible.
    // Next Id: 5

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 1;

    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * <p>In response to this request, {@link IComplicationManager#updateComplicationData} should be
     * called on the {@link IComplicationManager} after binding, with the data to be displayed. Or
     * if no update is needed, {@link
     * androidx.wear.complications.ComplicationManager#noUpdateRequired noUpdateRequired} may be
     * called instead. One of these methods must be called so that the system knows when the
     * provider has finished responding to  the request.
     *
     * @since API version 0.
     */
    void onUpdate(int complicationId, int type, IBinder manager) = 0;

    /**
     * Called when a complication is deactivated.
     *
     * <p>This occurs when the current watch face changes, or when the watch face calls
     * setActiveComplications and does not include the given complication (usually because the watch
     * face has stopped displaying it).
     *
     * <p>Once this has been called, no complication data should be sent for the given {@code
     * complicationId}, until {@link #onComplicationActivated} is called again for that id.
     *
     * @since API version 0.
     */
    void onComplicationDeactivated(int complicationId) = 1;

    /**
     * Called when a complication is activated.
     *
     * <p>This occurs when the watch face calls setActiveComplications, or when this provider is
     * chosen for a complication which is already active.
     *
     * @since API version 0.
     */
    void onComplicationActivated(int complicationId, int type, IBinder manager) = 2;

    /**
     * Returns the version number for this API which the client can use to determine which methods
     * are available. Note old implementations without this method will return zero.
     *
     * @since API version 1.
     */
    int getApiVersion() = 3;

    /**
     * Requests preview data for the given complication type for use in watch face editor UIs.
     * Preview data should be representative of the real data but is assumed to never change. E.g
     * rather than returning the real time and date for an appointment return Wed 10:10.
     *
     * @since API version 1
     */
    ComplicationData getComplicationPreviewData(int type) = 4;
}
