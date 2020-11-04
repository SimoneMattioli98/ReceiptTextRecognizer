package com.example.frontendapplication;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;


/**class used to connect the mobile app with the server flask*/
public class Connection {
    private RequestQueue requestQueue;

    private boolean isNetworkConnected = false;

    private Snackbar snackbar;
    private Activity activity;
    private String tag;

    public Connection(Context context, View view, Activity activity, String tag) {
        requestQueue = Volley.newRequestQueue(context);
        this.activity = activity;
        snackbar = Snackbar.make(view, R.string.noConnection, Snackbar.LENGTH_INDEFINITE).setAction(R.string.settings, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //apriamo le impostazioni di rete dell'utente
                setSettingsIntent();
            }
        }); //l'ultimo è il setAction ovvero l'azione che viene assegnta la bottoncino
        this.tag = tag;
    }


    /**used to verify if there i connection or not*/
    void registerNetworkCallback(){

        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);

        if(connectivityManager != null){
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){ //check android version
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else{
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo(); //in the newest version is deprecated
                isNetworkConnected = networkInfo != null && networkInfo.isConnected();
            }
        }else{
            isNetworkConnected = false;
        }
    }

    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            isNetworkConnected = true;
            snackbar.dismiss();
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            isNetworkConnected = false;
            snackbar.show();
        }
    };

    public void setNetworkCallback(ConnectivityManager.NetworkCallback networkCallback) {
        this.networkCallback = networkCallback;
    }

    private void setSettingsIntent() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_WIRELESS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if(intent.resolveActivity(activity.getPackageManager()) != null){
            activity.startActivity(intent);
        }
    }

    boolean isConnected(){
        return this.isNetworkConnected;
    }

    public void showSnackbar(){
        this.snackbar.show();
    }

    void addRequest(Request request){
        requestQueue.add(request);
    }

    void close(){
        if(requestQueue != null){
            requestQueue.cancelAll(tag);
        }

        //clear connection control
        ConnectivityManager connectivityManager = (ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null){
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }

    public void dismissSnackbar(){
        this.snackbar.dismiss();
    }

    public void setNetworkConnected(boolean connected){
        this.isNetworkConnected = connected;
    }
}
