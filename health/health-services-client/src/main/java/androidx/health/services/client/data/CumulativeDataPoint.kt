package androidx.health.services.client.data

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.CumulativeDataPoint as CumulativeDataPointProto
import java.time.Instant

/**
 * An [AggregateDataPoint] containing a cumulative [total] for the type [dataType] between
 * [startTime] and [endTime]. This should generally correspond with [DataPoint]s of type
 * [DataType.TimeType.INTERVAL].
 */
@Suppress("ParcelCreator")
public class CumulativeDataPoint(
    public val startTime: Instant,
    public val endTime: Instant,
    public val dataType: DataType,
    public val total: Value,
) : AggregateDataPoint() {

    internal constructor(
        proto: DataProto.AggregateDataPoint
    ) : this(
        Instant.ofEpochMilli(proto.cumulativeDataPoint.startTimeEpochMs),
        Instant.ofEpochMilli(proto.cumulativeDataPoint.endTimeEpochMs),
        DataType(proto.cumulativeDataPoint.dataType),
        Value(proto.cumulativeDataPoint.total)
    )

    /** @hide */
    override val proto: DataProto.AggregateDataPoint by lazy {
        DataProto.AggregateDataPoint.newBuilder()
            .setCumulativeDataPoint(
                CumulativeDataPointProto.newBuilder()
                    .setStartTimeEpochMs(startTime.toEpochMilli())
                    .setEndTimeEpochMs(endTime.toEpochMilli())
                    .setDataType(dataType.proto)
                    .setTotal(total.proto)
                    .build()
            )
            .build()
    }

    override fun toString(): String =
        "CumulativeDataPoint(" +
            "startTime=$startTime, " +
            "endTime=$endTime, " +
            "dataType=$dataType, " +
            "total=$total)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<CumulativeDataPoint> = newCreator {
            val proto = DataProto.AggregateDataPoint.parseFrom(it)
            CumulativeDataPoint(proto)
        }
    }
}
