package com.agora.io.audioGather;


public interface IAudioController {
    AudioStatus init(IAudioCallback callback);
//    AudioStatus start(int samplingRate);
    AudioStatus start();
    AudioStatus stop();
    void destroy();
}
