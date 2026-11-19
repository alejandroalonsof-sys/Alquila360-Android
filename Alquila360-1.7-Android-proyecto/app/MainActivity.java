package com.alquila360.app;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.admin.DevicePolicyManager;
import android.view.WindowManager;
import android.print.PrintManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.os.Build;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import android.util.Base64;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends Activity {
    private static final int UNLOCK_REQUEST = 2003;
    private boolean unlocked = false;
    private boolean unlockPromptActive = false;
    private long backgroundAt = 0L;
    private static final long REAUTH_AFTER_MS = 30_000L;
    private static final int EXPORT_REQUEST = 2001;
    private static final int IMPORT_REQUEST = 2002;
    private WebView web;
    private String backupToWrite;
    private String importPassword;
    private static final int KDF_ITERATIONS = 310000;
    private static final int MAX_BACKUP_BYTES = 10 * 1024 * 1024;
    private static final SecureRandom RANDOM = new SecureRandom();
    private WebView printWeb;
    private boolean encryptedImportRequested = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        web = new WebView(this);
        web.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equalsIgnoreCase(uri.getScheme()) &&
                    "".equals(uri.getHost() == null ? "" : uri.getHost()) &&
                    uri.toString().startsWith("file:///android_asset/")) return null;
                return new WebResourceResponse("text/plain", "UTF-8", 403,
                    "Blocked", java.util.Collections.emptyMap(),
                    new java.io.ByteArrayInputStream(new byte[0]));
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"file".equalsIgnoreCase(request.getUrl().getScheme()) ||
                    !"android_asset".equals(request.getUrl().getHost()) &&
                    !request.getUrl().toString().startsWith("file:///android_asset/");
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !url.startsWith("file:///android_asset/");
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccessFromFileURLs(false);
        settings.setAllowUniversalAccessFromFileURLs(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        if (Build.VERSION.SDK_INT >= 26) settings.setSafeBrowsingEnabled(true);
        WebView.setWebContentsDebuggingEnabled(false);
        web.addJavascriptInterface(new Bridge(), "AlquilaAndroid");
        setContentView(web);
        web.setVisibility(android.view.View.INVISIBLE);
        web.loadUrl("file:///android_asset/index.html");
        requestUnlock();
    }
    private void requestUnlock() {
        if (unlockPromptActive) return;
        unlockPromptActive = true;
        unlocked = false;
        web.setVisibility(android.view.View.INVISIBLE);
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (km == null || !km.isDeviceSecure()) {
            unlocked = false;
            new android.app.AlertDialog.Builder(this)
                .setTitle("Protege tu dispositivo")
                .setMessage("Alquila360 requiere que configures un PIN, patrón o contraseña de bloqueo del teléfono antes de acceder a datos de alquileres.")
                .setCancelable(false)
                .setPositiveButton("Configurar bloqueo", (d, w) -> {
                    startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
                })
                .setNegativeButton("Salir", (d, w) -> finish()).show();
            return;
        }
        Intent intent = km.createConfirmDeviceCredentialIntent("Desbloquear Alquila360", "Confirma el bloqueo de tu teléfono para acceder a los alquileres");
        if (intent != null) startActivityForResult(intent, UNLOCK_REQUEST);
        else { unlockPromptActive = false; finish(); }
    }
    @Override protected void onStop() {
        super.onStop();
        backgroundAt = android.os.SystemClock.elapsedRealtime();
    }
    @Override protected void onResume() {
        super.onResume();
        if (unlocked && backgroundAt > 0L &&
                android.os.SystemClock.elapsedRealtime() - backgroundAt >= REAUTH_AFTER_MS) {
            requestUnlock();
        }
        backgroundAt = 0L;
    }
    private void notice(String message) { runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show()); }
    private class Bridge {
        @JavascriptInterface public void exportEncryptedBackup(String json, String password) {
            runOnUiThread(() -> {
                if (!unlocked) return;
                if (json == null || json.getBytes(StandardCharsets.UTF_8).length > MAX_BACKUP_BYTES) {
                    notice("La copia supera el límite de 10 MB"); return;
                }
                if (password == null || password.length() < 12) {
                    notice("La contraseña debe tener al menos 12 caracteres"); return;
                }
                try {
                    backupToWrite = encryptBackup(json, password);
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "alquila360-copia-cifrada.json");
                    startActivityForResult(intent, EXPORT_REQUEST);
                } catch (Exception e) { notice("No se pudo cifrar la copia"); }
            });
        }
        @JavascriptInterface public void importEncryptedBackup(String password) {
            runOnUiThread(() -> {
                if (!unlocked) return;
                if (password == null || password.isEmpty()) { notice("Introduce la contraseña"); return; }
                importPassword = password;
                encryptedImportRequested = true;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, IMPORT_REQUEST);
            });
        }
        @JavascriptInterface public void importBackup() {
            runOnUiThread(() -> {
                if (!unlocked) return;
                importPassword = null;
                encryptedImportRequested = false;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, IMPORT_REQUEST);
            });
        }
        @JavascriptInterface public void printReceipt(String html) {
            runOnUiThread(() -> {
                if (!unlocked) return;
                printWeb = new WebView(MainActivity.this);
                printWeb.getSettings().setDefaultTextEncodingName("UTF-8");
                printWeb.getSettings().setJavaScriptEnabled(false);
                printWeb.getSettings().setAllowFileAccess(false);
                printWeb.getSettings().setAllowContentAccess(false);
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
        if (request == UNLOCK_REQUEST) {
            unlockPromptActive = false;
            if (result == RESULT_OK) { unlocked = true; web.setVisibility(android.view.View.VISIBLE); }
            else finish();
            return;
        }
        if (!unlocked) return;
        if (result != RESULT_OK || data == null || data.getData() == null) {
            importPassword = null;
            encryptedImportRequested = false;
            backupToWrite = null;
            return;
        }
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
                if (encryptedImportRequested && importPassword != null) {
                    String password = importPassword;
                    importPassword = null;
                    encryptedImportRequested = false;
                    try { content = decryptBackup(content, password); }
                    catch (Exception e) { notice("Contraseña incorrecta o copia cifrada dañada"); return; }
                } else if (new JSONObject(content).optBoolean("encrypted", false)) {
                    notice("Esta copia está cifrada: usa Importar copia cifrada"); return;
                }
                web.evaluateJavascript("restoreBackupFromAndroid(" + JSONObject.quote(content) + ")", null);
            }
        } catch (Exception e) { notice("No se pudo completar la operación de archivo"); }
        finally { importPassword = null; encryptedImportRequested = false; backupToWrite = null; }
    }
    private static String encryptBackup(String plaintext, String password) throws Exception {
        byte[] salt = new byte[16], iv = new byte[12];
        RANDOM.nextBytes(salt); RANDOM.nextBytes(iv);
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, KDF_ITERATIONS, 256);
        byte[] key;
        try { key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new JSONObject().put("app", "Alquila360").put("encrypted", true)
                .put("format", 1).put("kdf", "PBKDF2-HMAC-SHA256")
                .put("iterations", KDF_ITERATIONS).put("cipher", "AES-256-GCM")
                .put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                .put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
                .put("data", Base64.encodeToString(ciphertext, Base64.NO_WRAP)).toString();
        } finally { java.util.Arrays.fill(key, (byte) 0); }
    }
    private static String decryptBackup(String wrapped, String password) throws Exception {
        JSONObject obj = new JSONObject(wrapped);
        if (!"Alquila360".equals(obj.getString("app")) || !obj.getBoolean("encrypted") ||
            obj.getInt("format") != 1 || obj.getInt("iterations") != KDF_ITERATIONS ||
            !"AES-256-GCM".equals(obj.getString("cipher")) ||
            !"PBKDF2-HMAC-SHA256".equals(obj.getString("kdf"))) throw new Exception("Formato no válido");
        byte[] salt = Base64.decode(obj.getString("salt"), Base64.DEFAULT);
        byte[] iv = Base64.decode(obj.getString("iv"), Base64.DEFAULT);
        byte[] data = Base64.decode(obj.getString("data"), Base64.DEFAULT);
        if (salt.length != 16 || iv.length != 12 || data.length > MAX_BACKUP_BYTES + 16) throw new Exception("Tamaño no válido");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, KDF_ITERATIONS, 256);
        byte[] key;
        try { key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        finally { spec.clearPassword(); }
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } finally { java.util.Arrays.fill(key, (byte) 0); }
    }
    @Override public void onBackPressed() {
        if (!unlocked) { finish(); return; }
        web.evaluateJavascript("handleAndroidBack()", result -> {
            if (!"true".equals(result)) runOnUiThread(() -> MainActivity.super.onBackPressed());
        });
    }
}
