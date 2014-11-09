package com.example.helloworld;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String DEFAULT_RECV_MSG = "NO RECVICE MESSAGE";
	private static final String DEFAULT_SEND_MSG = "NO SEND MESSAGE";
	public static final String ACTION_RECEIVED = "com.example.helloworld.ACTION_RECEIVED";
	static final String sendTextAddress = "192.168.0.101";
	static final String sendTextPort = "4510";
	static final String phoneNum = "+85227802211";
	EditText editText;
	LinkedList<String> logList = new LinkedList<String>();
	TextView response;
	TextView myip;
	ReceivedBroadcastReceiver receiver = null;
	IntentFilter intentFilter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		editText = (EditText) findViewById(R.id.msg);
		response = (TextView) findViewById(R.id.response);

		// Recv
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifIinfo = wifiManager.getConnectionInfo();
		int address = wifIinfo.getIpAddress();
		String ipAddressStr = ((address >> 0) & 0xFF) + "."
				+ ((address >> 8) & 0xFF) + "." + ((address >> 16) & 0xFF)
				+ "." + ((address >> 24) & 0xFF);
		myip = (TextView) findViewById(R.id.myAddr);
		myip.setText(ipAddressStr);

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
						send(editText.getText().toString());
					}

				});

		findViewById(R.id.sendSms).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						sendSms(editText.getText().toString());
					}
				});
	}
	private void sendSms(String msg) {
		SmsManager smsManager = SmsManager.getDefault();
		if (msg == null || msg.length() == 0) {
			msg = DEFAULT_SEND_MSG;
		}
		setResponse("[Send]SMS:" + msg);
		smsManager.sendTextMessage(phoneNum, null, msg, null, null);

	}

	private void setResponse(String msg) {
		if (logList.size() >= 10) {
			logList.removeFirst();
		}
		logList.addLast(msg);
		StringBuffer sb = new StringBuffer();
		for (String alog : logList) {
			sb.append(alog + "\n");
		}
		response.setText(sb.toString());
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
				setResponse("[Recv]Server:" + values[0]);
				if (values[0].equals("@Successfully!")) {
					Toast.makeText(MainActivity.this, "连接成功!",
							Toast.LENGTH_SHORT).show();
				} else {
					sendSms(values[0]);
				}
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
			if (msg == null || msg.length() == 0) {
				msg = DEFAULT_RECV_MSG;
			}
			setResponse("[Send]Server:" + msg);
			writer.write(msg + "\n");
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	class ReceivedBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String s = intent.getStringExtra("txt");
			setResponse("[Recv]SMS:" + s);
			send(s);
			// text.setText(s);
		}
	}
}