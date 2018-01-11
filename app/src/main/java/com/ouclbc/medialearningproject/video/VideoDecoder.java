package video;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by libaocheng on 2017-11-24.
 */

public class VideoDecoder {
    private static final String TAG = "VideoDecoder";
    private MediaExtractor mMediaExtractor;
    private MediaFormat mMediaFormat;
    private MediaMuxer mMediaMuxer;
    private int mVideoMaxInputSize = 0;
    private int mVideoIndex = 0;

    /**
     *解析出文件中的video
     * @param filePath
     * @param type
     * @param outfilePath
     */
    public void muxerVideo(String filePath,int type,String outfilePath){
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(filePath);
            int numTracks = mMediaExtractor.getTrackCount();
            for (int i = 0; i < numTracks; ++i) {
                MediaFormat format = mMediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("video/")) {
                    mVideoIndex = i;
                    mMediaExtractor.selectTrack(i);
                    mMediaFormat = mMediaExtractor.getTrackFormat(i);
                    mVideoMaxInputSize = mMediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                }
            }

            mMediaMuxer = new MediaMuxer(outfilePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int trackIndex = mMediaMuxer.addTrack(mMediaFormat);
            ByteBuffer byteBuffer = ByteBuffer.allocate(mVideoMaxInputSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mMediaMuxer.start();
            long videoSampleTime;
            mMediaExtractor.readSampleData(byteBuffer, 0);
            if (mMediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC)
                mMediaExtractor.advance();
            mMediaExtractor.readSampleData(byteBuffer, 0);
            long firstVideoPTS = mMediaExtractor.getSampleTime();
            mMediaExtractor.advance();
            mMediaExtractor.readSampleData(byteBuffer, 0);
            long SecondVideoPTS = mMediaExtractor.getSampleTime();
            videoSampleTime = Math.abs(SecondVideoPTS - firstVideoPTS);
            Log.d(TAG, "videoSampleTime is " + videoSampleTime);
            mMediaExtractor.unselectTrack(mVideoIndex);
            mMediaExtractor.selectTrack(mVideoIndex);
            while (true) {
                //读取帧之间的数据
                int readSampleSize = mMediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    break;
                }
                mMediaExtractor.advance();
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mMediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs += videoSampleTime;
                //写入帧的数据
                mMediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
            }
            //release
            mMediaMuxer.stop();
            mMediaExtractor.release();
            mMediaMuxer.release();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
