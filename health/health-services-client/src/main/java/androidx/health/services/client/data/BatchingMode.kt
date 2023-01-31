package androidx.health.services.client.data

import androidx.health.services.client.proto.DataProto

/**
 * Batching mode during an active exercise when the device is in a non-interactive power state, used
 * in [ExerciseConfig]. Not applicable when device is in interactive state because exercise updates
 * will be streaming.
 */
public class BatchingMode
internal constructor(
    /** Unique identifier for the [BatchingMode], as an `int`. */
    internal val id: Int,
) {

    internal constructor(
        proto: DataProto.BatchingMode
    ) : this(
        proto.number,
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BatchingMode) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id
    }

    internal fun toProto(): DataProto.BatchingMode =
        DataProto.BatchingMode.forNumber(id) ?: DataProto.BatchingMode.BATCHING_MODE_UNKNOWN

    public companion object {
        /**
         * Batching mode for receiving [DataType.HEART_RATE_BPM] updates with fast frequency.
         *
         * Note: This mode will cause significantly increased power consumption compared to the
         * default batching mode, while still being more power efficient than streaming when in
         * non-interactive state. The exact power/performance tradeoff of this mode is device
         * implementation dependent but aims for roughly five second updates.
         */
        @JvmField public val HEART_RATE_5_SECONDS: BatchingMode = BatchingMode(1)

        @JvmStatic
        internal fun fromProto(proto: DataProto.BatchingMode): BatchingMode =
            BatchingMode(proto.number)
    }
}
