package com.example.helloworld;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
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
	static final int sendTextPort = 4510;

	private ServerSocket mServer;
	// private Socket mSocket;
	// recv port   
	TextView myip,textResponse;
	EditText editTextAddress, editTextPort;
	Button buttonConnect, buttonClear;
	String message = "";
	Thread runner;
	Handler mHandler = new Handler();

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

		runner = new Thread(new SocketServerThread());
		runner.start();

		// Send
		editTextAddress = (EditText) findViewById(R.id.address);
		editTextPort = (EditText) findViewById(R.id.port);
		buttonConnect = (Button) findViewById(R.id.connect);
		buttonClear = (Button) findViewById(R.id.clear);
		textResponse = (TextView) findViewById(R.id.response);

		// editTextAddress.setText(sendTextAddress);
		// editTextPort.setText(sendTextPort);

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
		public void onClick(View v) {

			Socket socket = null;
			//request
			try {
				socket = new Socket(sendTextAddress, sendTextPort);
				socket.setSoTimeout(2000);
				if (socket.isConnected()) {
					mHandler.post(new Runnable(){
						@Override
						public void run() {
							textResponse.setText(textResponse.getText()
									+ "\n Socket Connected!");
						}
					});
				} else {
					mHandler.post(new Runnable(){
						@Override
						public void run() {
							textResponse.setText(textResponse.getText()
									+ "\n Socket cannot Connect!");
						}
					});
				}

				String sendMessage = "DataOutputStream : Message by Android<EOF>";
				DataOutputStream out = new DataOutputStream(
						socket.getOutputStream());
				out.writeBytes(sendMessage);

			} catch (UnknownHostException e) {

				e.printStackTrace();
				message += e.toString();
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						textResponse.setText(message);
					}
				});
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				message += e.toString();
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						textResponse.setText(message);
					}
				});
			}

			boolean havemsg = false;
			while(!havemsg) {
//				try{
					//Thread.sleep(1000);
					//response
					if (socket != null) {
						try {
							StringBuffer sb = new StringBuffer();
							int ch;
							while((ch = socket.getInputStream().read()) != -1){
								sb.append((char) ch);
								havemsg = true;
							}
							message += sb.toString();
							mHandler.post(new Runnable(){
								@Override
								public void run() {
									textResponse.setText(message);
								}
							});
						} catch (SocketTimeoutException e){
						} catch (IOException e) {
							message += e.toString();
							mHandler.post(new Runnable(){
								@Override
								public void run() {
									textResponse.setText(message);
								}
							});
							e.printStackTrace();
						}
					}
//				} catch (InterruptedException e1) {
//					// TODO Auto-generated catch block
//					e1.printStackTrace();
//				}
			}
			try {
				message += "\n socket close";
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						textResponse.setText(message);
					}
				});
				socket.close();
				socket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
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
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							textResponse.setText(textResponse.getText()
									+ "\n Socket Connected!");
						}
					});

					// DataOutputStream out = new DataOutputStream(
					// socket.getOutputStream());
					PrintWriter pw = new PrintWriter(socket.getOutputStream(),
							true);

					// Send Message
					String sendMessage = "Message by Android";
					// out.writeUTF(sendMessage);
					pw.println(sendMessage);

				} else {
					MainActivity.this.runOnUiThread(new Runnable() {
						@Override
						public void run() {
							textResponse.setText(textResponse.getText()
									+ "\n Socket cannot Connect!");
						}
					});
				}

			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				message += e.toString();
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						textResponse.setText(textResponse.getText()
								+ "\n UnknownHostException: " + message);
					}
				});
				e.printStackTrace();
			} catch (IOException e) {
				message += e.toString();
				// TODO Auto-generated catch block
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						textResponse.setText(textResponse.getText()
								+ "\n IOException: " + message.toString());
					}
				});
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						MainActivity.this.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								textResponse.setText(textResponse.getText()
										+ "\n socket close");
							}
						});
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
					socket.setSoTimeout(3000);

					count++;
					message += "#" + count + " from " + socket.getInetAddress()
							+ ":" + socket.getPort() + "\n say: ";

					InputStream stream = socket.getInputStream();
					byte[] buffer = new byte[1024];
					ByteArrayOutputStream bout = new ByteArrayOutputStream();
					// StringBuilder byteStr = new StringBuilder();
					// InputStreamReader reader = new InputStreamReader(stream);
					// BufferedReader in = new BufferedReader(reader);
					while (true) {
						try {
							int len = stream.read(buffer);

							// while (line != null && line.length() > 0) {
							// byteStr.append(line);
							// line = in.readLine();
							// }
							if (len < 0) {
								break;
							}
							bout.write(buffer, 0, len);
							if (len > 0) {
								byte[] data = bout.toByteArray();
								message += "[Success]" + new String(data)
										+ "\n";
							} else {
								break;
							}
						} catch (SocketTimeoutException ex) {
							message += "[Info] TimeOut\n";
							break;
						}
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
				message = e.getMessage();
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						textResponse.setText(message);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
				message = e.getMessage();
				MainActivity.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						textResponse.setText(message);
					}
				});
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