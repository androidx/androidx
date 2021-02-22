package android.support.wearable.complications;

import android.support.wearable.complications.ComplicationData;

/**
 * Interface of a service that allows data providers to interact with the complications manager.
 *
 * {@hide}
 */
interface IComplicationManager {
    /**
     * Updates complication data for the given complication.
     */
    void updateComplicationData(int complicationId, in ComplicationData data);
}
