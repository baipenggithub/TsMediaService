package com.ts.service.media;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ts.service.R;
import com.ts.service.media.utils.LogUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity {

    private static final String[] PERMISSIONS = {
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private static final int REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ArrayList<String> noPermissions = getNeedRequestPermission(PERMISSIONS);
        if (noPermissions.size() == 0) {
            LogUtil.debug("MainActivity", "已有权限=====");
        } else {
            LogUtil.debug("MainActivity", "申请权限=====");
            initPermission();
        }
    }

    private void initPermission() {
        List<String> listNonPermissions = new ArrayList<>();
        for (String type : PERMISSIONS) {
            if (checkSelfPermission(type) != PackageManager.PERMISSION_GRANTED) {
                listNonPermissions.add(type);
            }
        }
        if (listNonPermissions.size() > 0) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE);
        }
    }

    private ArrayList<String> getNeedRequestPermission(String[] permission) {
        ArrayList<String> needRequestPermission = new ArrayList<>();
        for (String s : permission) {
            if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) {
                needRequestPermission.add(s);
            }
        }
        return needRequestPermission;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("MainActivity", "申请权限成功回调---" + requestCode + " ,grantResults:" + Arrays.toString(grantResults));
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // 用户已授予权限，执行您的逻辑
            LogUtil.debug("MainActivity", "位置权限已赋予");
        } else {
            // 用户拒绝了权限，您可以在这里处理拒绝权限的逻辑
            LogUtil.debug("MainActivity", "位置权限已拒绝");
        }
    }
}
