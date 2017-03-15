package com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.business;

import android.app.Application;
import android.os.AsyncTask;

import com.example.android.flatfoot.codelab.flatfootcodelab.step10.Comment;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10.Product;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10.ProductReviewService;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10.ProductReviewServiceFactory;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.db.MyDatabase;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyComment;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyProduct;

import java.util.ArrayList;
import java.util.List;

public class MyAppLogic {
    private static MyAppLogic sInstance;
    private static final Object LOCK = new Object();
    public static MyAppLogic getInstance(Application application) {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new MyAppLogic(application);
                }
            }
        }
        return sInstance;
    }

    MyDatabase mDatabase;
    ProductReviewService mProductReviewService;
    public MyAppLogic(Application application) {
        mDatabase = MyDatabase.getInstance(application);
        mProductReviewService = ProductReviewServiceFactory.get();
    }

    public void fetchProducts() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<Product> products = mProductReviewService.fetchProducts();
                List<MyProduct> converted = new ArrayList<>();
                for(Product input : products) {
                    converted.add(new MyProduct(input));
                }
                mDatabase.productDao().insertAll(converted);
                return null;
            }
        }.execute();
    }

    public void fetchComments(final int productId) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                List<Comment> comments = mProductReviewService.fetchComments(productId);
                List<MyComment> converted = new ArrayList<>();
                for (Comment comment : comments) {
                    converted.add(new MyComment(comment));
                }
                mDatabase.commentDao().insertAll(converted);
                return null;
            }
        }.execute();
    }
}
