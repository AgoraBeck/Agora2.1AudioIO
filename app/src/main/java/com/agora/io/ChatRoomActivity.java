package com.agora.io;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.service.carrier.CarrierService;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.agora.io.audioPlay.AudioPlayer;
import com.agora.io.audioGather.AudioImpl;
import com.agora.io.audioGather.IAudioCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.security.auth.login.LoginException;

import agora.io.agoraaudio.R;
import io.agora.rtc.Constants;
import io.agora.rtc.IAudioFrameObserver;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class ChatRoomActivity extends AppCompatActivity implements IAudioCallback, IAudioFrameObserver {
    private final static String TAG = ChatRoomActivity.class.getSimpleName();
    private TextView mTvInfoDisplay;

    private String mStrChannelName;
    private AudioEnum mAE = AudioEnum.SDK2SDK;
    private AudioProfile mAP  = AudioProfile.AUDIO_PROFILE_16000;
    private RtcEngine mRtcEngine = null;
    private int samplingRate = 16000; // debug, use the fixed value
    private AudioPlayer mAudioPlayer = null;
    private AudioImpl mAI = null;
    private static final double sampleInterval  = 0.01; //  sampleInterval >= 0.01
    private int channels = 2; // 1: Mono, 2: Stereo
    private int samplesPerCall = 0;

    File f ;
    FileOutputStream fps ;

    IRtcEngineEventHandler mEngineHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            sendMessage("onJoinChannelSuccess:" + (uid & 0xFFFFFFFFL));

            if (mAE == AudioEnum.App2App || mAE == AudioEnum.App2SDK) {
                mAI.start();
            }
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
        }

        @Override
        public void onError(int err) {
            super.onError(err);
        }

//        @Override
//        public void onApiCallExecuted(String api, int error) {
//            super.onApiCallExecuted(api, error);
//            sendMessage("ApiCallExecuted:" + api);
//        }

        @Override
        public void onLeaveChannel(IRtcEngineEventHandler.RtcStats stats) {
            Log.e(TAG,"onLeaveChannel");
            super.onLeaveChannel(stats);
        }

        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            sendMessage("onUserJoined:" + (uid & 0xFFFFFFFFL));
        }

        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            sendMessage("onUserOffLine:" + (uid & 0xFFFFFFFFL));
        }

        @Override
        public void onUserMuteAudio(int uid, boolean muted) {
            super.onUserMuteAudio(uid, muted);
            sendMessage("onUserMuteAudio:" + (uid & 0xFFFFFFFFL));
        }

        @Override
        public void onConnectionLost() {
            super.onConnectionLost();
            sendMessage("onConnectionLost");
        }

        @Override
        public void onConnectionInterrupted() {
            super.onConnectionInterrupted();
            sendMessage("onConnectionInterrupted");
        }

        @Override
        public void onConnectionBanned() {
            super.onConnectionBanned();
        }

        @Override
        public void onAudioRouteChanged(int routing) {
            super.onAudioRouteChanged(routing);
        }

        @Override
        public void onFirstLocalAudioFrame(int elapsed) {
            super.onFirstLocalAudioFrame(elapsed);
            sendMessage("onFirstLocalAudioFrame:" + elapsed);
        }

        @Override
        public void onFirstRemoteAudioFrame(int uid, int elapsed) {
            super.onFirstRemoteAudioFrame(uid, elapsed);
            sendMessage("onFirstRemoteAudioFrame:" + elapsed);
        }

        @Override
        public void onRtcStats(IRtcEngineEventHandler.RtcStats stats) {
            super.onRtcStats(stats);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        initAction();
        initWidget();
        initAgoraEngine();
        dispatchWork();

        joinChannel();

        if (mAE == AudioEnum.App2App || mAE == AudioEnum.SDK2App) {
            mAudioPlayer.startPlayer();
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void initAction() {
        Intent mIntent = getIntent();
        mStrChannelName = mIntent.getStringExtra(Constants_.CHANNEL_NAME);
        mAE = (AudioEnum) mIntent.getSerializableExtra(Constants_.AUDIO_ENUM);
        mAP = (AudioProfile) mIntent.getSerializableExtra(Constants_.AUDIO_PROFILE);

        switch (mAP) {
            case AUDIO_PROFILE_8000:
                samplingRate = 8000;
                break;
            case AUDIO_PROFILE_16000:
                samplingRate = 16000;
                break;
            case AUDIO_PROFILE_32000:
                samplingRate = 32000;
                break;
            case AUDIO_PROFILE_44100:
                samplingRate = 44100;
        }
        Log.d(TAG, "samplingRate: " + samplingRate);
    }

    private void initWidget() {
        TextView mTvChannelName = findViewById(R.id.tv_channel_room);
        mTvInfoDisplay = findViewById(R.id.tv_info_display);

        mTvChannelName.setText(mStrChannelName);
    }

    public void onMuteClick(View v) {
        ImageView vi = (ImageView) v;
        if (mRtcEngine != null) {
            if (v.getTag() == null) {
                v.setTag(false);
            }
            boolean b = ((boolean) v.getTag());
            if (!b) {
                vi.setColorFilter(getResources().getColor(R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
                mRtcEngine.muteLocalAudioStream(true);
            } else {
                vi.clearColorFilter();
                mRtcEngine.muteLocalAudioStream(false);
            }
            v.setTag(!b);

        }
    }

    public void onHungUpClick(View v) {
        dispatchFinish();
    }

    public void onEarPhone(View v) {
        ImageView vi = (ImageView) v;
        if (mRtcEngine != null) {
            if (v.getTag() == null) {
                v.setTag(true);
            }
            boolean b = ((boolean) v.getTag());
            if (b) {
                vi.setColorFilter(getResources().getColor(R.color.agora_blue), PorterDuff.Mode.MULTIPLY);
                mRtcEngine.setEnableSpeakerphone(true);
            } else {
                vi.clearColorFilter();
                mRtcEngine.setEnableSpeakerphone(false);
            }
            v.setTag(!b);
        }
    }

    private void initAgoraEngine() {
        try {
            if(mRtcEngine == null){
                Log.d(TAG,"== initAgoraEngine ==");
                mRtcEngine = RtcEngine.create(getBaseContext(), getString(R.string.app_key), mEngineHandler);
                mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
                mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);
                mRtcEngine.setEnableSpeakerphone(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioDataAvailable(long timeStamp, byte[] audioData) {
            mRtcEngine.pushExternalAudioFrame(audioData, timeStamp);
    }

    @Override
    public boolean onRecordFrame(byte[] bytes, int i, int i1, int i2, int i3) {
        Log.e(TAG,"=== onRecordFrame ====");
        return true;
    }

    @Override
    public boolean onPlaybackFrame(final byte[] bytes, int i, int i1, int i2, final int i3) {
        //public abstract  boolean onPlaybackFrame(byte[] samples, int numOfSamples, int bytesPerSample, int channels, int samplesPerSec);
        //Log.e(TAG, "numOfSamples: "+ i + " bytesPerSample :" + i1 + " channels: " + i2 + " samplesPerSec :" + i3 + " bytes.length： " + bytes.length);
        // Log.e(TAG , Arrays.toString(bytes));ß

        if(mAudioPlayer!= null) {
            mAudioPlayer.play(bytes, 0, bytes.length);
        }
        try {
            fps.write(bytes);
        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }

    private void dispatchWork() {
        //The algorithms for samplesPerCall of setPlaybackAudioFrameParameters()
        samplesPerCall = (int)(samplingRate * channels * sampleInterval);
        Log.e(TAG,"App numOfSamples: " + samplesPerCall);
        switch (mAE) {
            case App2App:
                doApp2App();
                break;
            case App2SDK:
                doApp2Sdk();
                break;
            case SDK2App:
                doSdk2App();
                break;
            case SDK2SDK:
                doSdk2Sdk();
                break;
            default:
                Log.e(TAG, "error on dispatchWork!");
                break;
        }
    }

    private void dispatchFinish() {
        switch (mAE) {
            case App2App:
                finishApp2App();
                break;
            case App2SDK:
                finishApp2Sdk();
                break;
            case SDK2App:
                finishSdk2App();
                break;
            case SDK2SDK:
                finishSdk2Sdk();
                break;
            default:
                Log.e(TAG, "error on dispatchFinish!");
                break;
        }
        leaveChannel();
        finish();
    }

    private void doApp2App() {
        mTvInfoDisplay.append("enter App2App mode!\n");

        startAudioGather(samplingRate, channels);
        startAudioPlayer(AudioManager.STREAM_VOICE_CALL, samplingRate, channels, AudioFormat.ENCODING_PCM_16BIT);

        mRtcEngine.setExternalAudioSource(true, samplingRate, channels);
        mRtcEngine.setParameters("{\"che.audio.external_render\": true}");
        mRtcEngine.registerAudioFrameObserver(this);
        mRtcEngine.setPlaybackAudioFrameParameters(samplingRate, channels,0, samplesPerCall);
    }

    private void finishApp2App() {
        mRtcEngine.registerAudioFrameObserver(null);
        mRtcEngine.setParameters("{\"che.audio.external_render\": false}");
        mRtcEngine.setExternalAudioSource(false, samplingRate, channels);
        finishAudioGather();
        finishAudioPlayer();
    }

    private void doApp2Sdk() {
        startAudioGather(samplingRate, channels);
        mRtcEngine.setExternalAudioSource(true, samplingRate, channels);
        mRtcEngine.setParameters("{\"che.audio.external_render\": false}");
        mTvInfoDisplay.append("enter App2SDK mode!\n");
    }

    private void finishApp2Sdk() {
        mRtcEngine.setExternalAudioSource(false, samplingRate, channels);
        finishAudioGather();
    }

    private void doSdk2App() {
        startAudioPlayer(AudioManager.STREAM_VOICE_CALL, samplingRate, channels, AudioFormat.ENCODING_PCM_16BIT);
        mRtcEngine.setPlaybackAudioFrameParameters(samplingRate, channels, 0, samplesPerCall);
        mRtcEngine.setParameters("{\"che.audio.external_render\": true}");
        mTvInfoDisplay.append("enter SDK2App mode!\n");
        mRtcEngine.registerAudioFrameObserver(this);
    }

    private void finishSdk2App() {
        finishAudioPlayer();
        mRtcEngine.registerAudioFrameObserver(null);
        mRtcEngine.setParameters("{\"che.audio.external_render\": false}");
    }

    private void doSdk2Sdk() {
        mTvInfoDisplay.append("enter SDK2SDK mode!\n");
    }

    private void finishSdk2Sdk() {
    }

    private void startAudioGather(int samplingRate, int channelConfig) {
        if (mAI == null) {
            mAI = new AudioImpl(samplingRate, channelConfig);
        }
        mAI.init(this);
    }

    private void finishAudioGather() {
        if (mAI != null) {
            mAI.stop();
            mAI.destroy();
        }
    }

    private void startAudioPlayer(int streamType, int sampleRateInHz, int channelConfig, int audioFormat) {
        if (mAudioPlayer == null) {
            mAudioPlayer = new AudioPlayer(streamType, sampleRateInHz, channelConfig, audioFormat);
        }

        f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/123.pcm") ;
        if(f.exists()){
            f.delete() ;
        }
        try {
            fps = new FileOutputStream(f) ;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void finishAudioPlayer() {
        if (mAudioPlayer != null) {
            mAudioPlayer.stopPlayer();
        }
    }

    private void joinChannel() {
        mRtcEngine.setParameters("{\"rtc.log_filter\":65535}");
        mRtcEngine.setLogFile("/sdcard/open_live.log");

        int ret = mRtcEngine.joinChannel(null, mStrChannelName.trim(), getResources().getString(R.string.app_key), 0);
        if(null != mRtcEngine)
            Log.e(TAG, "SDK Ver: " + mRtcEngine.getSdkVersion() + " ret : " + ret);
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
    }

    private void sendMessage(@NonNull final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTvInfoDisplay.append(s + "\n");
            }
        });
    }
}
