package com.prach.mashup.waextractor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WAEServiceAIDL extends Service {
    final RemoteCallbackList<IWAEServiceCallback> waeCallbacks
    = new RemoteCallbackList<IWAEServiceCallback>();
	
    //private IWAEServiceCallback waeCallback;
    
	private static final String TAG = "WAEServiceAIDL";
	private WebView browser;
	private JSHandler jsh;
	private int pagecount = 0;
	private Thread thread;
	private Context context;
	private String[] scripts;
	private String URL;
	//private boolean isFinished;
	
	final Handler mHandler = new Handler();
	public Runnable mFinished1 = new Runnable() {
		/*public void run(){
			try {
				waeCallback.dofinish();
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}*/
		
		public void run() { 
			final int N = waeCallbacks.beginBroadcast();
            for (int i=0; i<N; i++) {
                try {
                	//if(waeCallbacks.getBroadcastItem(i).isFinished) 
                	waeCallbacks.getBroadcastItem(i).dofinish(); 
                } catch (RemoteException e) {
                    // The RemoteCallbackList will take care of removing
                    // the dead object for us.
                }
            }
            waeCallbacks.finishBroadcast();
		} 
	};
	
	public void debug(String msg){
		Log.d(TAG,msg);
	}
	
	public void setURL(String URL){
		this.URL = URL;
	}
	
	public void setScripts(String[] scripts){
		this.scripts = scripts;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.d(TAG, "onCreate()");
	}
	@Override
	public IBinder onBind(Intent arg0) { 
		// TODO Auto-generated method stub
		return new IWAEService.Stub(){
			public String[] getOutputs(){return jsh.outputs;}
			public String[] getNames(){return jsh.names;}
			@Override
			public void startExtract(String URL,String[] scripts )throws RemoteException{
				setURL(URL);
				setScripts(scripts);
				
				debug("URL==null:" + (URL == null));
				debug("scripts==null:" + (scripts == null));
				debug("logic:" + (scripts != null && URL != null));
				
				debug("IWAEService.startExtract()");
				if (thread != null)
					thread.stop();
				init();
				loadPages();
				/*thread = new Thread() {
					public void run() {
						
					} 
				};
				thread.start();*/
			}
			@Override
			public void registerCallback(IWAEServiceCallback cb)
					throws RemoteException {
				// TODO Auto-generated method stub
				if (cb != null) waeCallbacks.register(cb);
			}
			@Override
			public void unregisterCallback(IWAEServiceCallback cb)
					throws RemoteException {
				// TODO Auto-generated method stub
				if (cb != null) waeCallbacks.unregister(cb);
			}
		};
	}
	
	public void toast(String msg){
		Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
	}
	
	public void init() {
		debug("init()");
		Looper.prepare();
		browser = new WebView(getApplicationContext());
		WebSettings settings = browser.getSettings();
		settings.setJavaScriptEnabled(true);
		browser.clearCache(true);
		browser.setWebChromeClient(new WebChromeClient());
		pagecount = 0;
		debug("init() finished");
	}
	
	public void loadPages() {
		debug("loadPages()");
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
				debug("Loading[" + pagecount + "]:\n" + url);
				toast("Loading[" + pagecount + "]:\n" + url);
			}
		});
		debug("loadUrl("+URL+")");
		browser.loadUrl(URL);
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

		public void endapp() {
			//finish();
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

		public void setfinishstate(String state) {
			if (state.equals("true"))
				finished = true;
			else if (state.equals("false"))
				finished = false;
			//mHandler.post(mFinished1);
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
} 
