package com.agora.io.audioGather;


public interface IAudioController {
    AudioStatus init(IAudioCallback callback);
    AudioStatus start(int samplingRate);
    AudioStatus stop();
    void destroy();
}
