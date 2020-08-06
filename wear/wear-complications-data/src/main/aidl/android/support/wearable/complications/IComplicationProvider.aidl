package android.support.wearable.complications;

/**
 * Interface for a service that allows data providers to receive information.
 *
 * @hide
 */
interface IComplicationProvider {
    /**
     * Called when a complication data update is requested for the given complication id.
     *
     * <p>In response to this request, {@link IComplicationManager#updateComplicationData} should be
     * called on the {@link IComplicationManager} after binding, with the data to be displayed. Or
     * if no update is needed, {@link
     * androidx.wear.complications.ComplicationManager#noUpdateRequired noUpdateRequired} may be
     * called instead. One of these methods must be called so that the system knows when the
     * provider has finished responding to  the request.
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
     */
    void onComplicationDeactivated(int complicationId) = 1;

    /**
     * Called when a complication is activated.
     *
     * <p>This occurs when the watch face calls setActiveComplications, or when this provider is
     * chosen for a complication which is already active.
     */
    void onComplicationActivated(int complicationId, int type, IBinder manager) = 2;
}