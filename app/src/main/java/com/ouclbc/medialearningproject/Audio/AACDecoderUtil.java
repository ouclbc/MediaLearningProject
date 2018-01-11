package com.ouclbc.medialearningproject.Audio;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by libaocheng on 2017-12-1.
 */

public class AACDecoderUtil {
    private static final String TAG = "AACDecodeUtil";
    private static final int KEY_CHANNEL_COUNT = 2;
    private static final int KEY_SAMPLE_RATE = 48000;
    private MyAudioTrack mPlayer;
    private MediaCodec mDecoder;
    private int count =0;

    /**
     *
     * @return
     */
    public boolean prepare(){
        mPlayer = new MyAudioTrack(KEY_SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        mPlayer.init();
        try {
            String MIME = "audio/mp4a-latm";
            mDecoder = MediaCodec.createDecoderByType(MIME);
            MediaFormat mediaFormat = new MediaFormat();
            //数据类型
            mediaFormat.setString(MediaFormat.KEY_MIME, MIME);
            //声道个数
            mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, KEY_CHANNEL_COUNT);
            //采样率
            mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, KEY_SAMPLE_RATE);
            //比特率
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
            //用来标记AAC是否有adts头，1->有
            mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
            //用来标记aac的类型
            mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            byte[] data = new byte[]{(byte) 0x11, (byte) 0x90};
            ByteBuffer csd_0 = ByteBuffer.wrap(data);
            mediaFormat.setByteBuffer("csd-0", csd_0);
            mDecoder.configure(mediaFormat, null, null, 0);
        }catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (mDecoder == null) {
            return false;
        }
        mDecoder.start();
        return true;
    }

    /**
     * aac解码+播放
     */
    public void decode(byte[] buf, int offset, int length) {
        //输入ByteBuffer
        ByteBuffer[] codecInputBuffers = mDecoder.getInputBuffers();
        //输出ByteBuffer
        ByteBuffer[] codecOutputBuffers = mDecoder.getOutputBuffers();
        //等待时间，0->不等待，-1->一直等待
        long kTimeOutUs = 0;
        try {
            //返回一个包含有效数据的input buffer的index,-1->不存在
            int inputBufIndex = mDecoder.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                //获取当前的ByteBuffer
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                //清空ByteBuffer
                dstBuf.clear();
                //填充数据
                dstBuf.put(buf, offset, length);
                //将指定index的input buffer提交给解码器
                mDecoder.queueInputBuffer(inputBufIndex, 0, length, 0, 0);
            }
            //编解码器缓冲区
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            //返回一个output buffer的index，-1->不存在
            int outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);

            if (outputBufferIndex < 0) {
                //记录解码失败的次数
                count++;
            }
            ByteBuffer outputBuffer;
            while (outputBufferIndex >= 0) {
                //获取解码后的ByteBuffer
                outputBuffer = codecOutputBuffers[outputBufferIndex];
                //用来保存解码后的数据
                byte[] outData = new byte[info.size];
                outputBuffer.get(outData);
                //清空缓存
                outputBuffer.clear();
                //播放解码后的数据
                mPlayer.playAudioTrack(outData, 0, info.size);
                //释放已经解码的buffer
                mDecoder.releaseOutputBuffer(outputBufferIndex, false);
                //解码未解完的数据
                outputBufferIndex = mDecoder.dequeueOutputBuffer(info, kTimeOutUs);
            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            e.printStackTrace();
        }
    }

    //返回解码失败的次数
    public int getCount() {
        return count;
    }

    /**
     * 释放资源
     */
    public void stop() {
        try {
            if (mPlayer != null) {
                mPlayer.release();
                mPlayer = null;
            }
            if (mDecoder != null) {
                mDecoder.stop();
                mDecoder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
