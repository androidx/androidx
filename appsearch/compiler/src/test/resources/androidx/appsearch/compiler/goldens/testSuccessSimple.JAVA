package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  private static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("price")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("cat")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("dog")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() {
    return SCHEMA;
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    builder.setProperty("price", dataClass.price);
    builder.setProperty("cat", dataClass.cat);
    builder.setProperty("dog", dataClass.getDog());
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) {
    String uriConv = genericDoc.getUri();
    int priceConv = genericDoc.getPropertyLong("price");
    boolean catConv = genericDoc.getPropertyBoolean("cat");
    boolean dogConv = genericDoc.getPropertyBoolean("dog");
    Gift dataClass = new Gift(dogConv, uriConv);
    dataClass.price = priceConv;
    dataClass.cat = catConv;
    return dataClass;
  }
}
