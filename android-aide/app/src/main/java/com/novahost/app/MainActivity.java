package com.novahost.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class MainActivity extends Activity {
    private static final int PICK_ZIP = 41;
    private final int BG = Color.rgb(9, 13, 22);
    private final int PANEL = Color.rgb(18, 26, 40);
    private final int GREEN = Color.rgb(85, 230, 165);
    private final int WHITE = Color.rgb(235, 242, 255);
    private final int MUTED = Color.rgb(135, 151, 174);

    private EditText apiField;
    private EditText keyField;
    private EditText siteNameField;
    private TextView statusText;
    private LinearLayout siteList;
    private Uri selectedZip;
    private SharedPreferences prefs;

    public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("novahost", MODE_PRIVATE);
        buildScreen();
    }

    private GradientDrawable shape(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(8);
        d.setStroke(2, stroke);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(size);
        t.setTypeface(Typeface.MONOSPACE, bold ? Typeface.BOLD : Typeface.NORMAL);
        t.setPadding(2, 5, 2, 5);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = vertical();
        root.setPadding(18, 20, 18, 24);
        scroll.addView(root);

        TextView brand = text("[ NOVAHOST ]", 25, GREEN, true);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand, lp(-1, -2));
        TextView sub = text("STATIC WEBSITE CONTROL PANEL", 11, MUTED, true);
        sub.setGravity(Gravity.CENTER);
        root.addView(sub, lp(-1, -2));

        LinearLayout config = card();
        config.addView(text("01 / SERVER CONNECTION", 13, GREEN, true));
        apiField = edit("https://novahost-api.onrender.com", "API BASE URL");
        keyField = edit("", "ADMIN API KEY");
        keyField.setInputType(129);
        config.addView(apiField, lp(-1, 48));
        config.addView(keyField, lp(-1, 48));
        Button save = button("SAVE CONNECTION");
        config.addView(save, lp(-1, 46));
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                saveSettings();
                toast("Connection settings saved");
            }
        });
        root.addView(config, lp(-1, -2));

        LinearLayout upload = card();
        upload.addView(text("02 / PUBLISH WEBSITE", 13, GREEN, true));
        siteNameField = edit("", "SITE NAME (example: my-portfolio)");
        upload.addView(siteNameField, lp(-1, 48));
        Button choose = button("CHOOSE WEBSITE ZIP");
        upload.addView(choose, lp(-1, 46));
        choose.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseZip(); }
        });
        Button publish = button("PUBLISH NOW  >");
        upload.addView(publish, lp(-1, 48));
        publish.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { publishSite(); }
        });
        root.addView(upload, lp(-1, -2));

        LinearLayout sites = card();
        LinearLayout heading = new LinearLayout(this);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        heading.addView(text("03 / YOUR WEBSITES", 13, GREEN, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button refresh = button("REFRESH");
        heading.addView(refresh, lp(-2, 42));
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { loadSites(); }
        });
        sites.addView(heading, lp(-1, -2));
        siteList = vertical();
        sites.addView(siteList, lp(-1, -2));
        root.addView(sites, lp(-1, -2));

        statusText = text("SYSTEM READY  //  CONNECT YOUR SERVER", 11, MUTED, false);
        statusText.setGravity(Gravity.CENTER);
        root.addView(statusText, lp(-1, -2));

        setContentView(scroll);
        apiField.setText(prefs.getString("api", "https://novahost-api.onrender.com"));
        keyField.setText(prefs.getString("key", ""));
        siteNameField.setText("");
        siteList.addView(text("No site list loaded yet.", 12, MUTED, false));
    }

    private LinearLayout card() {
        LinearLayout l = vertical();
        l.setPadding(13, 12, 13, 13);
        l.setBackground(shape(PANEL, Color.rgb(42, 61, 82)));
        LinearLayout.LayoutParams p = lp(-1, -2);
        p.setMargins(0, 18, 0, 0);
        l.setLayoutParams(p);
        return l;
    }

    private EditText edit(String value, String hint) {
        EditText e = new EditText(this);
        e.setSingleLine(true);
        e.setText(value);
        e.setHint(hint);
        e.setHintTextColor(MUTED);
        e.setTextColor(WHITE);
        e.setTextSize(12);
        e.setTypeface(Typeface.MONOSPACE);
        e.setPadding(12, 4, 12, 4);
        e.setBackground(shape(BG, Color.rgb(52, 72, 95)));
        return e;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(BG);
        b.setTextSize(11);
        b.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        b.setAllCaps(false);
        b.setBackground(shape(GREEN, GREEN));
        return b;
    }

    private void saveSettings() {
        prefs.edit().putString("api", apiField.getText().toString().trim())
            .putString("key", keyField.getText().toString().trim()).apply();
    }

    private String baseUrl() {
        String s = apiField.getText().toString().trim();
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private String apiKey() { return keyField.getText().toString().trim(); }

    private void chooseZip() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("application/zip");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(Intent.createChooser(i, "Select website ZIP"), PICK_ZIP); }
        catch (Exception e) { toast("File picker unavailable"); }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_ZIP && resultCode == RESULT_OK && data != null) {
            selectedZip = data.getData();
            statusText.setText("ZIP SELECTED  //  READY TO PUBLISH");
            statusText.setTextColor(GREEN);
        }
    }

    private void hideKeyboard() {
        try {
            ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(siteNameField.getWindowToken(), 0);
        } catch (Exception ignored) {}
    }

    private void publishSite() {
        saveSettings();
        String name = siteNameField.getText().toString().trim();
        if (name.length() == 0 || selectedZip == null) {
            toast("Enter a site name and choose a ZIP first");
            return;
        }
        hideKeyboard();
        new ApiTask("publish", name).execute();
    }

    private void loadSites() {
        saveSettings();
        new ApiTask("list", "").execute();
    }

    private void deleteSite(String slug) {
        new ApiTask("delete", slug).execute();
    }

    private void setStatus(String value, boolean good) {
        statusText.setText(value);
        statusText.setTextColor(good ? GREEN : Color.rgb(255, 170, 110));
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private class ApiTask extends AsyncTask<Void, Void, String> {
        private String action;
        private String value;
        private String resultError = null;

        ApiTask(String a, String v) { action = a; value = v; }

        protected void onPreExecute() {
            setStatus("WORKING...  //  PLEASE WAIT", true);
        }

        protected String doInBackground(Void... params) {
            HttpURLConnection conn = null;
            try {
                if (apiKey().length() < 20) throw new Exception("Enter your admin API key in Server Connection.");
                String endpoint;
                if ("list".equals(action)) endpoint = baseUrl() + "/api/sites";
                else if ("delete".equals(action)) endpoint = baseUrl() + "/api/sites/" + value;
                else endpoint = baseUrl() + "/api/sites";

                URL url = new URL(endpoint);
                conn = (HttpURLConnection)url.openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Authorization", "Bearer " + apiKey());

                if ("publish".equals(action)) {
                    String boundary = "----NovaHost" + UUID.randomUUID().toString().replace("-", "");
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
                    OutputStream out = conn.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                    writer.write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"name\"\r\n\r\n");
                    writer.write(value + "\r\n");
                    writer.write("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"website.zip\"\r\nContent-Type: application/zip\r\n\r\n");
                    writer.flush();
                    InputStream in = getContentResolver().openInputStream(selectedZip);
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
                    in.close();
                    out.write(("\r\n--" + boundary + "--\r\n").getBytes("UTF-8"));
                    out.flush();
                    out.close();
                } else if ("delete".equals(action)) {
                    conn.setRequestMethod("DELETE");
                } else {
                    conn.setRequestMethod("GET");
                }

                int code = conn.getResponseCode();
                InputStream response = code >= 200 && code < 400 ? conn.getInputStream() : conn.getErrorStream();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while (response != null && (n = response.read(buf)) != -1) bytes.write(buf, 0, n);
                if (response != null) response.close();
                String body = new String(bytes.toByteArray(), "UTF-8");
                if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + body);
                return body;
            } catch (Exception e) {
                resultError = e.getMessage();
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        protected void onPostExecute(String body) {
            if (resultError != null) {
                setStatus("ERROR  //  " + resultError, false);
                toast(resultError);
                return;
            }
            try {
                JSONObject json = new JSONObject(body);
                if ("publish".equals(action)) {
                    String url = json.optString("url", "");
                    setStatus("PUBLISHED  //  " + value, true);
                    toast("Published: " + url);
                    selectedZip = null;
                    loadSites();
                } else if ("delete".equals(action)) {
                    setStatus("DELETED  //  " + value, true);
                    loadSites();
                } else {
                    JSONArray sites = json.optJSONArray("sites");
                    siteList.removeAllViews();
                    if (sites == null || sites.length() == 0) {
                        siteList.addView(text("No websites published yet.", 12, MUTED, false));
                    } else {
                        for (int i = 0; i < sites.length(); i++) {
                            final JSONObject item = sites.getJSONObject(i);
                            final String slug = item.optString("slug", item.optString("name", ""));
                            final String siteUrl = item.optString("url", "");
                            LinearLayout row = vertical();
                            row.setPadding(9, 9, 9, 9);
                            row.setBackground(shape(BG, Color.rgb(42, 61, 82)));
                            LinearLayout.LayoutParams rp = lp(-1, -2);
                            rp.setMargins(0, 9, 0, 0);
                            row.setLayoutParams(rp);
                            row.addView(text("> " + slug, 13, WHITE, true));
                            row.addView(text(siteUrl, 10, MUTED, false));
                            LinearLayout actions = new LinearLayout(MainActivity.this);
                            Button open = button("OPEN");
                            Button delete = button("DELETE");
                            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, 42, 1);
                            bp.setMargins(0, 4, 5, 0);
                            actions.addView(open, bp);
                            LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(0, 42, 1);
                            dp.setMargins(5, 4, 0, 0);
                            actions.addView(delete, dp);
                            row.addView(actions, lp(-1, -2));
                            open.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    try {
                                        String target = siteUrl;
                                        if (target.length() == 0) target = baseUrl() + "/s/" + slug + "/";
                                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(target)));
                                    } catch (Exception e) { toast("Cannot open website"); }
                                }
                            });
                            delete.setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    new AlertDialog.Builder(MainActivity.this)
                                        .setTitle("Delete " + slug + "?")
                                        .setMessage("This removes the published website files.")
                                        .setNegativeButton("CANCEL", null)
                                        .setPositiveButton("DELETE", (dialog, which) -> deleteSite(slug))
                                        .show();
                                }
                            });
                            siteList.addView(row);
                        }
                    }
                    setStatus("SYNC COMPLETE  //  " + sites.length() + " WEBSITE(S)", true);
                }
            } catch (Exception e) {
                setStatus("SERVER RESPONSE ERROR", false);
                toast("Could not read server response");
            }
        }
    }
}
