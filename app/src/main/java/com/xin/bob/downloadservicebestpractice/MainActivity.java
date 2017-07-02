package com.xin.bob.downloadservicebestpractice;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.xin.bob.downloadservicebestpractice.service.DownloadService;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private boolean mBound = false;
    private DownloadService.DownloadBinder downloadBinder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();

        // 启动服务
        startService(new Intent(this, DownloadService.class));
        // Activity bind DownloadService
        bindService(new Intent(this, DownloadService.class), conn, Context.BIND_AUTO_CREATE);

        // Check permission
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    private void initViews() {
        Button btnStart = (Button) findViewById(R.id.btn_start_download);
        Button btnPause = (Button) findViewById(R.id.btn_pause_download);
        Button btnCancel = (Button) findViewById(R.id.btn_cancel_download);
        btnStart.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnCancel.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "App have to grant that permission", Toast.LENGTH_SHORT).show();
                    MainActivity.this.finish();
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        if (!mBound) return;

        switch (view.getId()) {
            case R.id.btn_start_download:
                // final String url = "https://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
                final String url = "https://nodejs.org/dist/v8.1.3/node-v8.1.3-x64.msi";
                downloadBinder.startDownload(url);
                break;

            case R.id.btn_pause_download:
                downloadBinder.pauseDownload();
                break;

            case R.id.btn_cancel_download:
                downloadBinder.cancelDownload();
                break;
        }
    }

    // ServiceConnection instance
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            downloadBinder = (DownloadService.DownloadBinder) iBinder;
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            downloadBinder = null;
            mBound = false;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }
}
