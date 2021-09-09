package androidx.health.services.client.data

import android.os.Parcelable
import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.StatisticalDataPoint as StatisticalDataPointProto
import java.time.Instant

/**
 * An [AggregateDataPoint] containing the statistical aggregates [min], [max], and [average] for the
 * type [dataType] between [startTime] and [endTime]. This should generally correspond with
 * [DataPoint]s of type [DataType.TimeType.SAMPLE].
 */
@Suppress("ParcelCreator")
public class StatisticalDataPoint(
    public val startTime: Instant,
    public val endTime: Instant,
    public val dataType: DataType,
    public val min: Value,
    public val max: Value,
    public val average: Value,
) : AggregateDataPoint() {

    internal constructor(
        proto: DataProto.AggregateDataPoint
    ) : this(
        Instant.ofEpochMilli(proto.statisticalDataPoint.startTimeEpochMs),
        Instant.ofEpochMilli(proto.statisticalDataPoint.endTimeEpochMs),
        DataType(proto.statisticalDataPoint.dataType),
        Value(proto.statisticalDataPoint.minValue),
        Value(proto.statisticalDataPoint.maxValue),
        Value(proto.statisticalDataPoint.avgValue)
    )

    /** @hide */
    override val proto: DataProto.AggregateDataPoint by lazy {
        DataProto.AggregateDataPoint.newBuilder()
            .setStatisticalDataPoint(
                StatisticalDataPointProto.newBuilder()
                    .setStartTimeEpochMs(startTime.toEpochMilli())
                    .setEndTimeEpochMs(endTime.toEpochMilli())
                    .setDataType(dataType.proto)
                    .setMinValue(min.proto)
                    .setMaxValue(max.proto)
                    .setAvgValue(average.proto)
                    .build()
            )
            .build()
    }

    override fun toString(): String =
        "StatisticalDataPoint(" +
            "startTime=$startTime, " +
            "endTime=$endTime, " +
            "dataType=$dataType, " +
            "min=$min, " +
            "max=$max, " +
            "average=$average)"

    public companion object {
        @JvmField
        public val CREATOR: Parcelable.Creator<StatisticalDataPoint> = newCreator {
            val proto = DataProto.AggregateDataPoint.parseFrom(it)
            StatisticalDataPoint(proto)
        }
    }
}
