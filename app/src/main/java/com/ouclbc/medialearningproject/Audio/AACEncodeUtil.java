package com.ouclbc.medialearningproject.Audio;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by libaocheng on 2017-12-20.
 * http://blog.csdn.net/TinsanMr/article/details/51049179
 */

public class AACEncodeUtil {
    private static final String TAG = "AACEncodeUtil";
    private static final String MIME = "audio/mp4a-latm";

    private FileOutputStream fos;
    private BufferedOutputStream bos;
    private String mDstPath;
    private MediaCodec mMediaEncode;
    private ByteBuffer[] encodeInputBuffers;
    private ByteBuffer[] encodeOutputBuffers;
    private MediaCodec.BufferInfo encodeBufferInfo;

    /**
     * 设置输入输出文件位置
     * @param dstPath
     */
    public void setIOoutPath(String dstPath) {
        this.mDstPath=dstPath;
    }
    /**
     * 初始化AAC编码器
     */
    public void initAACMediaEncode() {
        if (mDstPath == null) {
            throw new IllegalArgumentException("mDstPath can't be null");
        }
        try {
            fos = new FileOutputStream(new File(mDstPath));
            bos = new BufferedOutputStream(fos,200*1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            MediaFormat encodeFormat = MediaFormat.createAudioFormat(MIME, 44100, 2);//参数对应-> mime type、采样率、声道数
            encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);//比特率
            encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//作用于inputBuffer的大小
            mMediaEncode = MediaCodec.createEncoderByType(MIME);
            mMediaEncode.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mMediaEncode == null) {
            Log.e(TAG, "create mediaEncode failed");
            return;
        }
        mMediaEncode.start();

        encodeInputBuffers=mMediaEncode.getInputBuffers();
        encodeOutputBuffers=mMediaEncode.getOutputBuffers();
        encodeBufferInfo=new MediaCodec.BufferInfo();
    }

    /**
     *
     * @param input
     */
    public void encodeAAC(byte[] input){
        int inputBufferIndex = mMediaEncode.dequeueInputBuffer(-1);
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = encodeInputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.limit(input.length);
            inputBuffer.put(input);
            mMediaEncode.queueInputBuffer(inputBufferIndex, 0, input.length, 0, 0);
        }
        int outputIndex = mMediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
        ByteBuffer outputBuffer;
        byte[] chunkAudio;
        int outBitSize;
        int outPacketSize;
        Log.d(TAG, "outputIndex="+outputIndex);
        while (outputIndex >= 0) {
            if ((encodeBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                // 这里可以从ByteBuffer中获取csd参数
                mMediaEncode.releaseOutputBuffer(outputIndex, false);
                return;
            }
            outBitSize=encodeBufferInfo.size;
            outPacketSize=outBitSize+7;//7为ADTS头部的大小
            outputBuffer = encodeOutputBuffers[outputIndex];//拿到输出Buffer
            outputBuffer.position(encodeBufferInfo.offset);
            outputBuffer.limit(encodeBufferInfo.offset + outBitSize);
            chunkAudio = new byte[outPacketSize];
            addADTStoPacket(chunkAudio,outPacketSize);//添加ADTS 代码后面会贴上
            outputBuffer.get(chunkAudio, 7, outBitSize);
            outputBuffer.position(encodeBufferInfo.offset);
            try {
                bos.write(chunkAudio,0,chunkAudio.length);//BufferOutputStream 将文件保存到内存卡中 *.aac
            } catch (IOException e) {
                e.printStackTrace();
            }
            mMediaEncode.releaseOutputBuffer(outputIndex,false);
            outputIndex = mMediaEncode.dequeueOutputBuffer(encodeBufferInfo, 10000);
        }
    }

    /**
     * aac添加头协议,除了手动添加外，还可以使用MediaMuxer
     * @param packet
     * @param packetLen
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 4; // 44.1KHz
        int chanCfg = 2; // CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    /*private void onEncodeAacFrame(ByteBuffer bb, MediaCodec.BufferInfo info) {
        mediaMuxer.writeSampleData(audioTrackIndex, bb, info);
    }*/
    /*
     * 释放资源
     */
    public void release() {
        try {
            if (bos != null) {
                bos.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    bos = null;
                }
            }
        }

        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            fos = null;
        }
        if (mMediaEncode != null) {
            mMediaEncode.stop();
            mMediaEncode.release();
            mMediaEncode = null;
        }
    }
}
