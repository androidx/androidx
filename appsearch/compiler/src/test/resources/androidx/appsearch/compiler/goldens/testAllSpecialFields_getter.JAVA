package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.Int64PropertyConfig.Builder("price")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.getUri(), SCHEMA_NAME);
    builder.setCreationTimestampMillis(document.getCreationTs());
    builder.setTtlMillis(document.getTtlMs());
    builder.setScore(document.getScore());
    builder.setPropertyLong("price", document.getPrice());
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
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.setUri(uriConv);
    document.setScore(scoreConv);
    document.setCreationTs(creationTsConv);
    document.setTtlMs(ttlMsConv);
    document.setPrice(priceConv);
    return document;
  }
}
