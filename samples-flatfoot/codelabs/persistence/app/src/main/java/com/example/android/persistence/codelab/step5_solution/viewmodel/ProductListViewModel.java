package com.example.android.persistence.codelab.step5_solution.viewmodel;

import android.app.Application;

import com.android.support.lifecycle.AndroidViewModel;
import com.android.support.lifecycle.LiveData;
import com.example.android.persistence.codelab.step5_solution.business.MyAppLogic;
import com.example.android.persistence.codelab.step5_solution.db.MyDatabase;
import com.example.android.persistence.codelab.step5_solution.entity.MyProduct;

import java.util.List;

public class ProductListViewModel extends AndroidViewModel {

    private LiveData<List<MyProduct>> mProducts;

    public ProductListViewModel(Application application) {
        super(application);
    }


    public LiveData<List<MyProduct>> getProducts() {
        if (mProducts == null) {
            mProducts = MyDatabase.getInstance(this.getApplication()).productDao()
                    .loadAllProducts();
            MyAppLogic.getInstance(this.getApplication()).fetchProducts();
        }
        return mProducts;
    }
}
