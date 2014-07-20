package com.prach.mashup.waextractor;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class WAExtractor extends Activity {
	private WebView browser = null;
	private JSHandler jsh;
	public int pagecount = 0;
	
	public String URL = null;
	public String[] inputs = null;
	//public int ocount = 0;
	public String[] scripts = null;
	public String mode = null;
	
	private Thread thread;
	final Handler mHandler = new Handler();
	public Runnable mFinished1 = new Runnable() {
		public void run() {
			dofinish(true);
		}
	};
	
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
			finish();
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
			mHandler.post(mFinished1);
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

	public void debug(String msg){
		Log.d("com.prach.mashup.WAExtractor",msg);
	}
	
	public void toast(String msg){
		Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
	}
	
	public void dofinish(boolean result_ok) {
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
	}

	public void init() {
		browser = (WebView) findViewById(R.id.webview);
		WebSettings settings = browser.getSettings();
		settings.setUserAgentString("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.220 Safari/535.1");
		settings.setJavaScriptEnabled(true);
		browser.clearCache(true);
		browser.setWebChromeClient(new WebChromeClient());
		pagecount = 0;
		if (thread != null) {
			thread.stop();
			thread = null;
		}
	}

	public void initScreen() {
		debug( "initScreen();");
		pagecount = 0;
		browser.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				debug( "Loading[" + pagecount+ "]:\n" + url);
			}
		});
		StringBuffer htmlcode = new StringBuffer();
		htmlcode.append(
				"<html><body>" +
				"WAExtractor:<br>" +
				"This application cannot be run in stand-alone mode, Intent call required" +
				"</body></html>");
		browser.loadData(htmlcode.toString(), "text/html", "UTF-8");
	}
	
	public void loadHtml(String source){
		pagecount = 0;
		browser.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				debug( "Loading[" + pagecount+ "]:\n" + url);
			}
		});
		/*StringBuffer htmlcode = new StringBuffer();
		htmlcode.append(
				"<html><body>" +
				"WAExtractor:<br>" +
				"This application cannot be run in stand-alone mode, Intent call required" +
				"</body></html>");*/
		
		if(source == null)
			source = "<html><body>" +
			"WAExtractor:<br>" +
			"&ldquo;HTML_SOURCE&rdquo;=NULL" +
			"</body></html>";
		else
			browser.loadData(source, "text/html", "utf-8");
	}

	public void loadPages() {
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
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			dofinish(false);
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		
		mode = intent.getStringExtra("MODE");
		if(mode==null) 
			mode = "INIT";
		inputs = intent.getStringArrayExtra("INPUTS");
		URL = intent.getStringExtra("URL");
		scripts = intent.getStringArrayExtra("SCRIPTS");
		//ocount = intent.getIntExtra("OCOUNT", 0);

		debug("intent==null:" + (intent == null));
		debug("t==null:" + (thread == null));
		debug("inputs==null:" + (inputs == null));
		debug("URL==null:" + (URL == null));
		debug("scripts==null:" + (scripts == null));
		//debug("ocount==0:" + (ocount == 0));
		debug("logic:" + (intent != null && scripts != null && URL != null));

		if (mode.equals("EXTRACTION") && intent != null && scripts != null && URL != null) {
			debug("onResume():external intent call");
			if (thread != null)
				thread.stop();
			init();
			
			inputs = intent.getStringArrayExtra("INPUTS");
			URL = intent.getStringExtra("URL");
			scripts = intent.getStringArrayExtra("SCRIPTS");
			//ocount = intent.getIntExtra("OCOUNT", 1);
			
			thread = new Thread() {
				public void run() {
					loadPages();
				}
			};
			thread.start();
			
		} else if (mode.equals("DISPLAY") && intent != null){
			debug( "onResume():display");
			init();
			loadHtml(intent.getStringExtra("HTML_SOURCE"));
			
		}else if (mode.equals("INIT")){
			debug( "onResume():fresh start");
			init();
			initScreen();
		}
	}
	
	@Override
	protected void onPause(){
		debug("onPause()");
		super.onPause();
		dofinish(false);
	}
}