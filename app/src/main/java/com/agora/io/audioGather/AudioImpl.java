package com.agora.io.audioGather;
import android.os.Environment;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.FileInputStream;
import java.io.InputStream;

public class AudioImpl implements IAudioController {
    private static final String TAG = AudioImpl.class.getName();
    private final static int mSendPeriod = 20;

    private AudioRecord mAudioRecorder = null;
    private IAudioCallback callback = null;

    private AudioStatus mStatus = AudioStatus.STOPPED;

    private int mFrameBufferSize = -1;
    private byte[] mAudioBuffer = null;

    //==
    private InputStream in = null;

    @Override
    public AudioStatus init(IAudioCallback callback) {
        if (mStatus == AudioStatus.STOPPED) {
            this.callback = callback;
            mStatus = AudioStatus.INITIALISING;
        }
        //==
        try {
            in = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Sugar_16k.pcm");
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return mStatus;
    }

    @Override
    public AudioStatus start(int samplingRate) {
        if (mStatus == AudioStatus.INITIALISING) {
            int sizeInBytes = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

            if (mAudioRecorder != null) {
                mAudioRecorder.release();
                mAudioRecorder = null;
            }

            mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    samplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeInBytes);

            mFrameBufferSize = samplingRate * mSendPeriod / 1000 * 1;

            if (mAudioBuffer == null)
                mAudioBuffer = new byte[mFrameBufferSize];
                

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
        if (mStatus == AudioStatus.RUNNING) {
            mStatus = AudioStatus.INITIALISING;
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

    int byteread = 0;
    private void gatherData() {
        while (mStatus == AudioStatus.RUNNING) {
            int read = mAudioRecorder.read(mAudioBuffer, 0, mFrameBufferSize);
            if (read != mFrameBufferSize){
                Log.e(TAG,"== before :onAudioDataAvailable ==");
                continue;
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
