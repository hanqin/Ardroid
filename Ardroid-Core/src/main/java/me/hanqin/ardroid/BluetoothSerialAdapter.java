package me.hanqin.ardroid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

import name.antonsmirnov.firmata.serial.SerialException;
import name.antonsmirnov.firmata.serial.StreamingSerialAdapter;

public class BluetoothSerialAdapter extends StreamingSerialAdapter {

    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket socket;

    public BluetoothSerialAdapter(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    @Override
    public void start() throws SerialException {
        try {
            socket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();
            setInStream(socket.getInputStream());
            setOutStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new SerialException(e);
        }
        super.start();
    }

    @Override
    public void stop() throws SerialException {
        setStopReading();

        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                throw new SerialException(e);
            }
        }
        super.stop();
    }
}
