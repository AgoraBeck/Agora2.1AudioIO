package com.agora.io;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
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

import agora.io.agoraaudio.R;
import io.agora.rtc.Constants;
import io.agora.rtc.IAudioFrameObserver;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;

public class ChatRoomActivity extends AppCompatActivity implements IAudioCallback, IAudioFrameObserver {
    private final static String TAG = ChatRoomActivity.class.getSimpleName();
    private TextView mTvInfoDisplay;
    private boolean mIsPlaying = false;

    private String mStrChannelName;
    private AudioEnum mAE;
    private AudioProfile mAP;
    private RtcEngine mRtcEngine;
    private int samplingRate;
    private AudioPlayer mAudioPlayer;
    private AudioImpl mAI;

    IRtcEngineEventHandler mEngineHandler = new IRtcEngineEventHandler() {
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            sendMessage("JoinChannelSuccess:" + (uid & 0xFFFFFFFFL));
        }

        @Override
        public void onRejoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onRejoinChannelSuccess(channel, uid, elapsed);
        }

        @Override
        public void onError(int err) {
            super.onError(err);
        }

        @Override
        public void onApiCallExecuted(String api, int error) {
            super.onApiCallExecuted(api, error);
            sendMessage("ApiCallExecuted:" + api);
        }

        @Override
        public void onLeaveChannel(RtcStats stats) {
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
        public void onRtcStats(RtcStats stats) {
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
            mRtcEngine = RtcEngine.create(ChatRoomActivity.this , getString(R.string.app_key), mEngineHandler);
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING) ;
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER,"") ;
            mRtcEngine.setEnableSpeakerphone(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAudioDataAvailable(long timeStamp, byte[] audioData) {
            mRtcEngine.pushExternalAudioFrame(audioData, timeStamp);
//        Log.e(TAG , Arrays.toString(audioData)) ;
    }

    @Override
    public boolean onRecordFrame(byte[] bytes, int i, int i1, int i2, int i3) {
       // Log.e(TAG , Arrays.toString(bytes)) ;
        return false;
    }

    @Override
    public boolean onPlaybackFrame(final byte[] bytes, int i, int i1, int i2, final int i3) {
       // Log.e(TAG , Arrays.toString(bytes)) ;
        if (!mIsPlaying) {
            mAudioPlayer.startPlayer(AudioManager.STREAM_VOICE_CALL, i3, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mIsPlaying = true;
        }
        mAudioPlayer.play(bytes, 0, bytes.length);

        try {
            fps.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void dispatchWork() {
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
        joinChannel();
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
        Log.e(TAG , "onLeaveChannel") ;
        leaveChannel();
    }

    private void doApp2App() {
        mTvInfoDisplay.append("enter App2App mode!\n");

        startAudioGather();
        startAudioPlayer();
    }

    private void finishApp2App() {
        finishAudioGather();
        finishAudioPlayer();
    }

    private void doApp2Sdk() {
        mTvInfoDisplay.append("enter App2SDK mode!\n");
        startAudioGather();
    }

    private void finishApp2Sdk() {
        finishAudioGather();
    }

    private void doSdk2App() {
        mTvInfoDisplay.append("enter SDK2App mode!\n");
        startAudioPlayer();
    }

    private void finishSdk2App() {
        finishAudioPlayer();
    }

    private void doSdk2Sdk() {
        mTvInfoDisplay.append("enter SDK2SDK mode!\n");
    }

    private void finishSdk2Sdk() {
    }

    private void startAudioGather() {
        mRtcEngine.setExternalAudioSource(true, samplingRate, 1);

        if (mAI == null)
            mAI = new AudioImpl();
        mAI.init(this);
        mAI.start(samplingRate);
    }

    private void finishAudioGather() {
        if (mAI != null) {
            mAI.stop();
            mAI.destroy();
        }
    }

    File f ;
    FileOutputStream fps ;
    private void startAudioPlayer() {
        if (mAudioPlayer == null)
            mAudioPlayer = new AudioPlayer();

        mRtcEngine.setParameters("{\"che.audio.external_render\": true}");
        mRtcEngine.registerAudioFrameObserver(this);

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
            mIsPlaying = false ;
            mRtcEngine.registerAudioFrameObserver(null);
        }
    }

    private void joinChannel() {
        mRtcEngine.setParameters("{\"rtc.log_filter\":65535}");
        mRtcEngine.setLogFile("/sdcard/open_live.log");
        mRtcEngine.joinChannel(null, mStrChannelName.trim(), getResources().getString(R.string.app_key), 0);
    }

    private void leaveChannel() {
        mRtcEngine.leaveChannel();
        finish();
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
