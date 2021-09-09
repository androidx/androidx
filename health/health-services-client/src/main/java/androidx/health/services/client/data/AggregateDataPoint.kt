package androidx.health.services.client.data

import androidx.health.services.client.proto.DataProto
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.AggregateCase.AGGREGATE_NOT_SET
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.AggregateCase.CUMULATIVE_DATA_POINT
import androidx.health.services.client.proto.DataProto.AggregateDataPoint.AggregateCase.STATISTICAL_DATA_POINT

/** Accuracy of a [DataPoint]. */
@Suppress("ParcelCreator", "ParcelNotFinal")
public abstract class AggregateDataPoint : ProtoParcelable<DataProto.AggregateDataPoint>() {

    internal companion object {
        internal fun fromProto(proto: DataProto.AggregateDataPoint): AggregateDataPoint =
            when (proto.aggregateCase) {
                CUMULATIVE_DATA_POINT -> CumulativeDataPoint(proto)
                STATISTICAL_DATA_POINT -> StatisticalDataPoint(proto)
                null, AGGREGATE_NOT_SET ->
                    throw IllegalStateException("Aggregate not set on $proto")
            }
    }
}
