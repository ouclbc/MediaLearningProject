package com.ouclbc.medialearningproject.player;

import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.util.Log;

import java.nio.ByteBuffer;

public class AVSync {

    private AudioSink mAudioSink;
    private MediaCodec mAudioDecoder;
    private MediaCodec mVideoDecoder;
    private MediaExtractor mExtractor;
    private int mAudioTrack = -1;
    private int mVideoTrack = -1;
    private Thread mThread;
    private Thread mAudioSinkThread;
    private Thread mVideoSinkThread;
    private long mStartMs;
    private long mPositionUs;
    private long mVideoLastTimeUs;
    private long mAudioLastTimeUs;
    private ByteBuffer[] mAudioInputBuffers;
    private ByteBuffer[] mVideoInputBuffers;

    public AVSync(MediaExtractor extractor, int videoTrack, int audioTrack,
            MediaCodec videoDecoder, MediaCodec audioDecoder, AudioSink audioSink) {
        mExtractor = extractor;
        mVideoDecoder = videoDecoder;
        mAudioDecoder = audioDecoder;
        mAudioSink = audioSink;
        mVideoTrack = videoTrack;
        mAudioTrack = audioTrack;
        mVideoInputBuffers = mVideoDecoder.getInputBuffers();
        mAudioInputBuffers = mAudioDecoder.getInputBuffers();
    }

    public void start() {
        Log.d("XPlayer", "AVsync.start");
        mThread = new Thread() {
            public void run() {
                doStart();
            }
        };
        mThread.start();
        if (XPlayer.SHOW_AUDIO) {
            mAudioSinkThread = new Thread() {
                public void run() {
                    doAudioSink();
                }

            };
            mAudioSinkThread.start();
        }

        if (XPlayer.SHOW_VIDEO) {
            mVideoSinkThread = new Thread() {
                public void run() {
                    doVideoSink();
                }

            };
            mVideoSinkThread.start();
        }
    }

    protected void doVideoSink() {
        BufferInfo info = new BufferInfo();
        int outIndex = mVideoDecoder.dequeueOutputBuffer(info, 10000);
        while (!Thread.interrupted()) {
//            Log.w("XPlayer", "v==>>>voutIndex = " + outIndex);
            if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                Log.d("XPlayer", "v==>>>==INFO_OUTPUT_BUFFERS_CHANGED==");
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("XPlayer",
                        "v==>>==INFO_OUTPUT_FORMAT_CHANGED== New format "
                                + mVideoDecoder.getOutputFormat());
            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                Log.d("XPlayer", "v==>>>==INFO_TRY_AGAIN_LATER==");
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
            } else if (outIndex >= 0) {
                Log.d("XPlayer", "v==>>>presentationTimeUs = " + info.presentationTimeUs
                        + "mPositionUs = "+mPositionUs+",now - startMs = " + (System.currentTimeMillis() - mStartMs));
                while (info.presentationTimeUs - mPositionUs > 30000) {
                    try {
                        Log.d("XPlayer", "v==>>> sleep");
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }      
                if(info.presentationTimeUs - mPositionUs < -30000){
                    Log.d("XPlayer", "v==>>> DROP presentationTimeUs = " + info.presentationTimeUs);
                    mVideoDecoder.releaseOutputBuffer(outIndex, false);
                }else{
                    mVideoDecoder.releaseOutputBuffer(outIndex, true);
                }
                
            }
            // All decoded frames have been rendered, we can stop playing
            // now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("XPlayer", "v==>>OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
            outIndex = mVideoDecoder.dequeueOutputBuffer(info, 10000);
        }
    }

    private void doAudioSink() {
        ByteBuffer[] outputBuffers = mAudioDecoder.getOutputBuffers();
        BufferInfo info = new BufferInfo();
        mStartMs = System.currentTimeMillis();
        int outIndex = mAudioDecoder.dequeueOutputBuffer(info, 10000);
        while (!Thread.interrupted()) {
//            Log.w("XPlayer", "a==>>>outIndex = " + outIndex);
            if (outIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
//                Log.d("XPlayer", "a==>>>==INFO_OUTPUT_BUFFERS_CHANGED==");
                outputBuffers = mAudioDecoder.getOutputBuffers();
            } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                Log.d("XPlayer",
                        "a==>>==INFO_OUTPUT_FORMAT_CHANGED== New format "
                                + mAudioDecoder.getOutputFormat());

            } else if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
//                Log.d("XPlayer", "a==>>>==INFO_TRY_AGAIN_LATER==");
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                }
            } else if (outIndex >= 0) {
                ByteBuffer buffer = outputBuffers[outIndex];

                Log.d("XPlayer", "a==>>>presentationTimeUs = " + info.presentationTimeUs
                        + ",mPositionUs = " + mPositionUs );
                byte[] out = new byte[info.size];
                buffer.get(out);
                buffer.clear();// MUST
                mAudioSink.write(out, 0, info.size);
                if(info.presentationTimeUs > mPositionUs ){
                    mPositionUs = info.presentationTimeUs;
                    mAudioDecoder.releaseOutputBuffer(outIndex, true);
                }else{
                    mAudioDecoder.releaseOutputBuffer(outIndex, false);
                }
            }
            // All decoded frames have been rendered, we can stop playing
            // now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("XPlayer", "aOutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
            outIndex = mAudioDecoder.dequeueOutputBuffer(info, 10000);
        }

    }

    private void doStart() {
        Log.d("XPlayer", "AVsync.doStart");
        boolean isEOS = false;
        int videoInIndex = -1;
        int audioInIndex = -1;
        Log.d("XPlayer", "mAudioTrack = " + mAudioTrack);
        if (XPlayer.SHOW_AUDIO && mAudioTrack >= 0) {

            while (!Thread.interrupted() && !isEOS) {
                if (XPlayer.SHOW_VIDEO && mVideoTrack >= 0) {
                    mExtractor.selectTrack(mVideoTrack);
                    mExtractor.seekTo(mVideoLastTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                    while(!Thread.interrupted() && !isEOS){
                        if(videoInIndex < 0){
                            videoInIndex = mVideoDecoder.dequeueInputBuffer(0);
                        }
                        Log.w("XPlayer", "v<<<==inIndex = " + videoInIndex);
                        if (videoInIndex >= 0) {
                            ByteBuffer buffer = mVideoInputBuffers[videoInIndex];
                            int offset = buffer.position();
                            int sampleSize = mExtractor.readSampleData(buffer, offset);
                            if (sampleSize < 0) {
                                Log.d("XPlayer", "v<<<==vInputBuffer BUFFER_FLAG_END_OF_STREAM");
                                mVideoDecoder.queueInputBuffer(videoInIndex, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                videoInIndex = -1;
                                isEOS = true;
                            } else {
                                buffer.position(buffer.position()+sampleSize);
                                Log.d("XPlayer",
                                        "v<<<==queueInputBuffer sampleSize = " + sampleSize
                                                + ",SampleTime() = " + mExtractor.getSampleTime()
                                                + ",flag = " + mExtractor.getSampleFlags());
                                if(mExtractor.getSampleTime() > mVideoLastTimeUs){
                                    mVideoLastTimeUs = mExtractor.getSampleTime();
                                    mVideoDecoder.queueInputBuffer(videoInIndex, 0, sampleSize,
                                        mExtractor.getSampleTime(), 0);
                                    videoInIndex = -1;
                                }else{
                                    buffer.clear();
                                }
                                mExtractor.advance();
                            }
                        }else{
                            break;
                        }
                    }
                    mExtractor.unselectTrack(mVideoTrack);
                }

                mExtractor.selectTrack(mAudioTrack);
                mExtractor.seekTo(mAudioLastTimeUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                {
                    while (!Thread.interrupted() && !isEOS) {
                        if(audioInIndex < 0){
                            audioInIndex = mAudioDecoder.dequeueInputBuffer(0);
                        }
                        Log.w("XPlayer", "a<<<==inIndex = " + audioInIndex);
                        if (audioInIndex >= 0) {
                            ByteBuffer buffer = mAudioInputBuffers[audioInIndex];
                            int offset = buffer.position();
                            int sampleSize = mExtractor.readSampleData(buffer, offset);
                            if (sampleSize < 0) {
                                Log.d("XPlayer", "a<<<==InputBuffer BUFFER_FLAG_END_OF_STREAM");
                                mAudioDecoder.queueInputBuffer(audioInIndex, 0, 0, 0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                                audioInIndex = -1;
                                isEOS = true;
                            } else {
                                buffer.position(buffer.position() + sampleSize);
                                Log.d("XPlayer",
                                        "a<<<==queueInputBuffer sampleSize = " + sampleSize
                                                + ",SampleTime() = " + mExtractor.getSampleTime()
                                                + ",flag = " + mExtractor.getSampleFlags());
                                if(mExtractor.getSampleTime()>mAudioLastTimeUs){
                                    mAudioLastTimeUs = mExtractor.getSampleTime();
                                
                                    mAudioDecoder.queueInputBuffer(audioInIndex, 0, sampleSize,
                                        mExtractor.getSampleTime(), 0);
                                    audioInIndex = -1;
                                }else{
                                    buffer.clear();
                                }
                                mExtractor.advance();
                            }
                        }else{
                            break;
                        }
                    }
                    mExtractor.unselectTrack(mAudioTrack);
                }
            }
        } else {

        }

    }

    public void release() {
        if (mThread != null) {
            mThread.interrupt();
        }
        if (mAudioSinkThread != null) {
            mAudioSinkThread.interrupt();
        }
    }

}
