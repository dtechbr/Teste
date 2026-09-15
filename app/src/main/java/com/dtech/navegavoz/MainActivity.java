package com.dtech.navegavoz;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity implements RecognitionListener {
    private static final int REQ_AUDIO = 1001;
    private WebView webView;
    private EditText address;
    private TextView status;
    private SpeechRecognizer recognizer;
    private Intent speechIntent;
    private boolean keepListening = true;
    private boolean isListening = false;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        configureWebView();
        configureVoice();
        webView.loadUrl("https://www.google.com");
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
        } else {
            startListeningSoon(600);
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(8, 21, 34));

        TextView title = new TextView(this);
        title.setText("NAVEGA VOZ");
        title.setTextColor(Color.rgb(22, 213, 227));
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setPadding(12, 14, 12, 8);
        root.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(10, 6, 10, 6);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("Digite endereço ou pesquisa");
        address.setTextColor(Color.WHITE);
        address.setHintTextColor(Color.LTGRAY);
        address.setBackgroundColor(Color.rgb(20, 50, 70));
        bar.addView(address, new LinearLayout.LayoutParams(0, 52, 1f));

        Button go = button("IR");
        go.setOnClickListener(v -> navigateFromText(address.getText().toString()));
        bar.addView(go, new LinearLayout.LayoutParams(72, 52));
        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(8, 2, 8, 6);

        Button back = button("←");
        back.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); });
        Button home = button("⌂");
        home.setOnClickListener(v -> webView.loadUrl("https://www.google.com"));
        Button refresh = button("↻");
        refresh.setOnClickListener(v -> webView.reload());
        Button mic = button("🎤");
        mic.setOnClickListener(v -> { keepListening = true; startListeningNow(); });

        controls.addView(back, new LinearLayout.LayoutParams(0, 48, 1f));
        controls.addView(home, new LinearLayout.LayoutParams(0, 48, 1f));
        controls.addView(refresh, new LinearLayout.LayoutParams(0, 48, 1f));
        controls.addView(mic, new LinearLayout.LayoutParams(0, 48, 1f));
        root.addView(controls, new LinearLayout.LayoutParams(-1, -2));

        status = new TextView(this);
        status.setText("Preparando reconhecimento de voz…");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setPadding(12, 4, 12, 8);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setAllCaps(false);
        return b;
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url);
                status.setText(keepListening ? "Pronto. Pode falar um comando." : "Reconhecimento pausado.");
            }
        });
    }

    private void configureVoice() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.setText("Reconhecimento de voz indisponível neste aparelho.");
            return;
        }
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        speechIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        speechIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        speechIntent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
    }

    private void startListeningNow() {
        if (recognizer == null || isListening || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        try {
            isListening = true;
            status.setText("Ouvindo…");
            recognizer.startListening(speechIntent);
        } catch (Exception e) {
            isListening = false;
            status.setText("Falha ao iniciar microfone. Toque no botão de voz para tentar novamente.");
        }
    }

    private void startListeningSoon(long delay) {
        handler.removeCallbacksAndMessages(null);
        if (keepListening) handler.postDelayed(this::startListeningNow, delay);
    }

    private void handleCommand(String spoken) {
        String cmd = spoken.toLowerCase(Locale.ROOT).trim();
        status.setText("Entendi: “" + spoken + "”");

        if (cmd.equals("parar de ouvir") || cmd.equals("pausar voz") || cmd.equals("parar reconhecimento")) {
            keepListening = false;
            status.setText("Reconhecimento pausado. Toque no microfone para voltar.");
            return;
        }
        if (cmd.contains("voltar")) { if (webView.canGoBack()) webView.goBack(); return; }
        if (cmd.contains("avançar") || cmd.contains("avancar")) { if (webView.canGoForward()) webView.goForward(); return; }
        if (cmd.contains("atualizar") || cmd.contains("recarregar")) { webView.reload(); return; }
        if (cmd.equals("início") || cmd.equals("inicio") || cmd.contains("abrir google")) { webView.loadUrl("https://www.google.com"); return; }
        if (cmd.contains("abrir youtube")) { webView.loadUrl("https://www.youtube.com"); return; }
        if (cmd.contains("abrir wikipedia")) { webView.loadUrl("https://pt.wikipedia.org"); return; }

        String query = null;
        if (cmd.startsWith("pesquisar por ")) query = spoken.substring(13).trim();
        else if (cmd.startsWith("pesquise por ")) query = spoken.substring(12).trim();
        else if (cmd.startsWith("buscar por ")) query = spoken.substring(10).trim();
        else if (cmd.startsWith("procure por ")) query = spoken.substring(11).trim();
        else if (cmd.startsWith("pesquisar ")) query = spoken.substring(10).trim();
        else if (cmd.startsWith("buscar ")) query = spoken.substring(7).trim();

        if (query != null && !query.isEmpty()) {
            search(query);
            return;
        }

        if (cmd.startsWith("abrir ")) {
            navigateFromText(spoken.substring(6).trim());
            return;
        }

        search(spoken);
    }

    private void navigateFromText(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return;
        if (text.startsWith("http://") || text.startsWith("https://")) {
            webView.loadUrl(text);
        } else if (text.contains(".") && !text.contains(" ")) {
            webView.loadUrl("https://" + text);
        } else {
            search(text);
        }
    }

    private void search(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        webView.loadUrl("https://www.google.com/search?q=" + encoded);
    }

    @Override public void onReadyForSpeech(Bundle params) { status.setText("Ouvindo… fale agora."); }
    @Override public void onBeginningOfSpeech() { status.setText("Reconhecendo sua voz…"); }
    @Override public void onRmsChanged(float rmsdB) { }
    @Override public void onBufferReceived(byte[] buffer) { }
    @Override public void onEndOfSpeech() { isListening = false; status.setText("Processando…"); }

    @Override
    public void onError(int error) {
        isListening = false;
        String msg;
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO: msg = "Erro no áudio"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: msg = "Permissão de microfone negada"; break;
            case SpeechRecognizer.ERROR_NETWORK: msg = "Erro de rede"; break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: msg = "Tempo de rede esgotado"; break;
            case SpeechRecognizer.ERROR_NO_MATCH: msg = "Não entendi"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: msg = "Reconhecedor ocupado"; break;
            case SpeechRecognizer.ERROR_SERVER: msg = "Serviço de voz indisponível"; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: msg = "Nenhuma fala detectada"; break;
            default: msg = "Erro de reconhecimento (" + error + ")";
        }
        status.setText(msg + ". Tentando novamente…");
        startListeningSoon(error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ? 1500 : 700);
    }

    @Override
    public void onResults(Bundle results) {
        isListening = false;
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) handleCommand(matches.get(0));
        startListeningSoon(850);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) status.setText("Ouvindo: " + matches.get(0));
    }

    @Override public void onEvent(int eventType, Bundle params) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_AUDIO && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startListeningSoon(500);
        } else {
            status.setText("Sem permissão de microfone. Navegação manual continua disponível.");
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        keepListening = false;
        handler.removeCallbacksAndMessages(null);
        if (recognizer != null) recognizer.destroy();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
