package com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.support.lifecycle.Lifecycle;
import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelStore;

import com.example.android.flatfoot.codelab.flatfootcodelab.step10.Product;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10.ui.ProductClickCallback;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10.ui.ProductAdapter;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.entity.MyProduct;
import com.example.android.flatfoot.codelab.flatfootcodelab.step10_solution.viewmodel
        .ProductListViewModel;

import java.util.List;

public class ProductListFragment extends LifecycleFragment {
    ProductAdapter productListAdapter;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(inflater.getContext());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        productListAdapter = new ProductAdapter(mProductClickCallback);
        recyclerView.setAdapter(productListAdapter);
        return recyclerView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ProductListViewModel viewModel = ViewModelStore.get(this, ProductListViewModel.class);
        viewModel.getProducts().observe(this, new Observer<List<MyProduct>>() {
            @Override
            public void onChanged(@Nullable List<MyProduct> myProducts) {
                productListAdapter.setProductList(myProducts);
            }
        });
    }

    ProductClickCallback mProductClickCallback = new ProductClickCallback() {
        @Override
        public void onClick(Product product) {
            if (getLifecycle().getCurrentState() >= Lifecycle.STARTED) {
                ((ProductReviewActivity) getActivity()).show(product);
            }
        }
    };
}
