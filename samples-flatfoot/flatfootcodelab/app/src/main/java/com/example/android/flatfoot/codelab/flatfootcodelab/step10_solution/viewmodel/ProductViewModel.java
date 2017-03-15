package com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.viewmodel;

import android.support.annotation.NonNull;

import com.android.support.lifecycle.LiveData;
import com.android.support.lifecycle.ViewModel;

import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.business.MyAppLogic;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.db.MyDatabase;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyComment;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyProduct;

import java.util.List;

public class ProductViewModel extends ViewModel {
    private LiveData<MyProduct> mProduct = new LiveData<>();
    private LiveData<List<MyComment>> mComments = new LiveData<>();

    private int productId;
    public void setProductId(int productId) {
        if (this.productId == productId) {
            return;
        }
        this.productId = productId;
        MyAppLogic.getInstance(this.getApplication()).fetchComments(productId);
        MyDatabase db = MyDatabase.getInstance(this.getApplication());
        this.mProduct = db.productDao().loadProduct(productId);
        this.mComments = db.commentDao().loadComments(productId);
    }

    @NonNull
    public LiveData<MyProduct> getProduct() {
        return mProduct;
    }

    @NonNull
    public LiveData<List<MyComment>> getComments() {
        return mComments;
    }
}
