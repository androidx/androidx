package com.sdkwithcallbacks

public object MyEnumConverter {
    public fun fromParcelable(parcelable: ParcelableMyEnum): MyEnum =
            MyEnum.entries[parcelable.variant_ordinal]

    public fun toParcelable(annotatedValue: MyEnum): ParcelableMyEnum {
        val parcelable = ParcelableMyEnum()
        parcelable.variant_ordinal = annotatedValue.ordinal
        return parcelable
    }
}
