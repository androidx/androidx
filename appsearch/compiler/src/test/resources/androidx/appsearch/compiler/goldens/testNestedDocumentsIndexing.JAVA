package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Collection;
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
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("giftContentsCollection", $$__AppSearch__GiftContent.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(true)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("giftContentsArray", $$__AppSearch__GiftContent.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(true)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("giftContent", $$__AppSearch__GiftContent.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(true)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("giftContentsCollectionNotIndexed", $$__AppSearch__GiftContent.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("giftContentsArrayNotIndexed", $$__AppSearch__GiftContent.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("giftContentNotIndexed", $$__AppSearch__GiftContent.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(GiftContent.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    Collection<GiftContent> giftContentsCollectionCopy = document.giftContentsCollection;
    if (giftContentsCollectionCopy != null) {
      GenericDocument[] giftContentsCollectionConv = new GenericDocument[giftContentsCollectionCopy.size()];
      int i = 0;
      for (GiftContent item : giftContentsCollectionCopy) {
        giftContentsCollectionConv[i++] = GenericDocument.fromDocumentClass(item);
      }
      builder.setPropertyDocument("giftContentsCollection", giftContentsCollectionConv);
    }
    GiftContent[] giftContentsArrayCopy = document.giftContentsArray;
    if (giftContentsArrayCopy != null) {
      GenericDocument[] giftContentsArrayConv = new GenericDocument[giftContentsArrayCopy.length];
      for (int i = 0; i < giftContentsArrayConv.length; i++) {
        giftContentsArrayConv[i] = GenericDocument.fromDocumentClass(giftContentsArrayCopy[i]);
      }
      builder.setPropertyDocument("giftContentsArray", giftContentsArrayConv);
    }
    GiftContent giftContentCopy = document.giftContent;
    if (giftContentCopy != null) {
      GenericDocument giftContentConv = GenericDocument.fromDocumentClass(giftContentCopy);
      builder.setPropertyDocument("giftContent", giftContentConv);
    }
    Collection<GiftContent> giftContentsCollectionNotIndexedCopy = document.giftContentsCollectionNotIndexed;
    if (giftContentsCollectionNotIndexedCopy != null) {
      GenericDocument[] giftContentsCollectionNotIndexedConv = new GenericDocument[giftContentsCollectionNotIndexedCopy.size()];
      int i = 0;
      for (GiftContent item : giftContentsCollectionNotIndexedCopy) {
        giftContentsCollectionNotIndexedConv[i++] = GenericDocument.fromDocumentClass(item);
      }
      builder.setPropertyDocument("giftContentsCollectionNotIndexed", giftContentsCollectionNotIndexedConv);
    }
    GiftContent[] giftContentsArrayNotIndexedCopy = document.giftContentsArrayNotIndexed;
    if (giftContentsArrayNotIndexedCopy != null) {
      GenericDocument[] giftContentsArrayNotIndexedConv = new GenericDocument[giftContentsArrayNotIndexedCopy.length];
      for (int i = 0; i < giftContentsArrayNotIndexedConv.length; i++) {
        giftContentsArrayNotIndexedConv[i] = GenericDocument.fromDocumentClass(giftContentsArrayNotIndexedCopy[i]);
      }
      builder.setPropertyDocument("giftContentsArrayNotIndexed", giftContentsArrayNotIndexedConv);
    }
    GiftContent giftContentNotIndexedCopy = document.giftContentNotIndexed;
    if (giftContentNotIndexedCopy != null) {
      GenericDocument giftContentNotIndexedConv = GenericDocument.fromDocumentClass(giftContentNotIndexedCopy);
      builder.setPropertyDocument("giftContentNotIndexed", giftContentNotIndexedConv);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    GenericDocument[] giftContentsCollectionCopy = genericDoc.getPropertyDocumentArray("giftContentsCollection");
    List<GiftContent> giftContentsCollectionConv = null;
    if (giftContentsCollectionCopy != null) {
      giftContentsCollectionConv = new ArrayList<>(giftContentsCollectionCopy.length);
      for (int i = 0; i < giftContentsCollectionCopy.length; i++) {
        giftContentsCollectionConv.add(giftContentsCollectionCopy[i].toDocumentClass(GiftContent.class));
      }
    }
    GenericDocument[] giftContentsArrayCopy = genericDoc.getPropertyDocumentArray("giftContentsArray");
    GiftContent[] giftContentsArrayConv = null;
    if (giftContentsArrayCopy != null) {
      giftContentsArrayConv = new GiftContent[giftContentsArrayCopy.length];
      for (int i = 0; i < giftContentsArrayCopy.length; i++) {
        giftContentsArrayConv[i] = giftContentsArrayCopy[i].toDocumentClass(GiftContent.class);
      }
    }
    GenericDocument giftContentCopy = genericDoc.getPropertyDocument("giftContent");
    GiftContent giftContentConv = null;
    if (giftContentCopy != null) {
      giftContentConv = giftContentCopy.toDocumentClass(GiftContent.class);
    }
    GenericDocument[] giftContentsCollectionNotIndexedCopy = genericDoc.getPropertyDocumentArray("giftContentsCollectionNotIndexed");
    List<GiftContent> giftContentsCollectionNotIndexedConv = null;
    if (giftContentsCollectionNotIndexedCopy != null) {
      giftContentsCollectionNotIndexedConv = new ArrayList<>(giftContentsCollectionNotIndexedCopy.length);
      for (int i = 0; i < giftContentsCollectionNotIndexedCopy.length; i++) {
        giftContentsCollectionNotIndexedConv.add(giftContentsCollectionNotIndexedCopy[i].toDocumentClass(GiftContent.class));
      }
    }
    GenericDocument[] giftContentsArrayNotIndexedCopy = genericDoc.getPropertyDocumentArray("giftContentsArrayNotIndexed");
    GiftContent[] giftContentsArrayNotIndexedConv = null;
    if (giftContentsArrayNotIndexedCopy != null) {
      giftContentsArrayNotIndexedConv = new GiftContent[giftContentsArrayNotIndexedCopy.length];
      for (int i = 0; i < giftContentsArrayNotIndexedCopy.length; i++) {
        giftContentsArrayNotIndexedConv[i] = giftContentsArrayNotIndexedCopy[i].toDocumentClass(GiftContent.class);
      }
    }
    GenericDocument giftContentNotIndexedCopy = genericDoc.getPropertyDocument("giftContentNotIndexed");
    GiftContent giftContentNotIndexedConv = null;
    if (giftContentNotIndexedCopy != null) {
      giftContentNotIndexedConv = giftContentNotIndexedCopy.toDocumentClass(GiftContent.class);
    }
    Gift document = new Gift();
    document.id = idConv;
    document.namespace = namespaceConv;
    document.giftContentsCollection = giftContentsCollectionConv;
    document.giftContentsArray = giftContentsArrayConv;
    document.giftContent = giftContentConv;
    document.giftContentsCollectionNotIndexed = giftContentsCollectionNotIndexedConv;
    document.giftContentsArrayNotIndexed = giftContentsArrayNotIndexedConv;
    document.giftContentNotIndexed = giftContentNotIndexedConv;
    return document;
  }
}
