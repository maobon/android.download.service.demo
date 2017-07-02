package com.xin.bob.downloadservicebestpractice.task;

/**
 * Created by bob on 2017/6/30.
 * listener Download task status and callback at right time
 */

public interface DownloadListener {

    void onProgress(int progress);

    void onSuccess();

    void onFailed();

    void onPaused();

    void onCancelled();

}
