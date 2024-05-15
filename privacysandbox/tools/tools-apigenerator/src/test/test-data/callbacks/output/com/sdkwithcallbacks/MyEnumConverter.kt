package com.sdkwithcallbacks

public object MyEnumConverter {
    private val enumValues: List<MyEnum> = MyEnum.values().toList()

    public fun fromParcelable(parcelable: ParcelableMyEnum): MyEnum =
            enumValues[parcelable.variant_ordinal]

    public fun toParcelable(annotatedValue: MyEnum): ParcelableMyEnum {
        val parcelable = ParcelableMyEnum()
        parcelable.variant_ordinal = annotatedValue.ordinal
        return parcelable
    }
}
