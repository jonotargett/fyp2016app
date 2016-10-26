package com.fyp2099.app;


import android.app.Fragment;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.VideoView;


/**
 * Created by Jono on 18/10/2016.
 */
public class CameraFragment extends Fragment {

	VideoView vidView;
	View v;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.camera_fragment_layout, container, false);

		init();

		return v;
	}


	private void init() {
		vidView = (VideoView)v.findViewById(R.id.camera);

		//String vidAddress = "rtsp://192.168.1.159:554/11";
		String vidAddress = "http://129.127.231.179:8080/mov.mp4";
		//String vidAddress = "rtsp://129.127.231.179:8554/mov.mp4";
		//String vidAddress = "rtsp://mm2.pcslab.com/mm/7m1000.mp4";
		//String vidAddress = "rtsp://mpv.cdn3.bigCDN.com:554/bigCDN/definst/mp4:bigbuckbunnyiphone_400.mp4";
		Uri vidUri = Uri.parse(vidAddress);

		vidView.setVideoURI(vidUri);

		vidView.requestFocus();
		vidView.start();
		/*
		vidView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			public void onPrepared(MediaPlayer mp) {
				vidView.start();
			}
		});
		*/

	}
}
