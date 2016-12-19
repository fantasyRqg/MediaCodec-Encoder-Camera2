package rqg.fantasy.rtmpdemo;

import org.red5.io.utils.ObjectMap;
import org.red5.server.messaging.IMessage;
import org.red5.server.net.rtmp.INetStreamEventHandler;
import org.red5.server.net.rtmp.RTMPClient;
import org.red5.server.net.rtmp.event.Notify;
import org.red5.server.net.rtmp.status.StatusCodes;
import org.red5.server.service.IPendingServiceCall;
import org.red5.server.service.IPendingServiceCallback;
import org.red5.server.stream.message.RTMPMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * * *Created by rqg on 12/13/16 10:40 AM.
 * <p>
 * A publish client  publish stream to server.
 */
public class RTMPPublishClient implements INetStreamEventHandler, IPendingServiceCallback {

    private static Logger log = LoggerFactory.getLogger(RTMPPublishClient.class);

    private List<IMessage> mFrameBuffer = new ArrayList<IMessage>();

    public static final int STOPPED = 0;

    public static final int CONNECTING = 1;

    public static final int STREAM_CREATING = 2;

    public static final int PUBLISHING = 3;

    public static final int PUBLISHED = 4;

    private String mHost;

    private int mPort;

    private String mApp;

    private int mState;

    private String mPublishName;

    private int mStreamId;

    private String mPublishMode;

    private RTMPClient mRTMPClient;

    private StateListener mStateListener;

    public int getState() {
        return mState;
    }

    public void setHost(String host) {
        this.mHost = host;
    }

    public void setPort(int port) {
        this.mPort = port;
    }

    public void setApp(String app) {
        this.mApp = app;
    }

    public synchronized void start(String publishName, String publishMode, Object[] params, StateListener listener) {
        mState = CONNECTING;
        this.mPublishName = publishName;
        this.mPublishMode = publishMode;
        mRTMPClient = new RTMPClient();
        mStateListener = listener;

        Map<String, Object> defParams = mRTMPClient.makeDefaultConnectionParams(mHost, mPort, mApp);
        mRTMPClient.connect(mHost, mPort, defParams, this, params);
        mRTMPClient.setConnectionClosedHandler(new Runnable() {
            @Override
            public void run() {
                if (mStateListener != null)
                    mStateListener.onPublishStop();
            }
        });
    }

    public synchronized void stop() {
        if (mState >= STREAM_CREATING) {
            mRTMPClient.disconnect();
        }
        mState = STOPPED;
    }

    synchronized public void pushMessage(IMessage message) {

        if (mState >= PUBLISHED && message instanceof RTMPMessage) {
            RTMPMessage rtmpMsg = (RTMPMessage) message;
            mRTMPClient.publishStreamData(mStreamId, rtmpMsg);
        } else {
            mFrameBuffer.add(message);
        }
    }

    @Override
    public synchronized void onStreamEvent(Notify notify) {
        log.debug("onStreamEvent: {}", notify);
        ObjectMap<?, ?> map = (ObjectMap<?, ?>) notify.getCall().getArguments()[0];
        String code = (String) map.get("code");
        log.debug("<:{}", code);
        if (StatusCodes.NS_PUBLISH_START.equals(code)) {
            mState = PUBLISHED;
            mStateListener.onPublishStarted();
            while (mFrameBuffer.size() > 0) {
                mRTMPClient.publishStreamData(mStreamId, mFrameBuffer.remove(0));
            }
        }
    }

    @Override
    public synchronized void resultReceived(IPendingServiceCall call) {
        log.debug("resultReceived:> {}", call.getServiceMethodName());
        if ("connect".equals(call.getServiceMethodName())) {
            mState = STREAM_CREATING;
            mRTMPClient.createStream(this);
        } else if ("createStream".equals(call.getServiceMethodName())) {
            mState = PUBLISHING;
            Object result = call.getResult();
            if (result instanceof Integer) {
                Integer streamIdInt = (Integer) result;
                mStreamId = streamIdInt.intValue();
                mRTMPClient.publish(streamIdInt.intValue(), mPublishName, mPublishMode, this);
            } else {
                mRTMPClient.disconnect();
                mState = STOPPED;
                mStateListener.onPublishStop();
            }
        }
    }


    public interface StateListener {
        /**
         * 所有rtmp 初始化完成，可以发送数据
         */
        void onPublishStarted();

        /**
         * create stream faile , stop publish
         */
        void onPublishStop();
    }
}
