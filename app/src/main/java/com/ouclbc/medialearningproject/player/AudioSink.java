package com.ouclbc.medialearningproject.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaFormat;
import android.util.Log;

public class AudioSink {
    private AudioTrack mAudioTrack;
    private int mMinBufferSize = 0;
    private byte[] mBuffer = null;
    private int mBufferSize = 0;

    public AudioSink(MediaFormat format) {
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        mMinBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.d("XPlayer", "==>>>bufferSize ===== " + mMinBufferSize);
        mBuffer = new byte[mMinBufferSize];
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize,
                AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    public void write(byte[] data, int offset, int size) {
        Log.d("XPlayer", "==>>>offset = " + offset + ",size = " + size);
        int copySize = 0;
        if (size + mBufferSize < mMinBufferSize) {
            System.arraycopy(data, offset, mBuffer, mBufferSize, size);
            mBufferSize += size;
        } else {
            copySize = (mMinBufferSize - mBufferSize) > size ? size
                    : (mMinBufferSize - mBufferSize);
            System.arraycopy(data, offset, mBuffer, mBufferSize, copySize);
            mBufferSize += copySize;
        }
        if (mBufferSize == mMinBufferSize) {
            mAudioTrack.write(mBuffer, 0, mMinBufferSize);
            mAudioTrack.flush();
            mBufferSize = 0;
        }
        if (copySize > 0) {
            System.arraycopy(data, offset + copySize, mBuffer, mBufferSize, size - copySize);
            mBufferSize += size - copySize;
        }
        Log.d("XPlayer", "==>>>mBufferSize = " + mBufferSize);
    }

    public void stop() {
        mAudioTrack.stop();
    }

    public void release() {
        mAudioTrack.release();
    }

}
