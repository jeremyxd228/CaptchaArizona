package com.captchatrainer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // UI
    TextView tvCaptchaCode, tvStatus, tvLog, tvPercent, tvAvgTime, tvAvgFirst, tvBestTime, tvControl;
    EditText etInput;
    Button btnN, btnPayday, btnFast, btnCam, btnStop, btnLag, btnChat, btnOldCaptcha;
    Button btnAccept, btnCancel;
    LinearLayout layoutCaptchaDialog;
    ProgressBar progressBar;

    // State
    String currentCode = "";
    boolean captchaActive = false;
    boolean waitingForFirstKey = false;
    long captchaStartTime = 0;
    long firstKeyTime = 0;
    int totalAttempts = 0;
    int correctAttempts = 0;
    double totalTime = 0;
    double totalFirstKeyTime = 0;
    double bestTime = Double.MAX_VALUE;
    CountDownTimer countDownTimer;
    Handler handler = new Handler();

    // Colors for log
    static final int COLOR_GREEN = Color.parseColor("#4EC994");
    static final int COLOR_RED   = Color.parseColor("#E05C5C");
    static final int COLOR_BLUE  = Color.parseColor("#7AB8F5");
    static final int COLOR_ORANGE= Color.parseColor("#F5A623");
    static final int COLOR_GRAY  = Color.parseColor("#AAAAAA");

    // Captcha char colors
    int[] CHAR_COLORS = {
        Color.parseColor("#E05C5C"),
        Color.parseColor("#4EC994"),
        Color.parseColor("#7AB8F5"),
        Color.parseColor("#F5A623"),
        Color.parseColor("#C77DFF"),
        Color.parseColor("#FF8C69"),
        Color.parseColor("#00CFCF"),
    };

    StringBuilder logBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        tvCaptchaCode       = findViewById(R.id.tvCaptchaCode);
        tvStatus            = findViewById(R.id.tvStatus);
        tvLog               = findViewById(R.id.tvLog);
        tvPercent           = findViewById(R.id.tvPercent);
        tvAvgTime           = findViewById(R.id.tvAvgTime);
        tvAvgFirst          = findViewById(R.id.tvAvgFirst);
        tvBestTime          = findViewById(R.id.tvBestTime);
        tvControl           = findViewById(R.id.tvControl);
        etInput             = findViewById(R.id.etInput);
        btnN                = findViewById(R.id.btnN);
        btnPayday           = findViewById(R.id.btnPayday);
        btnFast             = findViewById(R.id.btnFast);
        btnCam              = findViewById(R.id.btnCam);
        btnStop             = findViewById(R.id.btnStop);
        btnLag              = findViewById(R.id.btnLag);
        btnChat             = findViewById(R.id.btnChat);
        btnOldCaptcha       = findViewById(R.id.btnOldCaptcha);
        btnAccept           = findViewById(R.id.btnAccept);
        btnCancel           = findViewById(R.id.btnCancel);
        layoutCaptchaDialog = findViewById(R.id.layoutCaptchaDialog);
        progressBar         = findViewById(R.id.progressBar);

        tvControl.setText("Control [-.---s]");
        updateStats();

        // N button - open captcha
        btnN.setOnClickListener(v -> openCaptcha());

        // Accept button
        btnAccept.setOnClickListener(v -> submitCaptcha());

        // Cancel button
        btnCancel.setOnClickListener(v -> cancelCaptcha());

        // Enter on keyboard = Accept
        etInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE ||
               (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitCaptcha();
                return true;
            }
            return false;
        });

        // First key detection
        etInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {
                if (waitingForFirstKey && s.length() > 0) {
                    firstKeyTime = System.currentTimeMillis();
                    waitingForFirstKey = false;
                }
            }
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Other buttons - just log
        btnPayday.setOnClickListener(v -> appendLog("[CaptchaTrainer] Payday нажат", COLOR_GRAY));
        btnFast.setOnClickListener(v -> appendLog("[CaptchaTrainer] Fast нажат", COLOR_GRAY));
        btnCam.setOnClickListener(v -> appendLog("[CaptchaTrainer] Cam нажат", COLOR_GRAY));
        btnStop.setOnClickListener(v -> {
            cancelCaptcha();
            appendLog("[CaptchaTrainer] Stop — капча остановлена", COLOR_ORANGE);
        });
        btnLag.setOnClickListener(v -> appendLog("[CaptchaTrainer] Lag нажат", COLOR_GRAY));
        btnChat.setOnClickListener(v -> appendLog("[CaptchaTrainer] Chat нажат", COLOR_GRAY));
        btnOldCaptcha.setOnClickListener(v -> appendLog("[CaptchaTrainer] Old Captcha нажат", COLOR_GRAY));

        appendLog("[CaptchaTrainer] Скрипт загружен. Ваш лучший результат: —", COLOR_BLUE);
        appendLog("[CaptchaTrainer] Используйте N для активации", COLOR_BLUE);
    }

    void openCaptcha() {
        if (captchaActive) return;
        currentCode = generateCode();
        captchaActive = true;
        waitingForFirstKey = true;
        firstKeyTime = 0;
        etInput.setText("");
        etInput.setEnabled(true);
        layoutCaptchaDialog.setVisibility(View.VISIBLE);
        setCaptchaDisplay(currentCode);
        captchaStartTime = System.currentTimeMillis();
        startTimer(15000);
        etInput.requestFocus();
        appendLog("[CaptchaTrainer] Капча открыта: " + currentCode, COLOR_BLUE);
    }

    void submitCaptcha() {
        if (!captchaActive) return;
        String input = etInput.getText().toString().trim();
        if (input.isEmpty()) return;
        long elapsed = System.currentTimeMillis() - captchaStartTime;
        double sec = elapsed / 1000.0;
        double firstSec = firstKeyTime > 0 ? (firstKeyTime - captchaStartTime) / 1000.0 : sec;
        stopTimer();
        captchaActive = false;
        totalAttempts++;
        totalTime += sec;
        totalFirstKeyTime += firstSec;

        if (input.equals(currentCode)) {
            correctAttempts++;
            if (sec < bestTime) bestTime = sec;
            appendLog("[CaptchaTrainer] Капча [" + currentCode + "] введена верно за " +
                String.format("%.3f", sec) + "с", COLOR_GREEN);
            tvControl.setText("Control [" + String.format("%.3f", sec) + "s]");
        } else {
            appendLog("[CaptchaTrainer] Капча [" + currentCode + "] введена неверно (" +
                input + ") за " + String.format("%.3f", sec) + "с", COLOR_RED);
        }

        updateStats();
        closeCaptchaDialog();

        // Auto reopen after 400ms
        handler.postDelayed(this::openCaptcha, 400);
    }

    void cancelCaptcha() {
        if (!captchaActive) return;
        stopTimer();
        captchaActive = false;
        closeCaptchaDialog();
        appendLog("[CaptchaTrainer] Капча отменена", COLOR_ORANGE);
    }

    void closeCaptchaDialog() {
        layoutCaptchaDialog.setVisibility(View.GONE);
        etInput.setText("");
        etInput.setEnabled(false);
        progressBar.setProgress(0);
    }

    String generateCode() {
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(rnd.nextInt(10));
        return sb.toString();
    }

    void setCaptchaDisplay(String code) {
        SpannableString ss = new SpannableString(code);
        Random rnd = new Random();
        for (int i = 0; i < code.length(); i++) {
            int color = CHAR_COLORS[rnd.nextInt(CHAR_COLORS.length)];
            ss.setSpan(new ForegroundColorSpan(color), i, i+1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvCaptchaCode.setText(ss);
    }

    void startTimer(int ms) {
        progressBar.setMax(ms);
        progressBar.setProgress(ms);
        countDownTimer = new CountDownTimer(ms, 50) {
            public void onTick(long left) {
                progressBar.setProgress((int)left);
            }
            public void onFinish() {
                if (captchaActive) {
                    totalAttempts++;
                    appendLog("[CaptchaTrainer] Капча [" + currentCode + "] — ВРЕМЯ ВЫШЛО", COLOR_RED);
                    updateStats();
                    closeCaptchaDialog();
                    captchaActive = false;
                    handler.postDelayed(() -> openCaptcha(), 500);
                }
            }
        }.start();
    }

    void stopTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
    }

    void updateStats() {
        double pct = totalAttempts > 0 ? (correctAttempts * 100.0 / totalAttempts) : 0;
        double avg = totalAttempts > 0 ? totalTime / totalAttempts : 0;
        double avgF = totalAttempts > 0 ? totalFirstKeyTime / totalAttempts : 0;
        tvPercent.setText("Процент верных капч: " + String.format("%.0f", pct) + "%");
        tvAvgTime.setText("Средний ввод: " + String.format("%.3f", avg) + "s");
        tvAvgFirst.setText("Средний ввод первого символа: " + String.format("%.3f", avgF) + "s");
        tvBestTime.setText("Лучшее время: " + (bestTime < Double.MAX_VALUE ? String.format("%.3f", bestTime) + "s" : "—"));
    }

    void appendLog(String text, int color) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String time = sdf.format(new Date());
        String line = "[" + time + "] " + text + "\n";
        SpannableString ss = new SpannableString(line);
        ss.setSpan(new ForegroundColorSpan(color), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvLog.append(ss);
        // Scroll to bottom
        final ScrollView sv = findViewById(R.id.scrollLog);
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }
}
