package com.jcl.test.pushlinktest;

import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    TextView tvHelloWorld;

    final static int SYSTEM_UI_FLAGS = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    public static final String ACTION_INSTALL_COMPLETE = "com.jcl.test.pushlinktest.INSTALL_COMPLETE";

    private int ctr = 7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvHelloWorld = (TextView) findViewById(R.id.tv_hello_world);
        tvHelloWorld.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ctr == 0){
                    stopLockTask();
                }else{
                    ctr--;
                }
            }
        });

        // set the app into full screen mode
        getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAGS);

        // get policy manager
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context
                .DEVICE_POLICY_SERVICE);
        ComponentName deviceAdminReceiver = new ComponentName(this, AppAdmin.class);
        if (dpm.isDeviceOwnerApp(this.getPackageName())) {
            try {
                String[] packages = {this.getPackageName()};
                dpm.setLockTaskPackages(deviceAdminReceiver, packages);
                if (dpm.isLockTaskPermitted(this.getPackageName())) {
                    startLockTask();
                } else {
                    Log.e("app", "Lock screen is not permitted");
                }
            } catch (Exception e) {
                Log.e("app", "App is not a device administrator");
            }
        } else {
            Log.e("app", "App is not a device administrator");
        }

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Uri apkUri = (Uri) intent.getExtras().get("uri");
                //enjoy the apk uri
                //notice this will be called every 30s (more or less). You need to handle this.
                try {
                    installSilently(MainActivity.this, apkUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new IntentFilter(getPackageName() + ".pushlink.APPLY"));
    }

    public void installSilently(Context context, Uri apkUri) throws IOException {
        Log.d("MainActivity", "installSilently: " + apkUri.getPath());
        InputStream in = context.getContentResolver().openInputStream(apkUri);
        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(context.getPackageName());
        // set params
        int sessionId = packageInstaller.createSession(params);
        PackageInstaller.Session session = packageInstaller.openSession(sessionId);
        OutputStream out = session.openWrite("COSU", 0, -1);
        byte[] buffer = new byte[65536];
        int c;
        while ((c = in.read(buffer)) != -1) {
            out.write(buffer, 0, c);
        }
        session.fsync(out);
        in.close();
        out.close();

        session.commit(createIntentSender(context, sessionId));

    }

    private static IntentSender createIntentSender(Context context, int sessionId) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(ACTION_INSTALL_COMPLETE),
                0);
        return pendingIntent.getIntentSender();
    }
}
