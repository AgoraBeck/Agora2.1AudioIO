package com.agora.io.audioGather;
import android.os.Environment;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;

import java.io.InputStream;

public class AudioImpl implements IAudioController {
    private final static int mSendPeriod = 20;

    private AudioRecord mAudioRecorder = null;
    private IAudioCallback callback = null;

    private AudioStatus mStatus = AudioStatus.STOPPED;

    private byte[] mAudioBuffer = null;

    private InputStream in = null;
    private int byteread = 0;
    private int sizeInBytes = 0;
    private static final String  TAG = "SefAudioRecord";

    public AudioImpl(int samplingRate, int channelConfig){
        if (mStatus == AudioStatus.STOPPED) {
            int val = 0;
            if (1 == channelConfig)
                val = AudioFormat.CHANNEL_IN_MONO;
            else if(2 == channelConfig)
                val = AudioFormat.CHANNEL_IN_STEREO;
            else
                Log.e(TAG, "channelConfig is error !");

            // Double the size for much safer
            sizeInBytes = AudioRecord.getMinBufferSize(samplingRate, val, AudioFormat.ENCODING_PCM_16BIT) * 2;
            Log.e(TAG, "sizeInBytes: " + sizeInBytes);

            if (mAudioRecorder != null) {
                mAudioRecorder.release();
                mAudioRecorder = null;
            }

            mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    samplingRate,
                    val,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeInBytes);
            if(mAudioRecorder == null)
                Log.e(TAG, "mAudioRecorder is null ");
            mStatus = AudioStatus.INITIALISING;
        }
    }

    @Override
    public AudioStatus init(IAudioCallback callback) {
        if (mStatus == AudioStatus.INITIALISING) {
            this.callback = callback;
        }

//        try {
//            in = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sugar_16k.pcm");
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }

        return mStatus;
    }

    @Override
    public AudioStatus start() {
        if (mStatus == AudioStatus.INITIALISING) {

            if (mAudioBuffer == null)
                mAudioBuffer = new byte[sizeInBytes];
                

            mAudioRecorder.startRecording();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    gatherData();
                }
            }).start();
            mStatus = AudioStatus.RUNNING;
        }
        return mStatus;
    }

    @Override
    public AudioStatus stop() {
        mStatus = AudioStatus.INITIALISING;
        if (null != mAudioRecorder){
            mAudioRecorder.stop();
            mAudioRecorder.release();
            mAudioBuffer = null;
            mAudioRecorder = null;
        }
        return mStatus;
    }

    @Override
    public void destroy() {
        if (mStatus == AudioStatus.INITIALISING) {
            mStatus = AudioStatus.STOPPED;
        }
    }

    private void gatherData() {
        while (mStatus == AudioStatus.RUNNING) {
            int read = mAudioRecorder.read(mAudioBuffer, 0, sizeInBytes);
            if (read != sizeInBytes){
                Log.e(TAG,"== onAudioDataAvailable ==");
            }
            if (mAudioBuffer != null)
                callback.onAudioDataAvailable(System.currentTimeMillis(), mAudioBuffer);
        }

//       try {
//            while ((byteread = in.read(mAudioBuffer)) != -1) {
//                callback.onAudioDataAvailable(System.currentTimeMillis(), mAudioBuffer);
//            }
//        } catch (Exception e1) {
//            e1.printStackTrace();
//        }
    }
}
