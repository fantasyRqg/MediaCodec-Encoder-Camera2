package rqg.fantasy.rtmpdemo;

import android.util.Log;

import org.red5.server.messaging.IMessage;
import org.red5.server.stream.message.RTMPMessage;
import org.red5.server.stream.provider.FileProvider;

import java.io.File;

public class Publisher {
    private static final String TAG = "Publisher";

    public static void pushVideo() {
        new Thread("publish") {
            @Override
            public void run() {
                try {

                    String publishName = "test";
                    String localFile = "/sdcard/test.flv";
                    String host = "192.168.1.27";
                    int port = 1935;
                    String app = "live";

                    IMessage msg = null;
                    int timestamp = 0;
                    int lastTS = 0;

                    PublishClient client = new PublishClient();

                    client.setHost(host);
                    client.setPort(port);
                    client.setApp(app);

                    client.start(publishName, "live", null);

                    while (client.getState() != PublishClient.PUBLISHED) {
                        Thread.sleep(500);
                    }

                    FileProvider fp = new FileProvider(new File(localFile));

                    while (true) {
                        msg = fp.pullMessage(null);
                        if (msg == null) {
                            Log.d(TAG, "done !");
                            break;
                        }
                        timestamp = ((RTMPMessage) msg).getBody().getTimestamp();
                        Thread.sleep((timestamp - lastTS));
                        lastTS = timestamp;
                        client.pushMessage(msg);
                    }
                    client.stop();
                } catch (Exception e) {

                }
            }
        }.start();
    }


}
