package com.agora.io.audioGather;

public interface IAudioCallback {
    void onAudioDataAvailable(long timeStamp, byte[] audioData);
}
