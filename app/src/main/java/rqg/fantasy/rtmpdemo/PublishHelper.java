package rqg.fantasy.rtmpdemo;

import android.media.MediaCodec;
import android.media.MediaCodecList;
import android.media.MediaFormat;

import java.io.IOException;

/**
 * *Created by rqg on 12/19/16 6:47 PM.
 */

public class PublishHelper {

    public void init(int width, int height) {
        try {
            MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);

            MediaFormat mf = MediaFormat.createVideoFormat("video/avc", width, height);
            mf.setString();


            String encoderName = mediaCodecList.findEncoderForFormat();

            MediaCodec videoEncoder = MediaCodec.createByCodecName(encoderName);


            videoEncoder.configure();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
