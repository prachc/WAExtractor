package com.prach.mashup.waextractor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WAEService extends Service {
	private static final String TAG = "com.prach.mashup.WAEService";
	private Context context;
	
	@Override
	public IBinder onBind(Intent intent) {
		context = getApplicationContext();
		// TODO Auto-generated method stub
		return new WAEServiceBinder();
	}
	
	public void debug(String msg){
		Log.d(TAG,msg);
	}
	
	//public String URL = null;
	//public String[] inputs = null;
	//public int ocount = 0;
	//public String[] scripts = null;
	//public String mode = null;
	
	
	
	/*public void dofinish(boolean result_ok) {
		browser.stopLoading();
		if(result_ok){
			debug("dofinish(true);");
			Intent data = getIntent();
			data.putExtra("OUTPUTS", jsh.outputs);
			data.putExtra("NAMES", jsh.names);
			this.setResult(Activity.RESULT_OK, data);
		}else{
			debug("finish(false);");
			Intent data = getIntent();
			this.setResult(Activity.RESULT_CANCELED,data);
			data.putExtra("OUTPUTS", new String[]{});
			data.putExtra("NAMES", new String[]{});
			
		}
		if (thread != null) {
			thread.stop();
			thread = null;
		}
		finish();
		android.os.Process.killProcess(android.os.Process.myPid());
	}*/
	
	private class WAEServiceBinder extends Binder {
		public String mode;
		public String[] inputs;
		public String[] scripts;
		public String URL;
		public Bundle bundle;
		public WebView browser;
		private JSHandler jsh;
		public int pagecount = 0;
				
		public void toast(String msg){
			Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
		}
		
		public void init() {
			Looper.prepare();
			browser = new WebView(context);
			WebSettings settings = browser.getSettings();
			settings.setJavaScriptEnabled(true);
			browser.clearCache(true);
			browser.setWebChromeClient(new WebChromeClient());
			pagecount = 0;
		}
		
		public boolean loadPages() {
			jsh = new JSHandler();
			browser.addJavascriptInterface(jsh, "prach");
			//jsh.allocateOutput(ocount);

			pagecount = 0;
			browser.setWebViewClient(new WebViewClient() {
				@Override
				public void onPageFinished(WebView view, String url) {
					if (pagecount < (scripts.length - 1)) {
						debug("Page[" + pagecount + "]:finished");
						toast("Page[" + pagecount + "]:finished");
						browser.loadUrl("javascript:" + scripts[pagecount]);
						pagecount++;
					} else {
						debug("Page[" + pagecount + "]:finished (last)");
						toast("Page[" + pagecount + "]:finished (last)" );
						browser.loadUrl("javascript:" + scripts[pagecount]);
					}
				}

				@Override
				public void onPageStarted(WebView view, String url, Bitmap favicon) {
					toast("Loading[" + pagecount + "]:\n" + url);
				}
			});
			browser.loadUrl(URL);
			return true;
		}
		
		public class JSHandler {
			public boolean finished = false;
			public boolean saved = false;
			
			public String[] names = null;
			public String[] outputs = null;
			public String[] parameters = null;

			public void allocateOutput(int size) {
				debug("JSHandler.allocateOutput():size=" + size);
				outputs = new String[size];
				debug("JSHandler.outputs.length=" + outputs.length);
			}

			public void allocateParam(int size) {
				debug("JSHandler.allocateParam():size=" + size);
				parameters = new String[size];
				debug("JSHandler.parameters.length=" + parameters.length);
			}

			public void error(String str) {
				Log.e("com.prach.mashup.WAExtractor", "JSHandler.error:" + str);
			}

			public void info(String str) {
				debug("JSHandler.info:" + str);
			}

			public boolean saveOutput(String output, int index) {
				debug("saveOutput();");
				if (outputs == null || index >= outputs.length) {
					debug("JSHandler.saveOutput()=false,index false or null");
					return false;
				} else {
					outputs[index] = output;
					debug("JSHandler.saveOutput()=true,output:" + output);
					return true;
				}
			}
			
			public void addOutput(String output, String name){
				debug("addOutput(String output,String name);");
				outputs = concat(outputs,output);
				names = concat(names,name);
				debug("JSHandler.addOutput(),output:" + output+",name="+name);
			}
			
			public void addOutputArray(String[] output, String name){
				debug("addOutput(String[] output,String name);");
				outputs = concat(outputs,output);
				names = concat(names,fillarray(name,output.length));
				StringBuffer temp = new StringBuffer();
				temp.append("{");
				for (int i = 0; i < output.length; i++) {
					temp.append(output[i]);
					temp.append(",");
				}
				temp.deleteCharAt(temp.toString().length()-1);
				temp.append("}");
				debug("JSHandler.addOutput(),output:" + temp.toString()+",name="+name);
			}

			public boolean saveParam(String param, int index) {
				debug("saveParam();");
				if (parameters == null || index >= parameters.length) {
					debug("JSHandler.saveParam()=false,index false or null");
					return false;
				} else {
					parameters[index] = param;
					debug("JSHandler.saveParam()=true,param:" + param);
					return true;
				}
			}

			//don't use setfinishstate
			public void setfinishstate(String state) {
				if (state.equals("true"))
					finished = true;
				else if (state.equals("false"))
					finished = false;
				//mHandler.post(mFinished);
			}

			public void setsavestate(String state) {
				if (state.equals("true"))
					saved = true;
				else if (state.equals("false"))
					saved = false;
			}
			
			public String[] concat(String[] A, String B){
				if(A==null) 
					return new String[] {B};
				
				String[] C = new String[A.length+1];
				System.arraycopy(A, 0, C, 0, A.length);
				C[A.length] = B;
				return C;
			}
			
			public String[] concat(String[] A, String[] B) {
				if(A==null) 
					return B;
				else if(B==null) 
					return A;
				
				String[] C = new String[A.length + B.length];
				System.arraycopy(A, 0, C, 0, A.length);
				System.arraycopy(B, 0, C, A.length, B.length);
				return C;
			}
			
			public String[] fillarray(String name,int count){
				String[] output = new String[count];
				for (int i = 0; i < output.length; i++) {
					output[i] = name;
				}
				return output;
			}
		}
		
		@Override
		protected synchronized boolean onTransact(int code, Parcel data, Parcel reply,int flags) {
			if (code == 0x101) {
				//super.onResume();
				//Intent intent = getIntent();
				bundle = data.readBundle();
				mode = bundle.getString("MODE"); //EXTRACTION ONLY
				inputs = bundle.getStringArray("INPUTS");
				URL = bundle.getString("URL");
				scripts = bundle.getStringArray("SCRIPTS");
				//ocount = intent.getIntExtra("OCOUNT", 0);

				//debug("intent==null:" + (intent == null));
				//debug("t==null:" + (thread == null));
				debug("inputs==null:" + (inputs == null));
				debug("URL==null:" + (URL == null));
				debug("scripts==null:" + (scripts == null));
				debug("logic:" + (scripts != null && URL != null));

				if (mode.equals("EXTRACTION") && scripts != null && URL != null) {
					init();
					
					inputs = bundle.getStringArray("INPUTS");
					URL = bundle.getString("URL");
					scripts = bundle.getStringArray("SCRIPTS");

					Boolean finished = loadPages();
					//while(!jsh.finished);
					debug("finished");
					bundle = new Bundle();
					bundle.putStringArray("OUTPUTS", jsh.outputs);
					reply.writeBundle(bundle);
					return finished;
				}else
					return false;
			}else{
				Log.e(getClass().getSimpleName(),"Transaction code should be "+ 0x101 + ";"	+ " received instead " + code);
				return false;
			}
		}
	}
}
