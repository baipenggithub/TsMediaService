package com.ts.service.media;

import android.os.Bundle;
import android.os.RemoteException;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ts.sdk.media.contractinterface.IMediaServiceListener;
import com.ts.sdk.media.mananger.BaseManager;
import com.ts.sdk.media.mananger.BtMusicManager;
import com.ts.service.R;

public class DialogActivity extends FragmentActivity {

    public static final String TAG = "BT_Music_Dialog";
    private static final int ANIMATION_DEFAULT = 0;
    private BtMusicManager mBtMusicManager;
    private Boolean mBtMusicConnected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        setContentView(R.layout.activity_dialog);
        mBtMusicManager = BtMusicManager.getInstance(this, new IMediaServiceListener() {
            @Override
            public void onServiceConnected(BaseManager manager) {
                mBtMusicConnected = true;
            }

            @Override
            public void onServiceDisconnected() {
                mBtMusicConnected = false;
            }
        });
        showMaterialDialog();
    }

    private void showMaterialDialog() {
        new MaterialAlertDialogBuilder(this)
                .setCancelable(true)
                .setOnDismissListener(dialog -> finish())
                .setMessage(getResources().getString(R.string.switch_bt_music))
                .setCancelable(false)
                .setNegativeButton("取消", (dialog, which) -> {
                    if (mBtMusicConnected) {
                        try {
                            mBtMusicManager.pause();
                        } catch (RemoteException exception) {
                            exception.printStackTrace();
                        }
                    }
                })
                .setPositiveButton("确定", (dialog, which) -> {
                    if (mBtMusicConnected) {
                        try {
                            mBtMusicManager.play();
                        } catch (RemoteException exception) {
                            exception.printStackTrace();
                        }
                    }
                })
                .show();
    }

    @Override
    protected void onPause() {
        overridePendingTransition(ANIMATION_DEFAULT, ANIMATION_DEFAULT);
        super.onPause();
    }
}
