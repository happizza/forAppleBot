package com.example.helloworld;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import android.app.Activity;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {

	static final int SocketServerPORT = 8080;
	static final String sendTextAddress = "192.168.0.101";
	static final String sendTextPort = "4510";

	private ServerSocket mServer;
	// private Socket mSocket;
	// recv port
	Handler mHandler = new Handler();
	TextView myip, textResponse;
	EditText editTextAddress, editTextPort;
	Button buttonConnect, buttonClear;
	String message = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Recv
		WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
		WifiInfo wifIinfo = wifiManager.getConnectionInfo();
		int address = wifIinfo.getIpAddress();
		String ipAddressStr = ((address >> 0) & 0xFF) + "."
				+ ((address >> 8) & 0xFF) + "." + ((address >> 16) & 0xFF)
				+ "." + ((address >> 24) & 0xFF);
		myip = (TextView) findViewById(R.id.myAddr);
		myip.setText(ipAddressStr);

		Thread runner = new Thread(new SocketServerThread());
		runner.start();

		// Send
		editTextAddress = (EditText) findViewById(R.id.address);
		editTextPort = (EditText) findViewById(R.id.port);
		buttonConnect = (Button) findViewById(R.id.connect);
		buttonClear = (Button) findViewById(R.id.clear);
		textResponse = (TextView) findViewById(R.id.response);

		editTextAddress.setText(sendTextAddress);
		editTextPort.setText(sendTextPort);

		buttonConnect.setOnClickListener(buttonConnectOnClickListener);

		buttonClear.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				textResponse.setText("");
			}
		});
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (mServer != null) {
			try {
				mServer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	OnClickListener buttonConnectOnClickListener = new OnClickListener() {
		// Send Action
		@Override
		public void onClick(View arg0) {
			MyClientTask myClientTask = new MyClientTask(editTextAddress
					.getText().toString(), Integer.parseInt(editTextPort
					.getText().toString()));
			myClientTask.execute();
		}
	};

	/**
	 * Send to bot method
	 */
	public class MyClientTask extends AsyncTask<Void, Void, Void> {

		String dstAddress;
		int dstPort;
		String response = "";

		MyClientTask(String addr, int port) {
			dstAddress = addr;
			dstPort = port;
		}

		@Override
		protected Void doInBackground(Void... arg0) {
			int timeout = 2000;
			Socket socket = null;

			try {
				socket = new Socket();
				InetSocketAddress sc_add = new InetSocketAddress(
						InetAddress.getByName(dstAddress), dstPort);

				// create connection
				socket.connect(sc_add, timeout);

				// check connection
				if (socket.isConnected()) {
					textResponse.setText(textResponse.getText()
							+ "\n Socket Connected!");

					// DataOutputStream out = new DataOutputStream(
					// socket.getOutputStream());
					PrintWriter pw = new PrintWriter(socket.getOutputStream(),
							true);

					// Send Message
					String sendMessage = "Message by Android";
					// out.writeUTF(sendMessage);
					pw.println(sendMessage);

				} else {
					textResponse.setText(textResponse.getText()
							+ "\n Socket cannot Connect!");
				}

				// ByteArrayOutputStream byteArrayOutputStream = new
				// ByteArrayOutputStream(
				// 1024);
				// byte[] buffer = new byte[1024];
				//
				// int bytesRead;
				// InputStream inputStream = socket.getInputStream();

				//
				// /*
				// * notice: inputStream.read() will block if no data return
				// */
				// while ((bytesRead = inputStream.read(buffer)) != -1) {
				// byteArrayOutputStream.write(buffer, 0, bytesRead);
				// response += byteArrayOutputStream.toString("UTF-8");
				// }

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				textResponse.setText(textResponse.getText()
						+ "\n UnknownHostException: " + e.toString());
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				textResponse.setText(textResponse.getText()
						+ "\n IOException: " + e.toString());
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						textResponse.setText(textResponse.getText()
								+ "\n socket close");
						// socket close
						socket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
		}
	}

	private class SocketServerThread extends Thread {

		int count = 0;

		@Override
		public void run() {
			try {
				mServer = new ServerSocket(SocketServerPORT);
				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						textResponse.setText("I'm waiting here: "
								+ mServer.getLocalPort());
					}
				});

				while (true) {
					Socket socket = mServer.accept();
					count++;
					message += "#" + count + " from " + socket.getInetAddress()
							+ ":" + socket.getPort() + "\n say: ";

					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					String request = in.readLine();
					if (request != null) {
						// save msg
						message += "[Success]" + request + "\n";
					}
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							textResponse.setText(message);
						}
					});

					SocketServerReplyThread socketServerReplyThread = new SocketServerReplyThread(
							socket, count);
					socketServerReplyThread.run();

				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				textResponse.setText(e.getMessage());
			} catch (Exception e) {
				e.printStackTrace();
				textResponse.setText(e.getMessage());
			}
		}

	}
	private class SocketServerReplyThread extends Thread {

		private Socket hostThreadSocket;
		int cnt;

		SocketServerReplyThread(Socket socket, int c) {
			hostThreadSocket = socket;
			cnt = c;
		}

		@Override
		public void run() {
			OutputStream outputStream;
			String msgReply = "Hello from Android, you are #" + cnt;

			try {
				outputStream = hostThreadSocket.getOutputStream();
				PrintStream printStream = new PrintStream(outputStream);
				printStream.print(msgReply);
				printStream.close();

				message += "replayed: " + msgReply + "\n";

				MainActivity.this.runOnUiThread(new Runnable() {

					@Override
					public void run() {
						textResponse.setText(message);
					}
				});

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				message += "Something wrong! " + e.toString() + "\n";
			}

			MainActivity.this.runOnUiThread(new Runnable() {

				@Override
				public void run() {
					textResponse.setText(message);
				}
			});
		}

	}
}