package com.jcl.test.pushlinktest;

import android.app.Application;
import android.provider.Settings;

import com.pushlink.android.PushLink;
import com.pushlink.android.StrategyEnum;

public class PushLinkSetup extends Application {
    @Override
	public void onCreate() {
		super.onCreate();
        String androidId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
		PushLink.start(this, R.mipmap.ic_launcher, "dqsrloh0haa5s2k9", androidId);
		PushLink.setCurrentStrategy(StrategyEnum.CUSTOM);
	}


}