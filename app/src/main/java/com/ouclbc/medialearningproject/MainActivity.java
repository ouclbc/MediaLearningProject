package com.ouclbc.medialearningproject;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ouclbc.medialearningproject.Camera.Camera2Activity;
import com.ouclbc.medialearningproject.Camera.Camera2SurfaceViewFragment;
import com.ouclbc.medialearningproject.Camera.Camera2VideoFragment;
import com.ouclbc.medialearningproject.fragment.AudioEncoderFragment;
import com.ouclbc.medialearningproject.fragment.BackHandledFragment;
import com.ouclbc.medialearningproject.fragment.ItemFragment;
import com.ouclbc.medialearningproject.fragment.XplayerFragment;
import com.ouclbc.medialearningproject.fragment.dummy.DummyContent;
import com.ouclbc.medialearningproject.util.BackHandledInterface;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements ItemFragment.OnListFragmentInteractionListener,EasyPermissions.PermissionCallbacks,
        BackHandledInterface {

    private static final String TAG = "lbc";
    private static final int RC_CAMERA_AND_STORAGE = 123;
    private Context mContext;
    private ItemFragment mDemolistFragment;
    private FrameLayout mContainer;
    private BackHandledFragment mBackHandedFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mContainer = findViewById(R.id.id_content);
        methodRequiresPermission();
        setMainFragment();
    }
    private void methodRequiresPermission() {
        String[] perms = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // Already have permission, do the thing
            // ...
        } else {
            // Do not have permissions, request them now
            EasyPermissions.requestPermissions(this, getString(R.string.camera_and_storage),
                    RC_CAMERA_AND_STORAGE, perms);
        }
    }

    private void setMainFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        mDemolistFragment = ItemFragment.newInstance(1);
        transaction.replace(R.id.id_content, mDemolistFragment);
        transaction.commit();
    }

    private void setMediaCodecAACFragment() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        AudioEncoderFragment audioEncoderFragment = AudioEncoderFragment.newInstance("1","2");
        transaction.replace(R.id.id_content, audioEncoderFragment);
        transaction.commit();
    }

    /**
     *
     */
    private void setCameraFragment(){
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.id_content, Camera2VideoFragment.newInstance())
                .commit();
    }

    @Override
    public void onListFragmentInteraction(DummyContent.DummyItem item) {
        Toast.makeText(mContext,item.id+"click",Toast.LENGTH_LONG).show();
        int position = Integer.parseInt(item.id);
        switch (position){
            case 1:
                break;
            case 2:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.id_content, Camera2SurfaceViewFragment.newInstance())
                        .commit();
                break;
            case 3:
                break;
            case 4:
                setMediaCodecAACFragment();
                break;
            case 5:
                if (hasCameraSupport()){
                    setCameraFragment();
                }else{
                    Toast.makeText(mContext,"本机没有摄像头",Toast.LENGTH_LONG).show();
                }
                break;
            case 6:
                break;
            case 7:
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.id_content, XplayerFragment.newInstance(null,
                                "/mnt/sdcard/test.mp4"))
                        .commit();
                break;
                default:
                    break;
        }
    }

    @Override
    public void setSelectedFragment(BackHandledFragment selectedFragment) {
        this.mBackHandedFragment = selectedFragment;
    }
    @Override
    public void onBackPressed() {
        if(mBackHandedFragment == null || !mBackHandedFragment.onBackPressed()){
            if(getSupportFragmentManager().getBackStackEntryCount() == 0){
                super.onBackPressed();
            }else{
                getSupportFragmentManager().popBackStack();
            }
        }else {
            if (!(mBackHandedFragment instanceof ItemFragment)){
                setMainFragment();
            }
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(this, R.string.camera_and_storage, Toast.LENGTH_SHORT)
                .show();
    }
    private boolean hasCameraSupport() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }
}
