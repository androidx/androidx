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
        new GenericDocument.Builder<>(dataClass.getUri(), SCHEMA_TYPE);
    builder.setCreationTimestampMillis(dataClass.getCreationTs());
    builder.setTtlMillis(dataClass.getTtlMs());
    builder.setScore(dataClass.getScore());
    builder.setProperty("price", dataClass.getPrice());
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) {
    String uriConv = genericDoc.getUri();
    long creationTsConv = genericDoc.getCreationTimestampMillis();
    long ttlMsConv = genericDoc.getTtlMillis();
    int scoreConv = genericDoc.getScore();
    int priceConv = genericDoc.getPropertyLong("price");
    Gift dataClass = new Gift();
    dataClass.setUri(uriConv);
    dataClass.setScore(scoreConv);
    dataClass.setCreationTs(creationTsConv);
    dataClass.setTtlMs(ttlMsConv);
    dataClass.setPrice(priceConv);
    return dataClass;
  }
}
