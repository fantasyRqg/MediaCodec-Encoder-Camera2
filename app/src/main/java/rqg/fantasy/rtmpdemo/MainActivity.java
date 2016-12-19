package rqg.fantasy.rtmpdemo;

import android.Manifest;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.tbruyelle.rxpermissions.RxPermissions;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        RxPermissions rxPermissions = new RxPermissions(this);

        rxPermissions.request(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
        )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        getFragmentManager()
                                .beginTransaction()
                                .replace(R.id.container, Camera2BasicFragment.newInstance())
                                .commit();
                    }
                });

    }
}
