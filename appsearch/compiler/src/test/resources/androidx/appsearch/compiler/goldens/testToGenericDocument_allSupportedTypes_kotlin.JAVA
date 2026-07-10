package com.example.appsearch;

import androidx.appsearch.app.AppSearchBlobHandle;
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassMappingContext;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("collectLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("collectInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("collectNullInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nullCollectInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nullCollectNullInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("collectDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("collectFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("collectBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("collectByteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("collectString")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("collectGift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("collectVec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("collectBlob")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrBoxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrUnboxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrBoxInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrNullBoxInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nullArrBoxInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nullArrNullBoxInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrUnboxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrBoxDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrUnboxDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrBoxFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrUnboxFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("arrBoxBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("arrUnboxBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("arrUnboxByteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("arrString")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("arrGift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("arrVec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("arrBlob")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("string")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nonNullLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nullLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nonNullInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("nonNullDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("nonNullFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("nonNullBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("byteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("gift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("nullGift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("vec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BlobHandlePropertyConfig.Builder("blob")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Gift.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.getNamespace(), document.getId(), SCHEMA_NAME);
    Collection<Long> collectLongCopy = document.getCollectLong();
    if (collectLongCopy != null) {
      long[] collectLongConv = new long[collectLongCopy.size()];
      int i = 0;
      for (Long item : collectLongCopy) {
        collectLongConv[i++] = item;
      }
      builder.setPropertyLong("collectLong", collectLongConv);
    }
    Collection<Integer> collectIntegerCopy = document.getCollectInteger();
    if (collectIntegerCopy != null) {
      long[] collectIntegerConv = new long[collectIntegerCopy.size()];
      int i = 0;
      for (Integer item : collectIntegerCopy) {
        collectIntegerConv[i++] = item;
      }
      builder.setPropertyLong("collectInteger", collectIntegerConv);
    }
    Collection<Integer> collectNullIntegerCopy = document.getCollectNullInteger();
    if (collectNullIntegerCopy != null) {
      long[] collectNullIntegerConv = new long[collectNullIntegerCopy.size()];
      int i = 0;
      for (Integer item : collectNullIntegerCopy) {
        collectNullIntegerConv[i++] = item;
      }
      builder.setPropertyLong("collectNullInteger", collectNullIntegerConv);
    }
    Collection<Integer> nullCollectIntegerCopy = document.getNullCollectInteger();
    if (nullCollectIntegerCopy != null) {
      long[] nullCollectIntegerConv = new long[nullCollectIntegerCopy.size()];
      int i = 0;
      for (Integer item : nullCollectIntegerCopy) {
        nullCollectIntegerConv[i++] = item;
      }
      builder.setPropertyLong("nullCollectInteger", nullCollectIntegerConv);
    }
    Collection<Integer> nullCollectNullIntegerCopy = document.getNullCollectNullInteger();
    if (nullCollectNullIntegerCopy != null) {
      long[] nullCollectNullIntegerConv = new long[nullCollectNullIntegerCopy.size()];
      int i = 0;
      for (Integer item : nullCollectNullIntegerCopy) {
        nullCollectNullIntegerConv[i++] = item;
      }
      builder.setPropertyLong("nullCollectNullInteger", nullCollectNullIntegerConv);
    }
    Collection<Double> collectDoubleCopy = document.getCollectDouble();
    if (collectDoubleCopy != null) {
      double[] collectDoubleConv = new double[collectDoubleCopy.size()];
      int i = 0;
      for (Double item : collectDoubleCopy) {
        collectDoubleConv[i++] = item;
      }
      builder.setPropertyDouble("collectDouble", collectDoubleConv);
    }
    Collection<Float> collectFloatCopy = document.getCollectFloat();
    if (collectFloatCopy != null) {
      double[] collectFloatConv = new double[collectFloatCopy.size()];
      int i = 0;
      for (Float item : collectFloatCopy) {
        collectFloatConv[i++] = item;
      }
      builder.setPropertyDouble("collectFloat", collectFloatConv);
    }
    Collection<Boolean> collectBooleanCopy = document.getCollectBoolean();
    if (collectBooleanCopy != null) {
      boolean[] collectBooleanConv = new boolean[collectBooleanCopy.size()];
      int i = 0;
      for (Boolean item : collectBooleanCopy) {
        collectBooleanConv[i++] = item;
      }
      builder.setPropertyBoolean("collectBoolean", collectBooleanConv);
    }
    Collection<byte[]> collectByteArrCopy = document.getCollectByteArr();
    if (collectByteArrCopy != null) {
      byte[][] collectByteArrConv = new byte[collectByteArrCopy.size()][];
      int i = 0;
      for (byte[] item : collectByteArrCopy) {
        collectByteArrConv[i++] = item;
      }
      builder.setPropertyBytes("collectByteArr", collectByteArrConv);
    }
    Collection<String> collectStringCopy = document.getCollectString();
    if (collectStringCopy != null) {
      String[] collectStringConv = collectStringCopy.toArray(new String[0]);
      builder.setPropertyString("collectString", collectStringConv);
    }
    Collection<Gift> collectGiftCopy = document.getCollectGift();
    if (collectGiftCopy != null) {
      GenericDocument[] collectGiftConv = new GenericDocument[collectGiftCopy.size()];
      int i = 0;
      for (Gift item : collectGiftCopy) {
        collectGiftConv[i++] = GenericDocument.fromDocumentClass(item);
      }
      builder.setPropertyDocument("collectGift", collectGiftConv);
    }
    Collection<EmbeddingVector> collectVecCopy = document.getCollectVec();
    if (collectVecCopy != null) {
      EmbeddingVector[] collectVecConv = collectVecCopy.toArray(new EmbeddingVector[0]);
      builder.setPropertyEmbedding("collectVec", collectVecConv);
    }
    Collection<AppSearchBlobHandle> collectBlobCopy = document.getCollectBlob();
    if (collectBlobCopy != null) {
      AppSearchBlobHandle[] collectBlobConv = collectBlobCopy.toArray(new AppSearchBlobHandle[0]);
      builder.setPropertyBlobHandle("collectBlob", collectBlobConv);
    }
    Long[] arrBoxLongCopy = document.getArrBoxLong();
    if (arrBoxLongCopy != null) {
      long[] arrBoxLongConv = new long[arrBoxLongCopy.length];
      for (int i = 0; i < arrBoxLongCopy.length; i++) {
        arrBoxLongConv[i] = arrBoxLongCopy[i];
      }
      builder.setPropertyLong("arrBoxLong", arrBoxLongConv);
    }
    long[] arrUnboxLongCopy = document.getArrUnboxLong();
    if (arrUnboxLongCopy != null) {
      builder.setPropertyLong("arrUnboxLong", arrUnboxLongCopy);
    }
    Integer[] arrBoxIntegerCopy = document.getArrBoxInteger();
    if (arrBoxIntegerCopy != null) {
      long[] arrBoxIntegerConv = new long[arrBoxIntegerCopy.length];
      for (int i = 0; i < arrBoxIntegerCopy.length; i++) {
        arrBoxIntegerConv[i] = arrBoxIntegerCopy[i];
      }
      builder.setPropertyLong("arrBoxInteger", arrBoxIntegerConv);
    }
    Integer[] arrNullBoxIntegerCopy = document.getArrNullBoxInteger();
    if (arrNullBoxIntegerCopy != null) {
      long[] arrNullBoxIntegerConv = new long[arrNullBoxIntegerCopy.length];
      for (int i = 0; i < arrNullBoxIntegerCopy.length; i++) {
        arrNullBoxIntegerConv[i] = arrNullBoxIntegerCopy[i];
      }
      builder.setPropertyLong("arrNullBoxInteger", arrNullBoxIntegerConv);
    }
    Integer[] nullArrBoxIntegerCopy = document.getNullArrBoxInteger();
    if (nullArrBoxIntegerCopy != null) {
      long[] nullArrBoxIntegerConv = new long[nullArrBoxIntegerCopy.length];
      for (int i = 0; i < nullArrBoxIntegerCopy.length; i++) {
        nullArrBoxIntegerConv[i] = nullArrBoxIntegerCopy[i];
      }
      builder.setPropertyLong("nullArrBoxInteger", nullArrBoxIntegerConv);
    }
    Integer[] nullArrNullBoxIntegerCopy = document.getNullArrNullBoxInteger();
    if (nullArrNullBoxIntegerCopy != null) {
      long[] nullArrNullBoxIntegerConv = new long[nullArrNullBoxIntegerCopy.length];
      for (int i = 0; i < nullArrNullBoxIntegerCopy.length; i++) {
        nullArrNullBoxIntegerConv[i] = nullArrNullBoxIntegerCopy[i];
      }
      builder.setPropertyLong("nullArrNullBoxInteger", nullArrNullBoxIntegerConv);
    }
    int[] arrUnboxIntCopy = document.getArrUnboxInt();
    if (arrUnboxIntCopy != null) {
      long[] arrUnboxIntConv = new long[arrUnboxIntCopy.length];
      for (int i = 0; i < arrUnboxIntCopy.length; i++) {
        arrUnboxIntConv[i] = arrUnboxIntCopy[i];
      }
      builder.setPropertyLong("arrUnboxInt", arrUnboxIntConv);
    }
    Double[] arrBoxDoubleCopy = document.getArrBoxDouble();
    if (arrBoxDoubleCopy != null) {
      double[] arrBoxDoubleConv = new double[arrBoxDoubleCopy.length];
      for (int i = 0; i < arrBoxDoubleCopy.length; i++) {
        arrBoxDoubleConv[i] = arrBoxDoubleCopy[i];
      }
      builder.setPropertyDouble("arrBoxDouble", arrBoxDoubleConv);
    }
    double[] arrUnboxDoubleCopy = document.getArrUnboxDouble();
    if (arrUnboxDoubleCopy != null) {
      builder.setPropertyDouble("arrUnboxDouble", arrUnboxDoubleCopy);
    }
    Float[] arrBoxFloatCopy = document.getArrBoxFloat();
    if (arrBoxFloatCopy != null) {
      double[] arrBoxFloatConv = new double[arrBoxFloatCopy.length];
      for (int i = 0; i < arrBoxFloatCopy.length; i++) {
        arrBoxFloatConv[i] = arrBoxFloatCopy[i];
      }
      builder.setPropertyDouble("arrBoxFloat", arrBoxFloatConv);
    }
    float[] arrUnboxFloatCopy = document.getArrUnboxFloat();
    if (arrUnboxFloatCopy != null) {
      double[] arrUnboxFloatConv = new double[arrUnboxFloatCopy.length];
      for (int i = 0; i < arrUnboxFloatCopy.length; i++) {
        arrUnboxFloatConv[i] = arrUnboxFloatCopy[i];
      }
      builder.setPropertyDouble("arrUnboxFloat", arrUnboxFloatConv);
    }
    Boolean[] arrBoxBooleanCopy = document.getArrBoxBoolean();
    if (arrBoxBooleanCopy != null) {
      boolean[] arrBoxBooleanConv = new boolean[arrBoxBooleanCopy.length];
      for (int i = 0; i < arrBoxBooleanCopy.length; i++) {
        arrBoxBooleanConv[i] = arrBoxBooleanCopy[i];
      }
      builder.setPropertyBoolean("arrBoxBoolean", arrBoxBooleanConv);
    }
    boolean[] arrUnboxBooleanCopy = document.getArrUnboxBoolean();
    if (arrUnboxBooleanCopy != null) {
      builder.setPropertyBoolean("arrUnboxBoolean", arrUnboxBooleanCopy);
    }
    byte[][] arrUnboxByteArrCopy = document.getArrUnboxByteArr();
    if (arrUnboxByteArrCopy != null) {
      builder.setPropertyBytes("arrUnboxByteArr", arrUnboxByteArrCopy);
    }
    String[] arrStringCopy = document.getArrString();
    if (arrStringCopy != null) {
      builder.setPropertyString("arrString", arrStringCopy);
    }
    Gift[] arrGiftCopy = document.getArrGift();
    if (arrGiftCopy != null) {
      GenericDocument[] arrGiftConv = new GenericDocument[arrGiftCopy.length];
      for (int i = 0; i < arrGiftConv.length; i++) {
        arrGiftConv[i] = GenericDocument.fromDocumentClass(arrGiftCopy[i]);
      }
      builder.setPropertyDocument("arrGift", arrGiftConv);
    }
    EmbeddingVector[] arrVecCopy = document.getArrVec();
    if (arrVecCopy != null) {
      builder.setPropertyEmbedding("arrVec", arrVecCopy);
    }
    AppSearchBlobHandle[] arrBlobCopy = document.getArrBlob();
    if (arrBlobCopy != null) {
      builder.setPropertyBlobHandle("arrBlob", arrBlobCopy);
    }
    String stringCopy = document.getString();
    if (stringCopy != null) {
      builder.setPropertyString("string", stringCopy);
    }
    builder.setPropertyLong("nonNullLong", document.getNonNullLong());
    Long nullLongCopy = document.getNullLong();
    if (nullLongCopy != null) {
      builder.setPropertyLong("nullLong", nullLongCopy);
    }
    builder.setPropertyLong("nonNullInteger", document.getNonNullInteger());
    builder.setPropertyDouble("nonNullDouble", document.getNonNullDouble());
    builder.setPropertyDouble("nonNullFloat", document.getNonNullFloat());
    builder.setPropertyBoolean("nonNullBoolean", document.getNonNullBoolean());
    byte[] byteArrCopy = document.getByteArr();
    if (byteArrCopy != null) {
      builder.setPropertyBytes("byteArr", byteArrCopy);
    }
    Gift giftCopy = document.getGift();
    if (giftCopy != null) {
      GenericDocument giftConv = GenericDocument.fromDocumentClass(giftCopy);
      builder.setPropertyDocument("gift", giftConv);
    }
    Gift nullGiftCopy = document.getNullGift();
    if (nullGiftCopy != null) {
      GenericDocument nullGiftConv = GenericDocument.fromDocumentClass(nullGiftCopy);
      builder.setPropertyDocument("nullGift", nullGiftConv);
    }
    EmbeddingVector vecCopy = document.getVec();
    if (vecCopy != null) {
      builder.setPropertyEmbedding("vec", vecCopy);
    }
    AppSearchBlobHandle blobCopy = document.getBlob();
    if (blobCopy != null) {
      builder.setPropertyBlobHandle("blob", blobCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      DocumentClassMappingContext documentClassMappingContext) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    long[] collectLongCopy = genericDoc.getPropertyLongArray("collectLong");
    List<Long> collectLongConv = Collections.emptyList();
    if (collectLongCopy != null) {
      collectLongConv = new ArrayList<>(collectLongCopy.length);
      for (int i = 0; i < collectLongCopy.length; i++) {
        collectLongConv.add(collectLongCopy[i]);
      }
    }
    long[] collectIntegerCopy = genericDoc.getPropertyLongArray("collectInteger");
    List<Integer> collectIntegerConv = Collections.emptyList();
    if (collectIntegerCopy != null) {
      collectIntegerConv = new ArrayList<>(collectIntegerCopy.length);
      for (int i = 0; i < collectIntegerCopy.length; i++) {
        collectIntegerConv.add((int) collectIntegerCopy[i]);
      }
    }
    long[] collectNullIntegerCopy = genericDoc.getPropertyLongArray("collectNullInteger");
    List<Integer> collectNullIntegerConv = Collections.emptyList();
    if (collectNullIntegerCopy != null) {
      collectNullIntegerConv = new ArrayList<>(collectNullIntegerCopy.length);
      for (int i = 0; i < collectNullIntegerCopy.length; i++) {
        collectNullIntegerConv.add((int) collectNullIntegerCopy[i]);
      }
    }
    long[] nullCollectIntegerCopy = genericDoc.getPropertyLongArray("nullCollectInteger");
    List<Integer> nullCollectIntegerConv = null;
    if (nullCollectIntegerCopy != null) {
      nullCollectIntegerConv = new ArrayList<>(nullCollectIntegerCopy.length);
      for (int i = 0; i < nullCollectIntegerCopy.length; i++) {
        nullCollectIntegerConv.add((int) nullCollectIntegerCopy[i]);
      }
    }
    long[] nullCollectNullIntegerCopy = genericDoc.getPropertyLongArray("nullCollectNullInteger");
    List<Integer> nullCollectNullIntegerConv = null;
    if (nullCollectNullIntegerCopy != null) {
      nullCollectNullIntegerConv = new ArrayList<>(nullCollectNullIntegerCopy.length);
      for (int i = 0; i < nullCollectNullIntegerCopy.length; i++) {
        nullCollectNullIntegerConv.add((int) nullCollectNullIntegerCopy[i]);
      }
    }
    double[] collectDoubleCopy = genericDoc.getPropertyDoubleArray("collectDouble");
    List<Double> collectDoubleConv = Collections.emptyList();
    if (collectDoubleCopy != null) {
      collectDoubleConv = new ArrayList<>(collectDoubleCopy.length);
      for (int i = 0; i < collectDoubleCopy.length; i++) {
        collectDoubleConv.add(collectDoubleCopy[i]);
      }
    }
    double[] collectFloatCopy = genericDoc.getPropertyDoubleArray("collectFloat");
    List<Float> collectFloatConv = Collections.emptyList();
    if (collectFloatCopy != null) {
      collectFloatConv = new ArrayList<>(collectFloatCopy.length);
      for (int i = 0; i < collectFloatCopy.length; i++) {
        collectFloatConv.add((float) collectFloatCopy[i]);
      }
    }
    boolean[] collectBooleanCopy = genericDoc.getPropertyBooleanArray("collectBoolean");
    List<Boolean> collectBooleanConv = Collections.emptyList();
    if (collectBooleanCopy != null) {
      collectBooleanConv = new ArrayList<>(collectBooleanCopy.length);
      for (int i = 0; i < collectBooleanCopy.length; i++) {
        collectBooleanConv.add(collectBooleanCopy[i]);
      }
    }
    byte[][] collectByteArrCopy = genericDoc.getPropertyBytesArray("collectByteArr");
    List<byte[]> collectByteArrConv = Collections.emptyList();
    if (collectByteArrCopy != null) {
      collectByteArrConv = new ArrayList<>(collectByteArrCopy.length);
      for (int i = 0; i < collectByteArrCopy.length; i++) {
        collectByteArrConv.add(collectByteArrCopy[i]);
      }
    }
    String[] collectStringCopy = genericDoc.getPropertyStringArray("collectString");
    List<String> collectStringConv = Collections.emptyList();
    if (collectStringCopy != null) {
      collectStringConv = Arrays.asList(collectStringCopy);
    }
    GenericDocument[] collectGiftCopy = genericDoc.getPropertyDocumentArray("collectGift");
    List<Gift> collectGiftConv = Collections.emptyList();
    if (collectGiftCopy != null) {
      collectGiftConv = new ArrayList<>(collectGiftCopy.length);
      for (int i = 0; i < collectGiftCopy.length; i++) {
        collectGiftConv.add(collectGiftCopy[i].toDocumentClass(Gift.class, documentClassMappingContext));
      }
    }
    EmbeddingVector[] collectVecCopy = genericDoc.getPropertyEmbeddingArray("collectVec");
    List<EmbeddingVector> collectVecConv = Collections.emptyList();
    if (collectVecCopy != null) {
      collectVecConv = Arrays.asList(collectVecCopy);
    }
    AppSearchBlobHandle[] collectBlobCopy = genericDoc.getPropertyBlobHandleArray("collectBlob");
    List<AppSearchBlobHandle> collectBlobConv = Collections.emptyList();
    if (collectBlobCopy != null) {
      collectBlobConv = Arrays.asList(collectBlobCopy);
    }
    long[] arrBoxLongCopy = genericDoc.getPropertyLongArray("arrBoxLong");
    Long[] arrBoxLongConv = null;
    if (arrBoxLongCopy != null) {
      arrBoxLongConv = new Long[arrBoxLongCopy.length];
      for (int i = 0; i < arrBoxLongCopy.length; i++) {
        arrBoxLongConv[i] = arrBoxLongCopy[i];
      }
    }
    long[] arrUnboxLongConv = genericDoc.getPropertyLongArray("arrUnboxLong");
    long[] arrBoxIntegerCopy = genericDoc.getPropertyLongArray("arrBoxInteger");
    Integer[] arrBoxIntegerConv = null;
    if (arrBoxIntegerCopy != null) {
      arrBoxIntegerConv = new Integer[arrBoxIntegerCopy.length];
      for (int i = 0; i < arrBoxIntegerCopy.length; i++) {
        arrBoxIntegerConv[i] = (int) arrBoxIntegerCopy[i];
      }
    }
    long[] arrNullBoxIntegerCopy = genericDoc.getPropertyLongArray("arrNullBoxInteger");
    Integer[] arrNullBoxIntegerConv = null;
    if (arrNullBoxIntegerCopy != null) {
      arrNullBoxIntegerConv = new Integer[arrNullBoxIntegerCopy.length];
      for (int i = 0; i < arrNullBoxIntegerCopy.length; i++) {
        arrNullBoxIntegerConv[i] = (int) arrNullBoxIntegerCopy[i];
      }
    }
    long[] nullArrBoxIntegerCopy = genericDoc.getPropertyLongArray("nullArrBoxInteger");
    Integer[] nullArrBoxIntegerConv = null;
    if (nullArrBoxIntegerCopy != null) {
      nullArrBoxIntegerConv = new Integer[nullArrBoxIntegerCopy.length];
      for (int i = 0; i < nullArrBoxIntegerCopy.length; i++) {
        nullArrBoxIntegerConv[i] = (int) nullArrBoxIntegerCopy[i];
      }
    }
    long[] nullArrNullBoxIntegerCopy = genericDoc.getPropertyLongArray("nullArrNullBoxInteger");
    Integer[] nullArrNullBoxIntegerConv = null;
    if (nullArrNullBoxIntegerCopy != null) {
      nullArrNullBoxIntegerConv = new Integer[nullArrNullBoxIntegerCopy.length];
      for (int i = 0; i < nullArrNullBoxIntegerCopy.length; i++) {
        nullArrNullBoxIntegerConv[i] = (int) nullArrNullBoxIntegerCopy[i];
      }
    }
    long[] arrUnboxIntCopy = genericDoc.getPropertyLongArray("arrUnboxInt");
    int[] arrUnboxIntConv = null;
    if (arrUnboxIntCopy != null) {
      arrUnboxIntConv = new int[arrUnboxIntCopy.length];
      for (int i = 0; i < arrUnboxIntCopy.length; i++) {
        arrUnboxIntConv[i] = (int) arrUnboxIntCopy[i];
      }
    }
    double[] arrBoxDoubleCopy = genericDoc.getPropertyDoubleArray("arrBoxDouble");
    Double[] arrBoxDoubleConv = null;
    if (arrBoxDoubleCopy != null) {
      arrBoxDoubleConv = new Double[arrBoxDoubleCopy.length];
      for (int i = 0; i < arrBoxDoubleCopy.length; i++) {
        arrBoxDoubleConv[i] = arrBoxDoubleCopy[i];
      }
    }
    double[] arrUnboxDoubleConv = genericDoc.getPropertyDoubleArray("arrUnboxDouble");
    double[] arrBoxFloatCopy = genericDoc.getPropertyDoubleArray("arrBoxFloat");
    Float[] arrBoxFloatConv = null;
    if (arrBoxFloatCopy != null) {
      arrBoxFloatConv = new Float[arrBoxFloatCopy.length];
      for (int i = 0; i < arrBoxFloatCopy.length; i++) {
        arrBoxFloatConv[i] = (float) arrBoxFloatCopy[i];
      }
    }
    double[] arrUnboxFloatCopy = genericDoc.getPropertyDoubleArray("arrUnboxFloat");
    float[] arrUnboxFloatConv = null;
    if (arrUnboxFloatCopy != null) {
      arrUnboxFloatConv = new float[arrUnboxFloatCopy.length];
      for (int i = 0; i < arrUnboxFloatCopy.length; i++) {
        arrUnboxFloatConv[i] = (float) arrUnboxFloatCopy[i];
      }
    }
    boolean[] arrBoxBooleanCopy = genericDoc.getPropertyBooleanArray("arrBoxBoolean");
    Boolean[] arrBoxBooleanConv = null;
    if (arrBoxBooleanCopy != null) {
      arrBoxBooleanConv = new Boolean[arrBoxBooleanCopy.length];
      for (int i = 0; i < arrBoxBooleanCopy.length; i++) {
        arrBoxBooleanConv[i] = arrBoxBooleanCopy[i];
      }
    }
    boolean[] arrUnboxBooleanConv = genericDoc.getPropertyBooleanArray("arrUnboxBoolean");
    byte[][] arrUnboxByteArrConv = genericDoc.getPropertyBytesArray("arrUnboxByteArr");
    String[] arrStringConv = genericDoc.getPropertyStringArray("arrString");
    GenericDocument[] arrGiftCopy = genericDoc.getPropertyDocumentArray("arrGift");
    Gift[] arrGiftConv = null;
    if (arrGiftCopy != null) {
      arrGiftConv = new Gift[arrGiftCopy.length];
      for (int i = 0; i < arrGiftCopy.length; i++) {
        arrGiftConv[i] = arrGiftCopy[i].toDocumentClass(Gift.class, documentClassMappingContext);
      }
    }
    EmbeddingVector[] arrVecConv = genericDoc.getPropertyEmbeddingArray("arrVec");
    AppSearchBlobHandle[] arrBlobConv = genericDoc.getPropertyBlobHandleArray("arrBlob");
    String[] stringCopy = genericDoc.getPropertyStringArray("string");
    String stringConv = null;
    if (stringCopy != null && stringCopy.length != 0) {
      stringConv = stringCopy[0];
    }
    long nonNullLongConv = genericDoc.getPropertyLong("nonNullLong");
    long[] nullLongCopy = genericDoc.getPropertyLongArray("nullLong");
    Long nullLongConv = null;
    if (nullLongCopy != null && nullLongCopy.length != 0) {
      nullLongConv = nullLongCopy[0];
    }
    int nonNullIntegerConv = (int) genericDoc.getPropertyLong("nonNullInteger");
    double nonNullDoubleConv = genericDoc.getPropertyDouble("nonNullDouble");
    float nonNullFloatConv = (float) genericDoc.getPropertyDouble("nonNullFloat");
    boolean nonNullBooleanConv = genericDoc.getPropertyBoolean("nonNullBoolean");
    byte[][] byteArrCopy = genericDoc.getPropertyBytesArray("byteArr");
    byte[] byteArrConv = null;
    if (byteArrCopy != null && byteArrCopy.length != 0) {
      byteArrConv = byteArrCopy[0];
    }
    GenericDocument giftCopy = genericDoc.getPropertyDocument("gift");
    Gift giftConv = null;
    if (giftCopy != null) {
      giftConv = giftCopy.toDocumentClass(Gift.class, documentClassMappingContext);
    }
    GenericDocument nullGiftCopy = genericDoc.getPropertyDocument("nullGift");
    Gift nullGiftConv = null;
    if (nullGiftCopy != null) {
      nullGiftConv = nullGiftCopy.toDocumentClass(Gift.class, documentClassMappingContext);
    }
    EmbeddingVector[] vecCopy = genericDoc.getPropertyEmbeddingArray("vec");
    EmbeddingVector vecConv = null;
    if (vecCopy != null && vecCopy.length != 0) {
      vecConv = vecCopy[0];
    }
    AppSearchBlobHandle[] blobCopy = genericDoc.getPropertyBlobHandleArray("blob");
    AppSearchBlobHandle blobConv = null;
    if (blobCopy != null && blobCopy.length != 0) {
      blobConv = blobCopy[0];
    }
    Gift document = new Gift(namespaceConv, idConv, collectLongConv, collectIntegerConv, collectNullIntegerConv, nullCollectIntegerConv, nullCollectNullIntegerConv, collectDoubleConv, collectFloatConv, collectBooleanConv, collectByteArrConv, collectStringConv, collectGiftConv, collectVecConv, collectBlobConv, arrBoxLongConv, arrUnboxLongConv, arrBoxIntegerConv, arrNullBoxIntegerConv, nullArrBoxIntegerConv, nullArrNullBoxIntegerConv, arrUnboxIntConv, arrBoxDoubleConv, arrUnboxDoubleConv, arrBoxFloatConv, arrUnboxFloatConv, arrBoxBooleanConv, arrUnboxBooleanConv, arrUnboxByteArrConv, arrStringConv, arrGiftConv, arrVecConv, arrBlobConv, stringConv, nonNullLongConv, nullLongConv, nonNullIntegerConv, nonNullDoubleConv, nonNullFloatConv, nonNullBooleanConv, byteArrConv, giftConv, nullGiftConv, vecConv, blobConv);
    return document;
  }
}
