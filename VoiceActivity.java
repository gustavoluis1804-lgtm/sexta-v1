package com.gustavo.sextafeira;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

/** A visible activity keeps microphone access in the foreground while another app is controlled. */
public class VoiceActivity extends Activity implements RecognitionListener {
    private SpeechRecognizer recognizer;
    private AgentRuntime runtime;
    private TextView status;
    private EditText input;
    private Button listen;
    private boolean offline, listening, completed, paused, destroyed;
    private int recognitionSession;
    private String targetPackage;
    private final Runnable timeout=()->{
        if(listening && !completed) { cancelRecognition(); show("Não recebi uma frase. Toque em Falar para tentar novamente, ou digite o comando."); }
    };
    private int dp(int n) { return (int)(getResources().getDisplayMetrics().density*n+0.5f); }
    private GradientDrawable background(int color,int radius) {
        GradientDrawable shape=new GradientDrawable(); shape.setColor(color); shape.setCornerRadius(dp(radius)); return shape;
    }
    private TextView text(String value,int size,int color) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setPadding(0,dp(8),0,dp(8)); return t;
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        runtime=AgentRuntime.get(this); runtime.stopSpeech();
        targetPackage=getIntent().getStringExtra("targetPackage");
        LinearLayout outer=new LinearLayout(this); outer.setOrientation(LinearLayout.VERTICAL); outer.setGravity(Gravity.BOTTOM); outer.setPadding(dp(16),dp(32),dp(16),dp(24)); outer.setFitsSystemWindows(true);
        outer.setOnClickListener(v->input.clearFocus());
        LinearLayout panel=new LinearLayout(this); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(24),dp(24),dp(24),dp(18)); panel.setBackground(background(Color.rgb(17,29,22),28));
        panel.addView(text("SEXTA FEIRA  /  VOZ",12,Color.rgb(186,246,107)));
        panel.addView(text("O que vamos fazer?",28,Color.WHITE));
        status=text("Toque em Falar e diga um comando.",15,Color.rgb(170,186,175)); panel.addView(status);
        input=new EditText(this); input.setTextColor(Color.WHITE); input.setHintTextColor(Color.rgb(136,156,144)); input.setTextSize(16); input.setHint("Ex.: toque em Pesquisar"); input.setSingleLine(false); input.setMaxLines(3); input.setPadding(dp(12),dp(14),dp(12),dp(14)); input.setBackground(background(Color.rgb(8,17,12),12));
        panel.addView(input,new LinearLayout.LayoutParams(-1,dp(86)));
        LinearLayout row=new LinearLayout(this); row.setPadding(0,dp(12),0,0);
        listen=new Button(this); listen.setText("Falar"); listen.setAllCaps(false); listen.setTextColor(Color.BLACK); listen.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(186,246,107))); listen.setOnClickListener(v->{ if(listening) cancelRecognition(); else beginPreferred(); });
        Button apply=new Button(this); apply.setText("Executar"); apply.setAllCaps(false); apply.setOnClickListener(v->submit(input.getText().toString()));
        row.addView(listen,new LinearLayout.LayoutParams(0,dp(56),1)); LinearLayout.LayoutParams next=new LinearLayout.LayoutParams(0,dp(56),1); next.leftMargin=dp(10); row.addView(apply,next); panel.addView(row);
        panel.addView(text("“Abrir WhatsApp” · “Ler tela”\n“Toque em Buscar” · “Escreva olá”",13,Color.rgb(160,176,166)));
        panel.addView(text("A escuta termina após uma frase. Você também pode usar o microfone do teclado para ditar.",12,Color.rgb(138,156,144)));
        Button close=new Button(this); close.setText("Cancelar"); close.setAllCaps(false); close.setOnClickListener(v->finish()); panel.addView(close);
        outer.addView(panel,new LinearLayout.LayoutParams(-1,-2)); setContentView(outer);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN|android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        runtime.handler.postDelayed(()->{ if(!destroyed && !paused) beginPreferred(); },350);
    }
    private void show(String message) { status.setText(message); runtime.emit("voice",message); }
    private void beginPreferred() {
        if(destroyed || paused || completed) return;
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) { requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},101); return; }
        boolean local=Build.VERSION.SDK_INT>=31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(this);
        begin(local);
    }
    private void begin(boolean local) {
        if(paused || destroyed || completed) return;
        cancelRecognition();
        ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0); input.clearFocus();
        offline=local;
        try {
            if(local && Build.VERSION.SDK_INT>=31) recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(this);
            else {
                if(!SpeechRecognizer.isRecognitionAvailable(this)) { show("Este celular não tem um serviço de voz disponível. Ative o reconhecimento de voz do Android ou dite pelo microfone do teclado."); return; }
                recognizer=SpeechRecognizer.createSpeechRecognizer(this);
            }
            final int session=++recognitionSession;
            // Callbacks from a cancelled recognizer must not affect a new listening session.
            recognizer.setRecognitionListener(new RecognitionListener() {
                private boolean valid() { return session==recognitionSession && listening && !destroyed && !paused && !completed; }
                @Override public void onReadyForSpeech(Bundle b) { if(valid()) VoiceActivity.this.onReadyForSpeech(b); }
                @Override public void onBeginningOfSpeech() { if(valid()) VoiceActivity.this.onBeginningOfSpeech(); }
                @Override public void onRmsChanged(float v) {}
                @Override public void onBufferReceived(byte[] b) {}
                @Override public void onEndOfSpeech() { if(valid()) VoiceActivity.this.onEndOfSpeech(); }
                @Override public void onError(int c) { if(valid()) VoiceActivity.this.onError(c); }
                @Override public void onResults(Bundle b) { if(valid()) VoiceActivity.this.onResults(b); }
                @Override public void onPartialResults(Bundle b) { if(valid()) VoiceActivity.this.onPartialResults(b); }
                @Override public void onEvent(int t,Bundle b) {}
            });
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"pt-BR"); intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true); intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);
            if(local) intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);
            listening=true; listen.setText("Parar escuta"); show(local?"Preparando a voz no aparelho…":"Preparando o serviço de voz do Android…");
            recognizer.startListening(intent); runtime.handler.postDelayed(timeout,16000);
        } catch(Exception error) {
            cancelRecognition();
            if(local) begin(false); else show("Não consegui iniciar a voz. Confira o microfone nas permissões do app, ou digite o comando.");
        }
    }
    private void cancelRecognition() {
        runtime.handler.removeCallbacks(timeout); listening=false; recognitionSession++;
        if(recognizer!=null) { try { recognizer.cancel(); recognizer.destroy(); } catch(Exception ignored) {} recognizer=null; }
        if(listen!=null) listen.setText("Falar");
    }
    private void submit(String value) {
        if(completed || value.trim().isEmpty()) return;
        CommandParser.Command command=CommandParser.parse(value);
        if(command.type.equals("unknown")) { cancelRecognition(); show("Não entendi o comando. Tente “abrir WhatsApp”, “ler tela” ou “fique feliz”."); return; }
        completed=true; cancelRecognition();
        runtime.emit("transcript",value);
        finish();
        runtime.handler.postDelayed(()->{
            PhoneControlService service=PhoneControlService.instance;
            if(command.needsScreen() && service!=null && targetPackage!=null && !targetPackage.isEmpty() && !targetPackage.equals(service.currentPackage())) {
                runtime.tell("A tela mudou enquanto você falava. Chame a Sexta Feira novamente no aplicativo desejado.",true); return;
            }
            runtime.execute(command);
        },450);
    }
    @Override public void onReadyForSpeech(Bundle params) { show(offline?"Ouvindo no aparelho. Diga um comando.":"Ouvindo com o serviço de voz do Android. Diga um comando."); }
    @Override public void onBeginningOfSpeech() { show("Estou ouvindo…"); }
    @Override public void onRmsChanged(float value) {}
    @Override public void onBufferReceived(byte[] bytes) {}
    @Override public void onEndOfSpeech() { if(!completed) show("Entendendo a frase…"); }
    @Override public void onEvent(int type,Bundle params) {}
    @Override public void onPartialResults(Bundle params) {
        ArrayList<String> results=params.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(results!=null && !results.isEmpty() && !completed) input.setText(results.get(0));
    }
    @Override public void onResults(Bundle params) {
        if(completed || paused || destroyed) return;
        ArrayList<String> results=params.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if(results==null || results.isEmpty()) { cancelRecognition(); show("Não ouvi uma frase. Tente de novo ou digite."); return; }
        String value=results.get(0);
        if(!CommandParser.parse(value).type.equals("cancel") && CommandParser.parse(value).type.equals("unknown")) {
            for(String candidate:results) if(!CommandParser.parse(candidate).type.equals("unknown")) { value=candidate; break; }
        }
        input.setText(value); submit(value);
    }
    @Override public void onError(int code) {
        if(completed || paused || destroyed) return;
        boolean fallback=offline && code!=SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS && code!=SpeechRecognizer.ERROR_NO_MATCH && code!=SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
        cancelRecognition();
        if(fallback) { show("A voz local não está disponível em português. Tentando o serviço do Android…"); runtime.handler.postDelayed(()->begin(false),350); return; }
        String error=code==SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS?"Permita o microfone nas configurações do aplicativo.":
            code==SpeechRecognizer.ERROR_NETWORK || code==SpeechRecognizer.ERROR_NETWORK_TIMEOUT?"O serviço de voz precisa de conexão. Verifique a internet ou dite pelo teclado.":
            code==SpeechRecognizer.ERROR_NO_MATCH || code==SpeechRecognizer.ERROR_SPEECH_TIMEOUT?"Não entendi a fala. Toque em Falar e diga uma frase curta.":
            code==SpeechRecognizer.ERROR_RECOGNIZER_BUSY?"O microfone está ocupado. Feche outro app que esteja usando a voz e tente novamente.":
            "O reconhecimento não respondeu (código "+code+"). Tente novamente ou dite pelo teclado.";
        show(error);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grants) {
        super.onRequestPermissionsResult(request,permissions,grants);
        if(request==101) {
            runtime.refresh();
            if(grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED) runtime.handler.postDelayed(this::beginPreferred,350);
            else show("Microfone não autorizado. Você pode digitar ou ditar pelo teclado.");
        }
    }
    @Override public void onResume() { super.onResume(); paused=false; }
    @Override public void onPause() { paused=true; cancelRecognition(); super.onPause(); }
    @Override public void onDestroy() { destroyed=true; cancelRecognition(); super.onDestroy(); }
}
