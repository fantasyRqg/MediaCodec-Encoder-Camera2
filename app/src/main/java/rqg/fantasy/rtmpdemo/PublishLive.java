package rqg.fantasy.rtmpdemo;

import android.util.Log;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.ITag;
import org.red5.io.IoConstants;
import org.red5.io.amf.Output;
import org.red5.io.flv.Tag;
import org.red5.io.object.Serializer;
import org.red5.server.net.rtmp.event.IRTMPEvent;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.event.VideoData;
import org.red5.server.stream.message.RTMPMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * *Created by rqg on 12/16/16 4:23 PM.
 */

public class PublishLive {

    private static final String TAG = "PublishLive";

    private String mPublishName = "test";
    private String mHost = "192.168.1.27";
    private int mPort = 1935;
    private String mRtmpApp = "live";
    private boolean mPublishAvaliable = false;
    private RTMPPublishClient mClient;
    private long mStartTimestamp;

    private int mWidth, mHeight;


    public void setResolution(int width, int height) {
        mWidth = width;
        mHeight = height;
    }

    public void start() {
        Log.i(TAG, "start: ");

        mClient = new RTMPPublishClient();

        mClient.setHost(mHost);
        mClient.setPort(mPort);
        mClient.setApp(mRtmpApp);
        mStartTimestamp = -1;

        new Thread("rtmp") {
            @Override
            public void run() {
                mClient.start(mPublishName, "live", null, new RTMPPublishClient.StateListener() {
                    @Override
                    public void onPublishStarted() {
                        mPublishAvaliable = true;


                        sendMetaData();
                    }

                    @Override
                    public void onPublishStop() {
                        mPublishAvaliable = false;
                    }
                });
            }
        }.start();
    }

    public void stop() {
        if (mClient != null)
            mClient.stop();

        mPublishAvaliable = false;
    }

    public boolean isAvailable() {
        return mPublishAvaliable;
    }

    public void sendMetaData() {
        Map<Object, Object> metaData = new HashMap<>();

//        ecmaArray.setProperty("duration", 0);
//        ecmaArray.setProperty("width", videoWidth);
//        ecmaArray.setProperty("height", videoHeight);
//        ecmaArray.setProperty("videodatarate", 0);
        metaData.put("duration", 0);
        metaData.put("width", mWidth);
        metaData.put("height", mHeight);
        metaData.put("videodatarate", 0);
        metaData.put("framerate", 0);
        metaData.put("audiodatarate", 0);
        metaData.put("audiosamplerate", 44100);
        metaData.put("audiosamplesize", 16);
        metaData.put("stereo", true);
        metaData.put("filesize", 0);
        metaData.put("canSeekToEnd", false);


        IoBuffer buf = IoBuffer.allocate(1024);
        buf.setAutoExpand(true);
        Output out = new Output(buf);
        // Duration property
        out.writeString("onMetaData");

        out.writeMap(metaData, new Serializer());

        ITag result = new Tag(IoConstants.TYPE_METADATA, 0, buf.limit(), null, 0);
        result.setBody(buf);

        IRTMPEvent msg = new Notify(result.getBody());

        msg.setTimestamp(result.getTimestamp());
        RTMPMessage rtmpMsg = new RTMPMessage();
        rtmpMsg.setBody(msg);

        mClient.pushMessage(rtmpMsg);
    }

    public RTMPMessage prepareMessage(long timestamp, byte[] data, int offset, int length) {
        if (data == null || !isAvailable() || length < 500)
            return null;


        if (mStartTimestamp == -1) {
            mStartTimestamp = timestamp;
        }

        IoBuffer allocate = IoBuffer.allocate(length);

        allocate.put(data, offset, length);
        allocate.flip();

        VideoData videoData = new VideoData(allocate);

        videoData.setTimestamp((int) (timestamp - mStartTimestamp));
        RTMPMessage rm = new RTMPMessage();
        rm.setBody(videoData);

        return rm;
    }

    public void publishRtmpMessage(RTMPMessage rm) {
        if (rm == null || !isAvailable())
            return;

        mClient.pushMessage(rm);

    }

}
