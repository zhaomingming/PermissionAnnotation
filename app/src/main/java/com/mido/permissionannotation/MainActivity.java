package com.mido.permissionannotation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.mido.libannotation.PermissionDenied;
import com.mido.libannotation.PermissionGrant;
import com.mido.libannotation.PermissionRational;
import com.mido.libpermissionhelper.PermissionHelper;
import com.mido.libpermissionhelper.PermissionRationalCallback;

public class MainActivity extends ComponentActivity {

    private static final String TAG = "zhaoming";
    private static final int RESULT_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 100;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        init();
    }

    private void init() {
        int pid = Process.myPid();
        Log.d(TAG, "myPid: " + pid);
//        buyGoodsWithProxy();
//        if (ActivityCompat.checkSelfPermission(
//                this, android.Manifest.permission.CAMERA)
//                != PackageManager.PERMISSION_GRANTED) {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(
//                    this, android.Manifest.permission.CAMERA)) {
//                //dialog
//            }else {
//                ActivityCompat.requestPermissions(
//                        this, new String[]{Manifest.permission.CAMERA},
//                        RESULT_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
//            }
//        }
        String[] permissions = new String[] {Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.WRITE_CONTACTS, Manifest.permission.READ_PHONE_STATE};
        PermissionHelper.requestPermissions(
                this, permissions, RESULT_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
    }

    @PermissionGrant(RESULT_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)
    public void onRequestCameraPermissionGranted(String[] grantPermissions) {
        Toast.makeText(this, "Camera权限申请成功", Toast.LENGTH_SHORT).show();
    }

    @PermissionDenied(RESULT_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)
    public void onRequestCameraPermissionDenied(String[] deniedPermissions) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String permission: deniedPermissions) {
            stringBuilder.append(permission).append("\n");
        }
        new AlertDialog.Builder(this).setTitle("权限授权提示")
                .setMessage("很遗憾，以下权限被拒绝了，功能将无法继续使用\n\n" + stringBuilder)
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create().show();
    }

    @PermissionRational(RESULT_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE)
    public void onRequestCameraPermissionRational(String[] deniedPermissions, PermissionRationalCallback callback) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String permission: deniedPermissions) {
            stringBuilder.append(permission).append("\n");
        }
        new AlertDialog.Builder(this).setTitle("权限授权提示")
                .setMessage("请授予以下权限，以继续使用功能\n\n" + stringBuilder)
                .setPositiveButton("好的", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (callback != null) {
                            Log.d(TAG, "callback: " + callback);
                            callback.onRationalExecute();
                        }
                    }
                }).create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        PermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults, deviceId);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);
    }
}
