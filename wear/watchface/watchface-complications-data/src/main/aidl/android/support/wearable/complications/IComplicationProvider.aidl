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
    // Next Id: 8

    /**
     * API version number. This should be incremented every time a new method is added.
     */
    const int API_VERSION = 3;

    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * <p>In response to this request, {@link IComplicationManager#updateComplicationData} should be
     * called on the {@link IComplicationManager} after binding, with the data to be displayed. Or
     * if no update is needed, {@link
     * androidx.wear.watchface.complications.ComplicationManager#noUpdateRequired noUpdateRequired}
     * may be called instead. One of these methods must be called so that the system knows when the
     * provider has finished responding to  the request.
     *
     * This must not be called after onStartSynchronousComplicationRequests until o subsequent
     * StopSynchronousComplicationRequests call.
     *
     * @param complicationInstanceId The system's id for the updated complication which is a unique
     * value for the tuple [Watch face ComponentName, complication slot ID].
     * @param type The type of complication requested
     * @param manager The binder for IComplicationManager
     * @since API version 0.
     */
    void onUpdate(int complicationInstanceId, int type, IBinder manager) = 0;

    /**
     * Called when a complication is deactivated.
     *
     * <p>This occurs when the current watch face changes, or when the watch face calls
     * setActiveComplications and does not include the given complication (usually because the watch
     * face has stopped displaying it).
     *
     * <p>Once this has been called, no complication data should be sent for the given {@code
     * complicationInstanceId}, until {@link #onComplicationActivated} is called again for that id.
     *
     * @param complicationInstanceId The system's id for the deactivated complication which is a
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @since API version 0.
     */
    void onComplicationDeactivated(int complicationInstanceId) = 1;

    /**
     * Called when a complication is activated.
     *
     * <p>This occurs when the watch face calls setActiveComplications, or when this provider is
     * chosen for a complication which is already active.
     *
     * @param complicationInstanceId The system's id for the requested complication which is a
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @since API version 0.
     */
    void onComplicationActivated(int complicationInstanceId, int type, IBinder manager) = 2;

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
     * @param type The type of complication preview requested
     * @since API version 1
     */
    ComplicationData getComplicationPreviewData(int type) = 4;

    /**
     * If a metadata key with
     * androidx.wear.watchface.complications.data.source.SYNCHRONOUS_UPDATE_PERIOD_MILLISECONDS is
     * present in the manifest, and the watch face becomes visible and non-ambient then
     * onStartSynchronousComplicationRequests will be called. A series of calls to
     * onSynchronousComplicationRequest will follow, ending with a call to
     * onStopSynchronousComplicationRequests.
     *
     * After onStartSynchronousComplicationRequests calls to onComplicationRequest will stop until
     * the complication ceases to be visible and non-ambient.
     *
     * @param complicationInstanceId The system's id for the requested complication which is a
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @since API version 2.
     */
    void onStartSynchronousComplicationRequests(int complicationInstanceId) = 5;

    /**
     * If a metadata key with
     * androidx.wear.watchface.complications.data.source.SYNCHRONOUS_UPDATE_PERIOD_MILLISECONDS is
     * present in the manifest, when the watch face ceases to be visible and non ambient
     * onStopSynchronousComplicationRequests will be called. After this no subsequent calls to
     * onSynchronousComplicationRequest will me made unless the watch face becomes visible and non
     * ambient again. However calls to onComplicationRequest may resume (depending on
     * the value of METADATA_KEY_UPDATE_PERIOD_SECONDS).
     *
     * @param complicationInstanceId The system's id for the requested complication which is a
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @since API version 2.
     */
    void onStopSynchronousComplicationRequests(int complicationInstanceId) = 6;

    /**
     * If a metadata key with
     * androidx.wear.watchface.complications.data.source.SYNCHRONOUS_UPDATE_PERIOD_MILLISECONDS is
     * present in the manifest, then onSynchronousComplicationRequest will be called while the
     * complication is in this mode (i.e. while the watch face is visible and not ambient).
     *
     * In response to this request the [ComplicationData] must be returned immediately, or `null`
     * returned if there's either no data available or no update is necessary.
     *
     * @param complicationInstanceId The system's id for the requested complication which is a
     * @param type The type of complication requested
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @return The updated ComplicationData or null if no update is necessary
     * @since API version 2.
     */
    ComplicationData onSynchronousComplicationRequest(int complicationInstanceId, int type) = 7;

    /**
     * Same as {@link #onUpdate}, but specifies isForSafeWatchFace.
     *
     * @param complicationInstanceId The system's id for the updated complication which is a unique
     * value for the tuple [Watch face ComponentName, complication slot ID].
     * @param type The type of complication requested
     * @param isForSafeWatchFace Whether this request is on behalf of a safe watchface as defined by
     * the data source. Must be one of {@link #IS_SAFE_FOR_WATCHFACE_NULL},
     * {@link #IS_SAFE_FOR_WATCHFACE_TRUE} or {@link #IS_SAFE_FOR_WATCHFACE_FALSE}.
     * @param manager The binder for IComplicationManager
     * @since API version 3.
     */
    void onUpdate2(int complicationInstanceId, int type, int isForSafeWatchFace,
        IBinder manager) = 8;

    /**
     * Same as {@link #onSynchronousComplicationRequest2}, but specifies isForSafeWatchFace.
     *
     * @param complicationInstanceId The system's id for the requested complication which is a
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @param isForSafeWatchFace Whether this request is on behalf of a safe watchface as defined by
     * the data source. Must be one of {@link #IS_SAFE_FOR_WATCHFACE_NULL},
     * {@link #IS_SAFE_FOR_WATCHFACE_TRUE} or {@link #IS_SAFE_FOR_WATCHFACE_FALSE}.
     * @param type The type of complication requested
     * unique value for the tuple [Watch face ComponentName, complication slot ID].
     * @return The updated ComplicationData or null if no update is necessary
     * @since API version 3.
     */
    ComplicationData onSynchronousComplicationRequest2(int complicationInstanceId,
        int isForSafeWatchFace, int type) = 9;
}
