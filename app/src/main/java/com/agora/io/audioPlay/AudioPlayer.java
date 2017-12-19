package com.agora.io.audioPlay;

/**
 * Created by wubingshuai on 21/11/2017.
 */

import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

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
            Log.e("==Beck==", " sampleRateInHz :" +sampleRateInHz + " channelConfig :"+channelConfig +  " audioFormat: "+audioFormat);
//            long val =  (mMinBufferSize & 0xffffffffL);
            Log.e("==Beck==" , " val:  "  +  mMinBufferSize);
            if (mMinBufferSize == AudioTrack.ERROR_BAD_VALUE) {
                return false;
            }

            mAudioTrack = new AudioTrack(streamType, sampleRateInHz, channelConfig, audioFormat, mMinBufferSize, DEFAULT_PLAY_MODE);
            if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                throw new RuntimeException("Error on AudioTrack created");
            }
            mAudioTrack.play();
            mAudioStatus = AudioStatus.RUNNING;
        }

        return true;
    }

    public void stopPlayer() {
        if(mAudioStatus == AudioStatus.RUNNING) {
            mAudioStatus = AudioStatus.INITIALISING ;
            mAudioTrack.stop();
            mAudioTrack.release();
            mAudioTrack = null;
            Log.e("beck","Here it is 2!");
        }
        Log.e("beck","Here it is 3,mAudioStatus: " + mAudioStatus);
    }

    public boolean play(byte[] audioData, int offsetInBytes, int sizeInBytes) {
        if(mAudioStatus == AudioStatus.RUNNING) {
            mAudioTrack.write(audioData, offsetInBytes, sizeInBytes);
        }else{
            Log.e("AudioPlayer", "=== AudioPlayer Play ===");
        }
        return true;
    }
}