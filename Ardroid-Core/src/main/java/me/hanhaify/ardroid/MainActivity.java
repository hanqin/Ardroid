package me.hanhaify.ardroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private TextView dataField;
    private Handler handler = new Handler();
    private MainActivity.DefaultDataLoadedListener dataLoadedListener = new DefaultDataLoadedListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        dataField = (TextView) findViewById(R.id.data_field);
        dataField.setAnimation(AnimationUtils.makeInAnimation(this, true));
        findViewById(R.id.load_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLoadingData();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void startLoadingData() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> bondedDevices = defaultAdapter.getBondedDevices();
        Optional<BluetoothDevice> device = getDevice(bondedDevices);
        if (!device.isPresent()) {
            Toast.makeText(this, "No devices available", Toast.LENGTH_LONG).show();
            return;
        }
        defaultAdapter.cancelDiscovery();
        BluetoothDevice bluetoothDevice = device.get();
        try {
            BluetoothSocket socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            new DataLoadingThread(socket, dataLoadedListener).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Optional<BluetoothDevice> getDevice(Set<BluetoothDevice> bondedDevices) {
        return Iterables.tryFind(bondedDevices, byName("Bluetooth-Slave"));
    }

    private Predicate<? super BluetoothDevice> byName(final String name) {

        return new Predicate<BluetoothDevice>() {
            @Override
            public boolean apply(BluetoothDevice bluetoothDevice) {
                return name.equalsIgnoreCase(bluetoothDevice.getName());
            }
        };
    }

    private static class DataLoadingThread extends Thread {
        private final BluetoothSocket socket;
        private DataLoadedListener dataLoadedListener;

        public DataLoadingThread(BluetoothSocket socket, DataLoadedListener dataLoadedListener) {
            this.socket = socket;
            this.dataLoadedListener = dataLoadedListener;
        }

        @Override
        public void run() {
            try {
                socket.connect();

                InputStream inputStream = socket.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                while (true) {
                    String result = bufferedReader.readLine();
                    this.dataLoadedListener.onNewData(result);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private class DefaultDataLoadedListener implements DataLoadedListener {
        @Override
        public void onNewData(final String result) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    dataField.setText(result + "\r\n" + dataField.getText());
                }
            });
        }
    }
}
