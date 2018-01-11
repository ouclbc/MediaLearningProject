package com.ouclbc.medialearningproject.fragment;


import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.ouclbc.medialearningproject.util.BackHandledInterface;

/**
 * Created by libaocheng on 2018-1-2.
 */

public abstract class BackHandledFragment extends Fragment {
    protected BackHandledInterface mBackHandledInterface;
    public abstract boolean onBackPressed();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!(getActivity() instanceof BackHandledInterface)){
            throw new ClassCastException("Hosting Activity must implement BackHandledInterface");
        }else{
            this.mBackHandledInterface = (BackHandledInterface)getActivity();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        mBackHandledInterface.setSelectedFragment(this);
    }
}
