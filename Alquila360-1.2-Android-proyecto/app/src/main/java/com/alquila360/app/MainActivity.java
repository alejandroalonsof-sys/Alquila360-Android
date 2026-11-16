package com.alquila360.app;

import android.app.Activity;
import android.print.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 2001;
    private static final int IMPORT_REQUEST = 2002;
    private WebView web;
    private String backupToWrite;
    private WebView printWeb;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        web.addJavascriptInterface(new Bridge(), "AlquilaAndroid");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }
    private void notice(String message) { runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()); }
    private class Bridge {
        @JavascriptInterface public void exportBackup(String json) {
            runOnUiThread(() -> {
                backupToWrite = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "alquila360-copia.json");
                startActivityForResult(intent, EXPORT_REQUEST);
            });
        }
        @JavascriptInterface public void importBackup() {
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, IMPORT_REQUEST);
            });
        }
        @JavascriptInterface public void printReceipt(String html) {
            runOnUiThread(() -> {
                printWeb = new WebView(MainActivity.this);
                printWeb.getSettings().setDefaultTextEncodingName("UTF-8");
                printWeb.setWebViewClient(new WebViewClient() {
                    @Override public void onPageFinished(WebView view, String url) {
                        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                        if (manager != null) manager.print("Recibo Alquila360", view.createPrintDocumentAdapter("Recibo Alquila360"), null);
                    }
                });
                printWeb.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
            });
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            if (request == EXPORT_REQUEST && backupToWrite != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    if (out == null) throw new Exception("No se pudo abrir el destino");
                    out.write(backupToWrite.getBytes(StandardCharsets.UTF_8));
                }
                backupToWrite = null;
                notice("Copia guardada correctamente");
            } else if (request == IMPORT_REQUEST) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    if (in == null) throw new Exception("No se pudo abrir el archivo");
                    byte[] buffer = new byte[8192]; int n;
                    while ((n = in.read(buffer)) != -1) {
                        bytes.write(buffer, 0, n);
                        if (bytes.size() > 10 * 1024 * 1024) throw new Exception("La copia supera 10 MB");
                    }
                }
                String content = bytes.toString("UTF-8");
                web.evaluateJavascript("restoreBackupFromAndroid(" + JSONObject.quote(content) + ")", null);
            }
        } catch (Exception e) { notice("Error: " + e.getMessage()); }
    }
    @Override public void onBackPressed() {
        web.evaluateJavascript("handleAndroidBack()", result -> {
            if (!"true".equals(result)) runOnUiThread(() -> MainActivity.super.onBackPressed());
        });
    }
}
