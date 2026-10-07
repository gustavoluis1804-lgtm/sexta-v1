package com.gustavo.sextafeira;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONObject;
import java.io.ByteArrayInputStream;

public class MainActivity extends Activity {
    private WebView web;
    private AgentRuntime runtime;
    private boolean ready;
    private AgentRuntime.Listener listener;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        runtime=AgentRuntime.get(this);
        getWindow().setStatusBarColor(Color.rgb(9,14,12)); getWindow().setNavigationBarColor(Color.rgb(9,14,12));
        web=new WebView(this); web.setBackgroundColor(Color.rgb(9,14,12));
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setAllowFileAccess(false); web.getSettings().setAllowContentAccess(false);
        web.getSettings().setAllowFileAccessFromFileURLs(false); web.getSettings().setAllowUniversalAccessFromFileURLs(false);
        web.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        web.addJavascriptInterface(new Bridge(),"Android");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view,String url) { ready=true; runtime.refresh(); }
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request) {
                if(!request.getUrl().toString().startsWith("file:///android_asset/")) return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));
                return null;
            }
        });
        listener=event->runOnUiThread(()->{
            if(ready && !isFinishing()) web.evaluateJavascript("window.nativeEvent && window.nativeEvent("+event.toString()+")",null);
        });
        runtime.addListener(listener);
        android.widget.FrameLayout container=new android.widget.FrameLayout(this);
        container.addView(web,new android.widget.FrameLayout.LayoutParams(-1,-1));
        setContentView(container);
        // Keep content within OS insets on Android 15+ (which enables edge-to-edge by default).
        container.setOnApplyWindowInsetsListener((view,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());
                view.setPadding(bars.left,bars.top,bars.right,bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        web.loadUrl("file:///android_asset/index.html");
    }
    public class Bridge {
        @JavascriptInterface public void execute(String input) {
            runOnUiThread(()->{
                CommandParser.Command command=CommandParser.parse(input);
                if(command.needsScreen() && PhoneControlService.instance!=null) {
                    moveTaskToBack(true); runtime.handler.postDelayed(()->runtime.execute(command),450);
                } else runtime.execute(command);
            });
        }
        @JavascriptInterface public void listen() { runOnUiThread(()->startActivity(new Intent(MainActivity.this,VoiceActivity.class))); }
        @JavascriptInterface public void emotion(int index) { runOnUiThread(()->runtime.setEmotion(index)); }
        @JavascriptInterface public void voice(boolean enabled) { runOnUiThread(()->runtime.setVoice(enabled)); }
        @JavascriptInterface public void bubble(boolean enabled) { runOnUiThread(()->runtime.setBubble(enabled)); }
        @JavascriptInterface public void refresh() { runOnUiThread(()->runtime.refresh()); }
        @JavascriptInterface public void permission(String name) { runOnUiThread(()->openPermission(name)); }
        @JavascriptInterface public void stopControl() { runOnUiThread(()->runtime.execute(CommandParser.parse("desativar controle"))); }
    }
    private void openPermission(String name) {
        if(name.equals("microphone")) {
            if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));
            } else requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},100);
        } else if(name.equals("control")) {
            new AlertDialog.Builder(this).setTitle("Controle por comando")
                .setMessage("A Sexta Feira poderá ler o texto da tela, tocar, rolar e preencher campos quando você pedir. O botão flutuante chama a assistente em outros apps. O conteúdo da tela não é enviado para servidores.\n\nNa próxima tela, escolha Sexta Feira e ative o serviço. Você pode desativá-lo quando quiser.")
                .setPositiveButton("Abrir ajustes",(d,w)->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)))
                .setNegativeButton("Agora não",null).show();
        } else if(name.equals("notifications")) {
            new AlertDialog.Builder(this).setTitle("Leitura de notificações")
                .setMessage("Permite ler as notificações ativas quando você disser “ler notificações”. Não salva um histórico de notificações.\n\nNa próxima tela, selecione Sexta Feira e permita o acesso.")
                .setPositiveButton("Abrir ajustes",(d,w)->startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)))
                .setNegativeButton("Agora não",null).show();
        } else if(name.equals("app")) startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName())));
        else if(name.equals("tts")) {
            try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch(Exception error) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] result) { super.onRequestPermissionsResult(request,permissions,result); runtime.refresh(); }
    @Override public void onResume() { super.onResume(); if(web!=null) web.onResume(); if(runtime!=null) runtime.refresh(); }
    @Override public void onPause() { if(web!=null) web.onPause(); super.onPause(); }
    @Override public void onDestroy() {
        if(runtime!=null && listener!=null) runtime.removeListener(listener);
        if(web!=null) { web.removeJavascriptInterface("Android"); web.destroy(); }
        super.onDestroy();
    }
}
