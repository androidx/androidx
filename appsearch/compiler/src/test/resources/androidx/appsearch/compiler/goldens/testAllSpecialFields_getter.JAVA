package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("price")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.getId(), SCHEMA_NAME);
    builder.setScore(document.getScore());
    builder.setCreationTimestampMillis(document.getCreationTs());
    builder.setTtlMillis(document.getTtlMs());
    builder.setPropertyLong("price", document.getPrice());
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    long creationTsConv = genericDoc.getCreationTimestampMillis();
    long ttlMsConv = genericDoc.getTtlMillis();
    int scoreConv = genericDoc.getScore();
    int priceConv = (int) genericDoc.getPropertyLong("price");
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.setId(idConv);
    document.setScore(scoreConv);
    document.setCreationTs(creationTsConv);
    document.setTtlMs(ttlMsConv);
    document.setPrice(priceConv);
    return document;
  }
}
