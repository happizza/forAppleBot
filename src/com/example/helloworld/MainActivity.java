package com.example.helloworld;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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
 private ServerSocket mServer;
// private Socket mSocket;
 //recv port
 Handler mHandler = new Handler();
 TextView myip, textResponse;
 EditText editTextAddress, editTextPort; 
 Button buttonConnect, buttonClear;
 String message ="";

 @Override
 protected void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.activity_main);
  
  //Recv
  WifiManager wifiManager =  (WifiManager) getSystemService(WIFI_SERVICE);
  WifiInfo wifIinfo = wifiManager.getConnectionInfo();
  int address = wifIinfo.getIpAddress();
  String ipAddressStr = ((address >> 0) & 0xFF) + "."
          + ((address >> 8) & 0xFF) + "." + ((address >> 16) & 0xFF)
          + "." + ((address >> 24) & 0xFF);
  myip = (TextView) findViewById(R.id.myAddr);
  myip.setText(ipAddressStr);

  Thread runner = new Thread(new SocketServerThread());
  runner.start();
  
  //Send
  editTextAddress = (EditText)findViewById(R.id.address);
  editTextPort = (EditText)findViewById(R.id.port);
  buttonConnect = (Button)findViewById(R.id.connect);
  buttonClear = (Button)findViewById(R.id.clear);
  textResponse = (TextView)findViewById(R.id.response);
  
  buttonConnect.setOnClickListener(buttonConnectOnClickListener);
  
  buttonClear.setOnClickListener(new OnClickListener(){

   @Override
   public void onClick(View v) {
    textResponse.setText("");
   }});
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
// 
///**
// * Recv Server Method
// */
// @Override
// public void run() {
//     try {
//         mServer = new ServerSocket(port);
//         mSocket = mServer.accept();
//         while(true){
//        	 BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
//        	 OutputStream outputStream = mSocket.getOutputStream();
//        	 String message;
//        	 final StringBuilder messageBuilder = new StringBuilder();
//        	 while ((message = in.readLine()) != null){
//        		 //save msg
//        		 messageBuilder.append(message);
//             
//        		 outputStream.write(message.getBytes());
//        	 }
//        	 //msg post
//        	 mHandler.post(new Runnable() {
//              
//        		 @Override
//        		 public void run() {
//        			 Toast.makeText(getApplicationContext(), messageBuilder.toString(), Toast.LENGTH_SHORT).show();
//        		 }
//        	 });
//        	 
//        	 mHandler.post(new Runnable(){
//        		 @Override
//        		 public void run(){
//        			 textResponse.setText("Disconnect");
//        		 }
//        	 });
//         }
//         runner.start();
//     } catch (IOException e) {
//         e.printStackTrace();
//     }
// }
 
 OnClickListener buttonConnectOnClickListener = 
   new OnClickListener(){
	// Send Action
    @Override
    public void onClick(View arg0) {
     MyClientTask myClientTask = new MyClientTask(
       editTextAddress.getText().toString(),
       Integer.parseInt(editTextPort.getText().toString()));
     myClientTask.execute();
    }};

 /**
  * Send to bot method
  */
 public class MyClientTask extends AsyncTask<Void, Void, Void> {
  
  String dstAddress;
  int dstPort;
  String response = "";
  
  MyClientTask(String addr, int port){
   dstAddress = addr;
   dstPort = port;
  }

  @Override
  protected Void doInBackground(Void... arg0) {
   
   Socket socket = null;
   
   try {
    socket = new Socket(dstAddress, dstPort);
    
    ByteArrayOutputStream byteArrayOutputStream = 
                  new ByteArrayOutputStream(1024);
    byte[] buffer = new byte[1024];
    
    int bytesRead;
    InputStream inputStream = socket.getInputStream();
    
    /*
     * notice:
     * inputStream.read() will block if no data return
     */
             while ((bytesRead = inputStream.read(buffer)) != -1){
                 byteArrayOutputStream.write(buffer, 0, bytesRead);
                 response += byteArrayOutputStream.toString("UTF-8");
             }

   } catch (UnknownHostException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    response = "UnknownHostException: " + e.toString();
   } catch (IOException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
    response = "IOException: " + e.toString();
   }finally{
    if(socket != null){
     try {
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
   textResponse.setText(response);
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
       + ":" + socket.getPort() + "\n";

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