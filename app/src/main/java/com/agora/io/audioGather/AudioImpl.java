package com.agora.io.audioGather;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class AudioImpl implements IAudioController {
    private static final String TAG = AudioImpl.class.getName();
    private final static int mSendPeriod = 20;

    private AudioRecord mAudioRecorder = null;
    private IAudioCallback callback = null;

    private AudioStatus mStatus = AudioStatus.STOPPED;

    private int mFrameBufferSize = -1;
    private byte[] mAudioBuffer = null;


    @Override
    public AudioStatus init(IAudioCallback callback) {
        if (mStatus == AudioStatus.STOPPED) {
            this.callback = callback;
            mStatus = AudioStatus.INITIALISING;
        }
        return mStatus;
    }

    @Override
    public AudioStatus start(int samplingRate) {
        if (mStatus == AudioStatus.INITIALISING) {
            int sizeInBytes = AudioRecord.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 2;

            if (mAudioRecorder != null) {
                mAudioRecorder.release();
                mAudioRecorder = null;
            }

            mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    samplingRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    sizeInBytes);

            mFrameBufferSize = samplingRate * mSendPeriod / 1000;

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

    private void gatherData() {
        while (mStatus == AudioStatus.RUNNING) {
            int read = mAudioRecorder.read(mAudioBuffer, 0, mFrameBufferSize);
            if (read != mFrameBufferSize)
                continue;
            callback.onAudioDataAvailable(System.currentTimeMillis(), mAudioBuffer);
        }
    }
}
