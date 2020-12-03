package com.example.goodmorningnyc.ui.dashboard;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;


import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
import android.widget.Toast;

import com.example.goodmorningnyc.network.VolleyHelper;
import com.example.goodmorningnyc.R;

public class DashboardFragment extends Fragment implements View.OnClickListener{

    public static final String TAG = "APIFragment";
    private VolleyHelper mInstance;
    private DashboardViewModel dashboardViewModel;
    private Button mButton, mButton2;
    private final String API_KEY = "API_KEY";

    private void writeFile(String filename, String text) {
        try {
            FileOutputStream fos = getContext().openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(text.getBytes());
            fos.close();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String readFile(String filename) {
        File file = new File(getContext().getFilesDir(), filename);
        if (! file.exists()) {
            return "";
        }
        try {
            FileInputStream fin = getContext().openFileInput(filename);
            int c;
            String temp = "";
            while ((c = fin.read()) != -1) {
                temp = temp + Character.toString((char)c);
            }
            return temp;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return "";
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                ViewModelProviders.of(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView textView = root.findViewById(R.id.text_dashboard);
        dashboardViewModel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                textView.setText(s);
            }
        });

        mButton = root.findViewById(R.id.button);
        mButton.setOnClickListener(this);
        mButton2 = root.findViewById(R.id.button2);
        mButton2.setOnClickListener(this);
        return root;
    }

    public void onStart() {
        super.onStart();
        // Instantiate the RequestQueue from VolleySingleton
        mInstance = VolleyHelper.getInstance(getActivity().getApplicationContext());

        String saved_zip = readFile("zip.txt");
        if (saved_zip != "") {
            // API call, we can use the dashboardview var inside because of lifecycle
            // oncreate -> onstart
            requestWeather(saved_zip);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mInstance.getRequestQueue() != null) {
            mInstance.getRequestQueue().cancelAll(TAG);
        }
    }

    private void setText(JSONObject jsobj) throws JSONException {
        //dashboardViewModel.setText(jsobj.toString());
        String s = "";
        s += String.format("Location: %s\n", jsobj.getString("name"));
        JSONObject weather = jsobj.getJSONArray("weather").getJSONObject(0);
        s += String.format("Condition: %s\n", weather.getString("description"));
        double temp = Double.parseDouble(jsobj.getJSONObject("main").getString("temp"));
        double temp_min = Double.parseDouble(jsobj.getJSONObject("main").getString("temp_min"));
        double temp_max = Double.parseDouble(jsobj.getJSONObject("main").getString("temp_max"));
        // probably should have written helper func here
        s += String.format("Temp: %dF\n", (int) ((temp - 273.15) * 9/5 + 32));
        s += String.format("High: %dF\n", (int) ((temp_max - 273.15) * 9/5 + 32));
        s += String.format("Low: %dF\n", (int) ((temp_min - 273.15) * 9/5 + 32));
        dashboardViewModel.setText(s);
    }


    private void requestWeather(String zipcode) {
        dashboardViewModel.setText("Fetching weather stuff...");
        String url = "http://api.openweathermap.org/data/2.5/weather?zip=" + zipcode +
                "&appid=" + API_KEY;
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // get json
                        try {
                            setText(response);
                        } catch(Exception e) {
                            toast("Error... please try again");
                            Log.d("exception: ", e.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        toast("API timeout...");
                        Log.d("api response error: ", error.getMessage());
                    }
                });

        Log.d("connectApi: ", url);

        // Set the tag on the request.
        jsObjRequest.setTag(TAG);

        if (mInstance != null) {
            final JsonObjectRequest jsonObjReq = jsObjRequest;
            // add request to RequestQueue and start
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mInstance.addToRequestQueue(jsonObjReq);
                    mInstance.getRequestQueue().start();   
                }
            });

        }
    }

    public void toast(String msg) {
        Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT);
        toast.show();
    }


    @Override
    public void onClick(View view){
        if (view.getId() == R.id.button) {
            // validate weather first?
            EditText editText = (EditText) getView().findViewById(R.id.editText);
            String value = editText.getText().toString();
            if (value.matches("[0-9]{5}")) {
                requestWeather(value);
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            } else {
                Log.d("WRONG INPUT:", "IMPROPER ZIP CODE");
                toast("Improper zip code.");
            }
        } else if (view.getId() == R.id.button2) {
            EditText editText = (EditText) getView().findViewById(R.id.editText);
            String value = editText.getText().toString();
            if (value.matches("[0-9]{5}")) {
                Log.d("Action:", "WRITING TO ZIP.TXT");
                writeFile("zip.txt", value);
                toast("Saved %s as zip code.".format(value));
                //String s = readFile("zip.txt");
                //Log.d("reading string:", s);
            } else {
                Log.d("WRONG INPUT:", "IMPROPER ZIP CODE");
                toast("Improper zip code.");
            }
        }

    }
}
