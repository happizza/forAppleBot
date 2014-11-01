package com.example.helloworld;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URLDecoder;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

public class ReserveWorker {
	private OkHttpClient okHttpClient;
	private static final String TAG = "MyActivity";
	Map<String,String> loginPageQueryString = new HashMap<String, String>();

	public ReserveWorker() {
	    okHttpClient = new OkHttpClient();
	    okHttpClient.setFollowSslRedirects(true);
	    
	    CookieManager cookieManager = new CookieManager();
	    CookieHandler.setDefault(cookieManager);
	    cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
	    okHttpClient.setCookieHandler(cookieManager);
	    
	}
	
	public String visitFirstPage() throws Exception{
		Request request = new Request.Builder()
	        .url("https://reserve-"+Constant.COUNTRY.toLowerCase()+".apple.com/"+Constant.COUNTRY+"/en_"+Constant.COUNTRY+"/reserve/iPhone")
	        .build();
	
	    Response response = okHttpClient.newCall(request).execute();
	    
	    String resultUrl = response.request().url().toString();
	    
	    loginPageQueryString = extractQuertString(resultUrl);
	    Log.d(TAG,"Path is " + loginPageQueryString.get("path"));
	    
	    return resultUrl;
	}
	
	public static Map<String, String> extractQuertString(String url){
		String param = url.substring(url.indexOf("?")+ 1);
		if(param.indexOf("#") > -1){
			param = param.substring(0,param.indexOf("#"));
		}
		String paramsStr[] = param.split("&");
		Map<String,String> params = new HashMap<String,String>();
		for (String str : paramsStr){
			String keyVal[] = str.split("=");
			if(keyVal.length == 2 && keyVal[0].length() > 0 && keyVal[1].length()> 0){
				params.put(keyVal[0], keyVal[1]);
				Log.d(TAG, "key:"+keyVal[0]+" value:"+keyVal[1]);
			}
		}
		return params;
	}
	
	public synchronized String loginWithCaptcha(String captchaInput, String appleId, String password) throws Exception {
		Map<String,String> params = new HashMap<String, String>();
		params.put("openiForgetInNewWindow", "ture");
		params.put("fdcBrowserData", "{\"U\":\"Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36\",\"L\":\"zh-TW\",\"Z\":\"GMT+09:00\",\"V\":\"1.1\",\"F\":\"TF1;016;;;;;;;;;;;;;;;;;;;;;;Mozilla;Netscape;5.0%20%28Windows%20NT%206.3%3B%20WOW64%29%20AppleWebKit/537.36%20%28KHTML%2C%20like%20Gecko%29%20Chrome/38.0.2125.104%20Safari/537.36;20030107;undefined;true;;true;Win32;undefined;Mozilla/5.0%20%28Windows%20NT%206.3%3B%20WOW64%29%20AppleWebKit/537.36%20%28KHTML%2C%20like%20Gecko%29%20Chrome/38.0.2125.104%20Safari/537.36;zh-TW;Big5;signin.apple.com;undefined;undefined;undefined;undefined;false;false;1414250116968;9;2005/6/7%20%u4E0B%u53489%3A33%3A44;1280;1024;;15.0;;;;;7;-540;-540;2014/10/26%20%u4E0A%u534812%3A15%3A16;24;1280;984;0;0;;;;;;Shockwave%20Flash%7CShockwave%20Flash%2015.0%20r0;;;;;;;;;;;;;18;;;;;;;\"}");

        params.put("appleId", appleId);
        params.put("accountPassword", password);
        params.put("captchaInput", captchaInput);
        params.put("captchaAudioInput", "");
        params.put("appIdKey", "db0114b11bdc2a139e5adff448a1d7325febef288258f0dc131d6ee9afe63df3");
        params.put("language", Constant.COUNTRY+"-EN");
        String path = loginPageQueryString.get("path");
        String first = path.substring(0, 48);
        String second = path.substring(49);
        String testpath = first+"2"+second;
        params.put("path", URLDecoder.decode(path));
        params.put("rv", "4");
        params.put("sslEnabled", "true");
        params.put("Env", "PROD");
        params.put("captchaType", "image");
        params.put("captchaToken", "");

        Log.d(Constant.TAG, "path    :"+loginPageQueryString.get("path"));
        Log.d(Constant.TAG, "testpath:"+testpath);
		FormEncodingBuilder builder = new FormEncodingBuilder();
		for(String key: params.keySet()){
			builder.add(key, params.get(key));
		}
		RequestBody formBody = builder.build();
		Request request = new Request.Builder()
		        .url("https://signin.apple.com/IDMSWebAuth/authenticate")
		        .post(formBody)
		        .build();
		Response response = okHttpClient.newCall(request).execute();
		
		String resultUrl = response.request().urlString().toString();
		return resultUrl;
	
	}

}
