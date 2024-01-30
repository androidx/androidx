package com.example.appsearch;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchDocumentClassMap;
import com.google.auto.service.AutoService;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@AutoService(AppSearchDocumentClassMap.class)
@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__DocumentClassMap_fc605c65514dc62366bce05ca8e3a0a22e13d867acb0e63a098ee18dbd2de01e_0 extends AppSearchDocumentClassMap {
  @NonNull
  @Override
  protected Map<String, List<String>> getMap() {
    Map<String, List<String>> result = new HashMap<>();
    result.put("Gift", Arrays.asList("com.example.appsearch.Gift"));
    result.put("Root", Arrays.asList("com.example.appsearch.Root"));
    result.put("Parent2", Arrays.asList("com.example.appsearch.Parent2"));
    result.put("Parent1", Arrays.asList("com.example.appsearch.Parent1"));
    return result;
  }
}
