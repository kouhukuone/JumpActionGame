package jp.techacademy.kousei.asayama.jumpactiongame;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;

public class AndroidLauncher extends AndroidApplication implements ActivityRequestHandler {
	AdView mAdView;

	private final int SHOW_ADS = 1;
	private final int HIDE_ADS = 0;

	protected Handler handler = new Handler()
	{
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
				case SHOW_ADS:
				{
					mAdView.setVisibility(View.VISIBLE);
					break;
				}
				case HIDE_ADS:
				{
					mAdView.setVisibility(View.GONE);
					break;
				}
			}
		}
	};


	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		View gameView = initializeForView(new JumpActionGame(this), config);

		//initialize(new JumpActionGame(), config);

		mAdView = new AdView(this);
		mAdView.setAdSize(AdSize.BANNER);
		mAdView.setAdUnitId(getResources().getString(R.string.banner_ad_unit_id));
		mAdView.setVisibility(View.INVISIBLE);
		mAdView.setBackgroundColor(Color.BLACK);
		AdRequest adRequest = new AdRequest.Builder().build();
		mAdView.loadAd(adRequest);

		//レイアウト
		RelativeLayout layout = new RelativeLayout(this);
		layout.addView(gameView,
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		layout.addView(mAdView, params);

		setContentView(layout);
	}

	@Override
	public void showAds(boolean show){
		if(show){
			handler.sendEmptyMessage(SHOW_ADS);
		} else {
			handler.sendEmptyMessage(HIDE_ADS);
		}
	}
}
