package com.agora.io.audioPlay;

/**
 * Created by wubingshuai on 21/11/2017.
 */

import android.media.AudioTrack;

import com.agora.io.audioGather.AudioStatus;

public class AudioPlayer {

    private static final int DEFAULT_PLAY_MODE = AudioTrack.MODE_STREAM;

    private boolean mIsPlayStarted = false;
    private AudioTrack mAudioTrack;
    private AudioStatus mAudioStatus = AudioStatus.INITIALISING ;

    public boolean startPlayer(int streamType, int sampleRateInHz, int channelConfig, int audioFormat) {
        if(mAudioStatus == AudioStatus.INITIALISING) {
            if (mIsPlayStarted) {
                return false;
            }

            int mMinBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            if (mMinBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                return false;
            }

            mAudioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, mMinBufferSize, DEFAULT_PLAY_MODE);
            if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                throw new RuntimeException("Error on AudioTrack created");
            }
            mAudioStatus = AudioStatus.RUNNING ;
        }

        return true;
    }

    public void stopPlayer() {
        if(mAudioStatus == AudioStatus.RUNNING) {
            mAudioStatus = AudioStatus.INITIALISING ;
            mAudioTrack.stop();
            mAudioTrack.release();
        }
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if(mAudioStatus == AudioStatus.RUNNING) {
            mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
            mAudioTrack.play();
        }
        return true;
    }
}