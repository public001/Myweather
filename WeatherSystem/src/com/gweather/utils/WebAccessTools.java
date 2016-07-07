package com.gweather.utils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class WebAccessTools {
	private static final String TAG = "Gweather.WebAccessTools";
	
	private Context context;

	public WebAccessTools(Context context) {
		this.context = context;
	}

	public String getWebContent(String url) {
		if (!Utils.isNetworkAvailable(context)) {
			Log.i(TAG, "getWebContent, Network NOT Available");
			return null;
		}

		HttpGet request = new HttpGet(url);

		HttpParams params = new BasicHttpParams();
		
		HttpConnectionParams.setConnectionTimeout(params, 6000);
		HttpConnectionParams.setSoTimeout(params, 6000);
		
		HttpClient httpClient = new DefaultHttpClient(params);
		try {
			HttpResponse response = httpClient.execute(request);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
				String content = EntityUtils.toString(response.getEntity());
				return content;
			} else {
				Log.d(TAG, "connect internet failed");
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		Log.w(TAG, "getWebContent, null");
		return null;
	}
}