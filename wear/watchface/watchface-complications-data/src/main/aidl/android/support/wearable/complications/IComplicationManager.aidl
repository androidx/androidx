package android.support.wearable.complications;

import android.support.wearable.complications.ComplicationData;

/**
 * Interface of a service that allows data providers to interact with the complications manager.
 *
 */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IComplicationManager {
    /**
     * Updates complication data for the given complication slot.
     */
    void updateComplicationData(int complicationSlotId, in ComplicationData data);
}
