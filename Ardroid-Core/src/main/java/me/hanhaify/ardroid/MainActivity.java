package me.hanhaify.ardroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import name.antonsmirnov.firmata.Firmata;
import name.antonsmirnov.firmata.IFirmata;
import name.antonsmirnov.firmata.message.StringSysexMessage;
import name.antonsmirnov.firmata.serial.SerialException;

import static java.lang.String.format;

public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private TextView dataField;
    private Handler handler = new Handler();
    private Button loadButton;
    private Button stopButton;
    private BluetoothSerialAdapter serialAdapter;
    private Timer timer;
    private Firmata firmata;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        dataField = (TextView) findViewById(R.id.data_field);
        dataField.setAnimation(AnimationUtils.makeInAnimation(this, true));
        loadButton = (Button) findViewById(R.id.load_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLoading();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopLoading();
            }
        });
    }

    private void startLoading() {
        startLoadingData();
        loadButton.setVisibility(View.GONE);
        stopButton.setVisibility(View.VISIBLE);
    }

    private void stopLoading() {
        try {
            serialAdapter.stop();
        } catch (SerialException e) {
            e.printStackTrace();
        }
        timer.cancel();
        dataField.setText("");
        loadButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);
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
            Toast.makeText(this, "No devices found", Toast.LENGTH_LONG).show();
            return;
        }
        defaultAdapter.cancelDiscovery();
        final BluetoothDevice bluetoothDevice = device.get();

        serialAdapter = new BluetoothSerialAdapter(bluetoothDevice);
        firmata = new Firmata(serialAdapter);
        firmata.addListener(new IFirmata.StubListener() {
            @Override
            public void onStringSysexMessageReceived(final StringSysexMessage message) {
                Log.e(TAG, "Message = " + message);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String[] protocol = message.getData().split(";");
                        dataField.setText(format("Humidity: %s, Temperature: %s", protocol[0], protocol[1]) + "\r\n" + dataField.getText());
                    }
                });
            }
        });
        //BluetoothSocket connect need to be on non-ui thread
        new Thread() {
            @Override
            public void run() {
                try {
                    serialAdapter.start();
                    timer = new Timer();
                    timer.schedule(new DataTask(), 3000, 1000);
                } catch (SerialException e) {
                    e.printStackTrace();
                }
            }
        }.start();
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

    private class DataTask extends TimerTask {
        @Override
        public void run() {
            try {
                StringSysexMessage message = new StringSysexMessage();
                message.setData("H;T");
                firmata.send(message);
            } catch (SerialException e) {
                e.printStackTrace();
            }
        }
    }
}
