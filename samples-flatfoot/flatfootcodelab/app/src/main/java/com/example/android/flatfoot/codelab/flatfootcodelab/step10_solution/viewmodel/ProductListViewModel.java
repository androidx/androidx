package com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.viewmodel;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.business.MyAppLogic;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.db.MyDatabase;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyProduct;

import java.util.List;

public class ProductListViewModel extends ViewModel {
    private LiveData<List<MyProduct>> mProducts;
    public ProductListViewModel() {
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
