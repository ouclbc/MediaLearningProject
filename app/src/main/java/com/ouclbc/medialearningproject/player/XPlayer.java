package com.ouclbc.medialearningproject.player;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;

public class XPlayer extends Handler {
    
    public static final boolean SHOW_VIDEO = true;
    public static final boolean SHOW_AUDIO = true;
    
    private static final int ACTION_PLAY = 0;
    private static final int ACTION_PAUSE = 1;
    private static final int ACTION_STOP = 2;
    
    private MediaExtractor mExtractor;
    private MediaCodec mVideoDecoder;
    private int mVideoTrack = -1;
    private int mAudioTrack = -1;
    private MediaCodec mAudioDecoder;
    private AudioSink mAudioSink;
    private Surface mSurface;
    private static HandlerThread mThread;
    private AVSync mSync;
    static{
        if(mThread == null){
            mThread = new HandlerThread("XPlayer");
            mThread.start();
        }
    }
    
    public XPlayer(){
        super(mThread.getLooper());
        mExtractor = new MediaExtractor();
    }
    
    public void play(String path,Surface surface){
        Log.d("XPlayer", "play = "+path);
        this.mSurface = surface;
        Message msg = obtainMessage(ACTION_PLAY);
        msg.obj = path;
        sendMessage(msg);
    }
    public void stop(){
        
    }
    
    public void pause(){
        
    }
    
    public void release(){
        mThread.quit();
        mSync.release();
        mAudioSink.release();
        if(mVideoDecoder != null)
            mVideoDecoder.release();
        if(mAudioDecoder != null)
            mAudioDecoder.release();
        mExtractor.release();
    }

    @Override
    public void handleMessage(Message msg) {
        switch(msg.what){
            case ACTION_PLAY:
                doPlay((String)msg.obj);
                break;
            default:
                break;
        }
    }

    private void doPlay(String path) {
        Log.d("XPlayer", "doPlay path = "+path);
        try {
            mExtractor.setDataSource(path);
            initDecoder();
            mAudioSink = new AudioSink(mExtractor.getTrackFormat(mAudioTrack));
            mSync = new AVSync(mExtractor,mVideoTrack,mAudioTrack,mVideoDecoder,mAudioDecoder,mAudioSink);
            mSync.start();
        } catch (Exception e) {
            Log.e("XPlayer", "", e);
        }
        
    }

    private void initDecoder() {
        for (int i = 0; i < mExtractor.getTrackCount(); i++) {
            MediaFormat format = mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (SHOW_VIDEO && mime.startsWith("video/") && mVideoDecoder == null) {
                mVideoTrack = i;
                try {
                    mVideoDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mVideoDecoder.configure(format, mSurface, null, 0);
            }
            
            if(SHOW_AUDIO && mime.startsWith("audio/") && mAudioDecoder == null){
                mAudioTrack = i;
                try {
                    mAudioDecoder = MediaCodec.createDecoderByType(mime);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mAudioDecoder.configure(format, null, null, 0);
            }
        }
        if (mVideoDecoder == null && mAudioDecoder == null) {
            Log.e("XPlayer", "Can't find any video or audio track!");
            return;
        }
        if(mVideoDecoder != null){
            mVideoDecoder.start();
        }
        if(mAudioDecoder != null){
            mAudioDecoder.start();
        }
    }
    
    
}
