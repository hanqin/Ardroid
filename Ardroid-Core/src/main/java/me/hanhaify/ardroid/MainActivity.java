package me.hanhaify.ardroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.Set;

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

    public void startLoadingData() {
        BluetoothAdapter defaultAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!defaultAdapter.isEnabled()) {
            Toast.makeText(this, "Please start bluetooth first", Toast.LENGTH_LONG).show();
            return;
        }
        Set<BluetoothDevice> bondedDevices = defaultAdapter.getBondedDevices();
        Optional<BluetoothDevice> device = getDevice(bondedDevices);
        if (!device.isPresent()) {
            Toast.makeText(this, "No devices available", Toast.LENGTH_LONG).show();
            return;
        }
        defaultAdapter.cancelDiscovery();
        BluetoothDevice bluetoothDevice = device.get();

        new DataLoadingThread(bluetoothDevice).start();
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

    private class DefaultDataLoadedListener implements DataLoadedListener {
        @Override
        public void onData(final String result) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    dataField.setText(result + "\r\n" + dataField.getText());
                }
            });
        }
    }

    private class DataLoadingThread extends Thread {
        private final BluetoothDevice bluetoothDevice;

        public DataLoadingThread(BluetoothDevice bluetoothDevice) {
            this.bluetoothDevice = bluetoothDevice;
        }

        @Override
        public void run() {
            final BluetoothReader bluetoothReader;
            bluetoothReader = new BluetoothReader(bluetoothDevice);
            if (!bluetoothReader.connect()) {
                Log.w(TAG, "failed to connect to bluetooth device");
                return;
            }

            while (true) {
                String data = bluetoothReader.readLine();
                Log.w(TAG, "data = " + data);
                dataLoadedListener.onData(data);
            }
        }
    }
}
