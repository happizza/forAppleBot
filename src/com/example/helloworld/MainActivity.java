package com.example.helloworld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public static final String ACTION_RECEIVED = "com.example.helloworld.ACTION_RECEIVED";
	static final String sendTextAddress = "192.168.0.101";
	static final String sendTextPort = "4510";
	static final String phoneNum = "+85227802211";
	EditText editText;
	TextView text;
	ReceivedBroadcastReceiver receiver = null;
	IntentFilter intentFilter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		editText = (EditText) findViewById(R.id.msg);
		text = (TextView) findViewById(R.id.response);

		findViewById(R.id.connect).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						connect();
					}
				});

		findViewById(R.id.send2server).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						send("send2Server");
					}

				});

		findViewById(R.id.sendSms).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sendSms("testSMS");
					}
				});
	}
	private void sendSms(String msg) {
		SmsManager smsManager = SmsManager.getDefault();
		text.append("[Send]SMS:" + msg + "\n");
		smsManager.sendTextMessage(phoneNum, null, msg, null, null);

	}

	// 定义socket,以及它的输入输出流对象
	Socket socket = null;
	BufferedWriter writer = null;
	BufferedReader reader = null;

	public void connect() {
		// 启用异步处理多线程
		AsyncTask<Void, String, Void> read = new AsyncTask<Void, String, Void>() {

			@Override
			protected Void doInBackground(Void... arg0) {
				try {
					socket = new Socket(sendTextAddress,
							Integer.parseInt(sendTextPort));
					// 获取socket的输入输出流
					writer = new BufferedWriter(new OutputStreamWriter(
							socket.getOutputStream()));
					reader = new BufferedReader(new InputStreamReader(
							socket.getInputStream()));
					publishProgress("@Successfully!");
				} catch (UnknownHostException e1) {
					Toast.makeText(MainActivity.this, "无法建立连接!",
							Toast.LENGTH_SHORT).show();
					e1.printStackTrace();
				} catch (IOException e1) {
					Toast.makeText(MainActivity.this, "无法建立连接!",
							Toast.LENGTH_SHORT).show();
					e1.printStackTrace();
				}
				try {
					String line = null;
					while ((line = reader.readLine()) != null) {
						publishProgress(line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onProgressUpdate(String... values) {
				if (values[0].equals("@Successfully!")) {
					Toast.makeText(MainActivity.this, "连接成功!",
							Toast.LENGTH_SHORT).show();
				}
				sendSms(values[0]);
				text.append("[Recv]Server:" + values[0] + "\n");
				super.onProgressUpdate(values);
			}
		};

		// 启动线程
		read.execute();
	}
	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(receiver);
	}

	@Override
	protected void onResume() {
		super.onResume();
		receiver = new ReceivedBroadcastReceiver();
		intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_RECEIVED);
		registerReceiver(receiver, intentFilter);
	}

	public void send(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
			text.append("[Send]Server:" + msg + "\n");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	class ReceivedBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String s = intent.getStringExtra("txt");
			text.append("[Recv]SMS:" + s + "\n");
			send(s);
			// text.setText(s);
		}
	}
}