package com.example.helloworld;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import android.widget.Toast;

public class MainActivity extends Activity implements Runnable {

 private ServerSocket mServer;
 private Socket mSocket;
 //recv port
 int port = 8080;
 volatile Thread runner = null;
 Handler mHandler = new Handler();
 TextView myip, textResponse;
 EditText editTextAddress, editTextPort; 
 Button buttonConnect, buttonClear;

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
   
  if(runner == null){
      runner = new Thread(this);
      runner.start();
  }
  Toast.makeText(this, "スレッドスタート", Toast.LENGTH_SHORT).show();
  
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
/**
 * Recv Server Method
 */
 @Override
 public void run() {
     try {
         mServer = new ServerSocket(port);
         mSocket = mServer.accept();
         BufferedReader in = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
         String message;
         final StringBuilder messageBuilder = new StringBuilder();
         while ((message = in.readLine()) != null){
             //save msg
             messageBuilder.append(message);
         }
         //msg post
         mHandler.post(new Runnable() {
              
             @Override
             public void run() {
                 Toast.makeText(getApplicationContext(), messageBuilder.toString(), Toast.LENGTH_SHORT).show();
             }
         });
         runner.start();
     } catch (IOException e) {
         e.printStackTrace();
     }
 }
 
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

}