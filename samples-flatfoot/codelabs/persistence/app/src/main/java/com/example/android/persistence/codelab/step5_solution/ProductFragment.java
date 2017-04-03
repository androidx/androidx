package com.example.android.persistence.codelab.step5_solution;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.support.lifecycle.LifecycleFragment;
import com.android.support.lifecycle.Observer;
import com.android.support.lifecycle.ViewModelProviders;
import com.example.android.codelabs.persistence.R;
import com.example.android.codelabs.persistence.databinding.ProductFragmentBinding;
import com.example.android.persistence.codelab.step5.Comment;
import com.example.android.persistence.codelab.step5.ui.CommentAdapter;
import com.example.android.persistence.codelab.step5.ui.CommentClickCallback;
import com.example.android.persistence.codelab.step5_solution.entity.MyComment;
import com.example.android.persistence.codelab.step5_solution.entity.MyProduct;
import com.example.android.persistence.codelab.step5_solution.viewmodel.ProductViewModel;

import java.util.List;

public class ProductFragment extends LifecycleFragment {
    ProductFragmentBinding mBinding;
    CommentAdapter mCommentAdapter;
    private static final String KEY_PRODUCT_ID = "product_id";
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.product_fragment, container, false);
        mCommentAdapter = new CommentAdapter(mCommentClickCallback);
        mBinding.commentList.setAdapter(mCommentAdapter);
        return mBinding.getRoot();
    }

    private CommentClickCallback mCommentClickCallback = new CommentClickCallback() {
        @Override
        public void onClick(Comment comment) {
            // TODO
        }
    };

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ProductViewModel model = ViewModelProviders.of(this).get(ProductViewModel.class);
        model.setProductId(getArguments().getInt(KEY_PRODUCT_ID));
        model.getProduct().observe(this, new Observer<MyProduct>() {
            @Override
            public void onChanged(@Nullable MyProduct myProduct) {
                mBinding.setProduct(myProduct);
            }
        });
        model.getComments().observe(this, new Observer<List<MyComment>>() {
            @Override
            public void onChanged(@Nullable List<MyComment> myComments) {
                mCommentAdapter.setCommentList(myComments);
            }
        });
    }

    public static ProductFragment forProduct(int productId) {
        ProductFragment fragment = new ProductFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_PRODUCT_ID, productId);
        fragment.setArguments(args);
        return fragment;
    }
}
