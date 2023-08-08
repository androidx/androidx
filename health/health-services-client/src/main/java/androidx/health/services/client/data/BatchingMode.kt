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
         * Deliver smaller and more frequent batches of [DataType.HEART_RATE_BPM] when the device is
         * not interactive (e.g. screen is off).
         *
         * This setting significantly increases power consumption, and is intended to be used by
         * apps which need to send data to a separate device (e.g. a connected phone or TV) for
         * real-time visualisation. It has no effect if the device is interactive.
         *
         * The exact power/performance tradeoff of this mode is device implementation dependent and
         * batched updates may be aligned with other wake ups but target five second updates.
         */
        @JvmField public val HEART_RATE_5_SECONDS: BatchingMode = BatchingMode(1)

        @JvmStatic
        internal fun fromProto(proto: DataProto.BatchingMode): BatchingMode =
            BatchingMode(proto.number)
    }
}
