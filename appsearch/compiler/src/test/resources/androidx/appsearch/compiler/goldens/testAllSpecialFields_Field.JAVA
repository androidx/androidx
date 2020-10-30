package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_TYPE)
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("price")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    String namespaceCopy = dataClass.namespace;
    if (namespaceCopy != null) {
      builder.setNamespace(namespaceCopy);
    }
    builder.setCreationTimestampMillis(dataClass.creationTs);
    builder.setTtlMillis(dataClass.ttlMs);
    builder.setScore(dataClass.score);
    builder.setPropertyLong("price", dataClass.price);
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String namespaceConv = genericDoc.getNamespace();
    long creationTsConv = genericDoc.getCreationTimestampMillis();
    long ttlMsConv = genericDoc.getTtlMillis();
    int scoreConv = genericDoc.getScore();
    int priceConv = (int) genericDoc.getPropertyLong("price");
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.namespace = namespaceConv;
    dataClass.creationTs = creationTsConv;
    dataClass.ttlMs = ttlMsConv;
    dataClass.price = priceConv;
    dataClass.score = scoreConv;
    return dataClass;
  }
}
