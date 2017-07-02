package com.xin.bob.downloadservicebestpractice.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.xin.bob.downloadservicebestpractice.MainActivity;
import com.xin.bob.downloadservicebestpractice.R;
import com.xin.bob.downloadservicebestpractice.task.DownloadListener;
import com.xin.bob.downloadservicebestpractice.task.DownloadTask;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask mDownloadTask;
    private String downloadUrl;

    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onProgress(int progress) {
            getNotificationManager().notify(1, getNotification("Downloading...", progress));
        }

        @Override
        public void onSuccess() {
            // 清空任务实例
            mDownloadTask = null;
            // 停止前台服务 并移除Notification
            stopForeground(true);
            // 通知栏刷新UI
            getNotificationManager().notify(1, getNotification("Download Success", -1));
            // Toast
            Toast.makeText(DownloadService.this, "Download Success", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onFailed() {
            mDownloadTask = null;
            stopForeground(true);
            getNotificationManager().notify(1, getNotification("Download Failed", -1));
            Toast.makeText(DownloadService.this, "Download Failed", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPaused() {
            mDownloadTask = null;
            Toast.makeText(DownloadService.this, "Paused", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancelled() {
            mDownloadTask = null;
            stopForeground(true);
            Toast.makeText(DownloadService.this, "Cancelled", Toast.LENGTH_SHORT).show();
        }
    };


    private DownloadBinder mBinder = new DownloadBinder();

    /**
     * DownloadBinder class
     */
    public class DownloadBinder extends Binder {

        /**
         * 获取service实例
         *
         * @return DownloadService instance
         */
        public DownloadService getService() {
            return DownloadService.this;
        }

        /**
         * 开始下载
         *
         * @param downloadUrl url
         */
        public void startDownload(String downloadUrl) {
            if (mDownloadTask == null) {
                mDownloadTask = null;
                // create DownloadTask instance
                mDownloadTask = new DownloadTask(listener);
                mDownloadTask.execute(downloadUrl); // execute

                startForeground(1, getNotification("Downloading...", 0));
                Toast.makeText(DownloadService.this, "Downloading...", Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 暂停下载
         */
        public void pauseDownload() {
            if (mDownloadTask != null) {
                mDownloadTask.pauseDownload();
            }
        }

        /**
         * 取消下载
         */
        public void cancelDownload() {
            if (mDownloadTask != null) {
                mDownloadTask.cancelDownload();
            } else {
                if (downloadUrl != null) {
                    // 取消下载时 需将已下载形成文件的部分删除 并移除当前展示出的通知
                    // 1. 删除未下载完的文件
                    String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
                    String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                    File file = new File(directory + fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    // 2. 移除通知
                    getNotificationManager().cancel(1);
                    // 3. 停止前台服务
                    stopForeground(true);

                    Toast.makeText(DownloadService.this, "Canceled", Toast.LENGTH_SHORT).show();
                }
            }
        }

    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 获取 NotificationManager 实例
     *
     * @return NotificationManager instance
     */
    private NotificationManager getNotificationManager() {
        return (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * 创建 Notification通知实例
     *
     * @param title    notification content title
     * @param progress current download progress
     * @return notification instance
     */
    private Notification getNotification(String title, int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0));

        if (progress > 0) {
            builder.setContentText(progress + "%");
            builder.setProgress(100, progress, false); // 显示出进度条在通知上
        }
        return builder.build();
    }

}
