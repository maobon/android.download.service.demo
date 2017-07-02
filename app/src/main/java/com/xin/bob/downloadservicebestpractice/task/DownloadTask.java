package com.xin.bob.downloadservicebestpractice.task;

import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by bob on 2017/6/30.
 * Download file from network use with AndroidOS AsyncTask and OKHttp framework
 */

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;
    public static final int TYPE_PAUSED = 2;
    public static final int TYPE_CANCELED = 3;

    private DownloadListener listener;
    private boolean isCanceled = false;
    private boolean isPaused = false;

    public DownloadTask(DownloadListener listener) {
        this.listener = listener;
    }

    public void setListener(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        InputStream in = null;
        RandomAccessFile savedFile = null;
        File file = null;

        long downloadLen = 0;

        try {
            // target url
            String downloadUrl = params[0];
            // 从url中截取获得文件名称
            String fileName = downloadUrl.substring(downloadUrl.lastIndexOf("/"));
            // 下载文件存储文件夹地址 Environment.DIRECTORY_DOWNLOADS SD卡上的默认download文件夹
            String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();

            // create download file instance
            file = new File(directory + fileName);
            if (file.exists()) {
                // 如果文件存在 获取文件内容长度
                downloadLen = file.length();
            }

            // 联网首先请求目标地址 获得目标文件的长度
            // request target file content length compare with current local file content len
            long contentLen = getContentLen(downloadUrl);
            if (contentLen == 0) {
                // network failed
                return TYPE_FAILED;
            } else if (contentLen == downloadLen) {
                // already download success
                return TYPE_SUCCESS;
            }

            // 执行下载
            OkHttpClient client = new OkHttpClient();
            Request downloadReq = new Request.Builder()
                    .addHeader("RANGE", "bytes=" + downloadLen + "-") // 发送设定断点的位置
                    .url(downloadUrl)
                    .build();
            // request server
            Response res = client.newCall(downloadReq).execute();

            if (res != null) {
                // InputStream in = res.body().byteStream()
                // 使用InputStream来接受读取出的内容
                in = res.body().byteStream();

                // RandomAccessFile Java IO 文件工具类 操作读写文件
                // seek jump already download part
                // support breakpoint download
                savedFile = new RandomAccessFile(file, "rw");
                savedFile.seek(downloadLen); // 指针跳过已经下载好的部分

                // JAVA IO 经典操作
                // 读取InputStream输入流中的内容
                // RandomAccessFile 进行写入
                byte[] b = new byte[1024]; // buffer

                int total = 0;
                int len;

                while ((len = in.read(b)) != -1) {
                    if (isCanceled) {
                        return TYPE_CANCELED;

                    } else if (isPaused) {
                        return TYPE_PAUSED;

                    } else {
                        total += len;

                        savedFile.write(b, 0, len); // 真正写入本地文件的语句

                        // (total + downloadLen) / contentLen = 0.65 扩大100倍 并取整
                        int progress = (int) ((total + downloadLen) * 100 / contentLen);

                        publishProgress(progress); // 发布
                    }
                }
                // 跳出while循环时 写入本地文件的操作就完成了 下载任务完成

                res.body().close();
                return TYPE_SUCCESS;
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (savedFile != null) {
                    savedFile.close();
                }
                if (isCanceled && file != null) {
                    file.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return TYPE_FAILED;
    }

    private int lastProgress;

    @Override
    protected void onProgressUpdate(Integer... values) {
        int progress = values[0];

        if (progress > lastProgress) {
            listener.onProgress(progress);
            lastProgress = progress;
        }
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status) {
            case TYPE_SUCCESS:
                listener.onSuccess();
                break;
            case TYPE_FAILED:
                listener.onFailed();
                break;
            case TYPE_PAUSED:
                listener.onPaused();
                break;
            case TYPE_CANCELED:
                listener.onCancelled();
                break;
        }
    }

    // 暂停和取消任务 是通过修改布尔型标记控制的

    /**
     * 暂停任务
     */
    public void pauseDownload() {
        isPaused = true;
    }

    /**
     * 取消任务
     */
    public void cancelDownload() {
        isCanceled = true;
    }


    /**
     * 获取目标下载文件的长度(大小)
     * 获取服务端返回ContentLength字段的内容
     *
     * @param downloadUrl String target url
     * @return target file content length
     * @throws IOException throws to parent method
     */
    private long getContentLen(String downloadUrl) throws IOException {
        // use OKHttp framework
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(downloadUrl)
                .build();

        // execute request
        Response response = client.newCall(request).execute();

        // get target file content length
        if (response != null && response.isSuccessful()) {
            long contentLength = response.body().contentLength();
            response.close();
            return contentLength;
        }

        return 0;
    }

}
