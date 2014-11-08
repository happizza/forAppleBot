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
						send();
					}

				});

		findViewById(R.id.sendSms).setOnClickListener(
				new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						SmsManager smsManager = SmsManager.getDefault();
						smsManager.sendTextMessage(phoneNum, null, "Hello",
								null, null);
					}
				});
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
				text.append("别人说：" + values[0] + "\n");
				super.onProgressUpdate(values);
			}
		};

		// 启动线程
		read.execute();
	}

	public void send() {
		try {
			writer.write(editText.getText().toString() + "\n");
			writer.flush();
			text.append("我说:" + editText.getText().toString() + "\n");
			editText.setText("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	class ReceivedBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String s = intent.getStringExtra("txt");
			editText.setText(s);
		}
	}
}