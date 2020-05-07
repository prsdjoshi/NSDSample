package com.commodity.nsdsample;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.adroitandroid.near.BuildConfig;
import com.adroitandroid.near.connect.NearConnect;
import com.adroitandroid.near.discovery.NearDiscovery;
import com.adroitandroid.near.model.Host;
import com.commodity.nsdsample.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final long DISCOVERABLE_TIMEOUT_MILLIS = 60000;
    private static final long DISCOVERY_TIMEOUT_MILLIS = 10000;
    private static final long DISCOVERABLE_PING_INTERVAL_MILLIS = 5000;
    private NearDiscovery mNearDiscovery;
    private ActivityMainBinding binding;
    private boolean mDiscovering;
    private ArrayList<Host> mParticipants;
    private ParticipantsAdapter mParticipantsAdapter;
    int i=0;
    private NearConnect mNearConnect;
    public static final String CONTENT_REQUEST_START = "start_chat";
    public static final String CONTENT_RESPONSE_DECLINE_REQUEST = "decline_request";
    public static final String CONTENT_RESPONSE_ACCEPT_REQUEST = "accept_request";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        mNearDiscovery = new NearDiscovery.Builder()
                .setContext(this)
                .setDiscoverableTimeoutMillis(DISCOVERABLE_TIMEOUT_MILLIS)
                .setDiscoveryTimeoutMillis(DISCOVERY_TIMEOUT_MILLIS)
                .setDiscoverablePingIntervalMillis(DISCOVERABLE_PING_INTERVAL_MILLIS)
                .setDiscoveryListener(getNearDiscoveryListener(), Looper.getMainLooper())
                .build();
        mNearConnect = new NearConnect.Builder()
                .fromDiscovery(mNearDiscovery)
                .setContext(this)
                .setListener(getNearConnectListener(), Looper.getMainLooper())
                .build();
        mParticipantsAdapter = new ParticipantsAdapter(new ParticipantsAdapter.Listener() {

            @Override
            public void reqToConnect(Host host) {
                Toast.makeText(MainActivity.this, "Connecting to "+host.getName(), Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(MainActivity.this,ConnectActivity.class));
                mNearConnect.send(CONTENT_REQUEST_START.getBytes(),host);
            }
        });
        binding.discoveryList.setLayoutManager(new LinearLayoutManager(this));
        binding.discoveryList.setAdapter(mParticipantsAdapter);
        if(!mDiscovering)
        {
            i++;
            mNearDiscovery.makeDiscoverable(Build.MANUFACTURER+"-"+Build.MODEL);
            startDiscovery();
            if(!mNearConnect.isReceiving())
            {
                mNearConnect.startReceiving();
            }
        }
        binding.discoveryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    if(mDiscovering)
                    {
                        stopDiscovery();
                    }
                    else {
                        mParticipantsAdapter.clearData();
                        mNearDiscovery.makeDiscoverable(Build.MANUFACTURER+"-"+Build.MODEL);
                        startDiscovery();
                        if(!mNearConnect.isReceiving())
                        {
                            mNearConnect.startReceiving();
                        }
                    }

            }
        });

    }

    NearDiscovery.Listener getNearDiscoveryListener(){
        return new NearDiscovery.Listener() {
            @Override
            public void onPeersUpdate(Set<Host> host) {
                    mParticipantsAdapter.setData(host);
//                    mParticipants = new ArrayList<>(host);
//                    String str = "";
//                    for (int i=0;i<mParticipants.size();i++)
//                    {
//                        str = str + mParticipants.get(i).getHostAddress().toString()+"-"+mParticipants.get(i).getName()+"\n";
//                    }
//                    binding.discoveryList.setText(str);

            }

            @Override
            public void onDiscoveryTimeout() {
                Toast.makeText(MainActivity.this, "No other participants found", Toast.LENGTH_SHORT).show();
                binding.discoveryPb.setVisibility(View.GONE);
                binding.discoveryBtn.setText("Start Searching");
                mDiscovering = false;
                if(!mDiscovering && i<3)
                {

                    startDiscovery();
                }
            }

            @Override
            public void onDiscoveryFailure(Throwable e) {
                Toast.makeText(MainActivity.this, "Something went wrong while searching for participants", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onDiscoverableTimeout() {
                Toast.makeText(MainActivity.this, "You're not discoverable anymore", Toast.LENGTH_LONG).show();

            }
        };
    }
    private NearConnect.Listener getNearConnectListener()
    {
        return new NearConnect.Listener() {
            @Override
            public void onReceive(byte[] bytes, final Host sender) {
                if(bytes != null)
                {
                    switch (new String(bytes))
                    {
                        case CONTENT_REQUEST_START:
                            new AlertDialog.Builder(MainActivity.this)
                                    .setMessage(sender.getName() + " would like to connect with you ")
                                    .setPositiveButton("Start", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mNearConnect.send(CONTENT_RESPONSE_ACCEPT_REQUEST.getBytes(), sender);
                                            stopNearServicesAndReceivecontents(sender);
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            mNearConnect.send(CONTENT_RESPONSE_DECLINE_REQUEST.getBytes(), sender);
                                        }
                                    }).create().show();
                            break;
                        case CONTENT_RESPONSE_ACCEPT_REQUEST:
                            stopNearServicesAndReceivecontents(sender);
                            break;
                    }
                }
            }

            @Override
            public void onSendComplete(long jobId) {

            }

            @Override
            public void onSendFailure(Throwable e, long jobId) {

            }

            @Override
            public void onStartListenFailure(Throwable e) {

            }
        };
    }
    private void stopNearServicesAndReceivecontents(Host sender) {
        mNearConnect.stopReceiving(true);
        mNearDiscovery.stopDiscovery();
        ConnectActivity.start(MainActivity.this, sender);
    }
    public void startDiscovery()
    {
        mDiscovering = true;
        mNearDiscovery.startDiscovery();
        binding.discoveryPb.setVisibility(View.VISIBLE);
        binding.discoveryList.setVisibility(View.VISIBLE);
        binding.discoveryBtn.setText("Stop Searching");
    }

    public void stopDiscovery()
    {
        mDiscovering = false;
        mNearDiscovery.stopDiscovery();
        mNearDiscovery.makeNonDiscoverable();
        binding.discoveryPb.setVisibility(View.GONE);
      //  binding.discoveryList.setVisibility(View.GONE);
        binding.discoveryBtn.setText("Start Searching");
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();

        mNearDiscovery.stopDiscovery();
        mNearConnect.stopReceiving(true);
    }
}
