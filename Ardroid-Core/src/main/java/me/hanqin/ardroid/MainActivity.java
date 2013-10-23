package me.hanqin.ardroid;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.achartengine.GraphicalView;
import org.achartengine.chart.CubicLineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import name.antonsmirnov.firmata.Firmata;
import name.antonsmirnov.firmata.IFirmata;
import name.antonsmirnov.firmata.message.StringSysexMessage;
import name.antonsmirnov.firmata.serial.SerialException;

import static android.graphics.Paint.Align;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.FrameLayout.LayoutParams;

public class MainActivity extends Activity {

    public static final String TAG = MainActivity.class.getSimpleName();
    public static final int PERIOD_IN_SECONDS = 1;
    private Handler handler = new Handler();
    private Button loadButton;
    private Button stopButton;
    private BluetoothSerialAdapter serialAdapter;
    private Timer timer;
    private Firmata firmata;
    private GraphicalView chartView;
    private XYSeries temperature;
    private XYSeries humidity;
    private int xCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
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
        FrameLayout container = (FrameLayout) findViewById(R.id.chart_container);
        CubicLineChart chartConfig = initCubicLineChart();

        LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        chartView = new GraphicalView(this, chartConfig);
        container.addView(chartView, 0, params);
    }

    private CubicLineChart initCubicLineChart() {
        XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
        temperature = new XYSeries("Temperature");
        humidity = new XYSeries("Humidity");
        dataSet.addSeries(temperature);
        dataSet.addSeries(humidity);

        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer(2);
        renderer.setAxisTitleTextSize(16);
        renderer.setChartTitleTextSize(20);
        renderer.setLabelsTextSize(15);
        renderer.setLegendTextSize(15);
        renderer.setPointSize(5f);
        renderer.setMargins(new int[]{20, 30, 15, 20});

        renderer.setChartTitle("Environment Condition");
        renderer.setXTitle("Time Elapsed (in seconds)");
        renderer.setYTitle("°C / %rh", 0);

        renderer.setAxesColor(Color.LTGRAY);
        renderer.setXLabels(12);
        renderer.setYLabels(10);
        renderer.setShowGrid(true);
        renderer.setXLabelsAlign(Align.RIGHT);
        renderer.setYLabelsAlign(Align.RIGHT);
        renderer.setZoomButtonsVisible(true);
        renderer.setPanLimits(new double[]{-10, 20, -10, 40});
        renderer.setZoomLimits(new double[]{-10, 20, -10, 40});
        renderer.setZoomRate(1.05f);
        renderer.setLabelsColor(Color.WHITE);
        renderer.setXLabelsColor(Color.GREEN);
        renderer.setYLabelsColor(0, Color.BLUE);
        renderer.setYLabelsColor(1, Color.YELLOW);

        renderer.setYAxisAlign(Align.RIGHT, 1);
        renderer.setYLabelsAlign(Align.LEFT, 1);

        renderer.addSeriesRenderer(getTemperatureSeriesRenderer());
        renderer.addSeriesRenderer(getHumiditySeriesRenderer());

        return new CubicLineChart(dataSet, renderer, 0.3f);
    }

    private XYSeriesRenderer getTemperatureSeriesRenderer() {
        XYSeriesRenderer renderer1 = new XYSeriesRenderer();
        renderer1.setColor(Color.BLUE);
        renderer1.setPointStyle(PointStyle.POINT);
        renderer1.setLineWidth(3);
        return renderer1;
    }

    private XYSeriesRenderer getHumiditySeriesRenderer() {
        XYSeriesRenderer renderer1 = new XYSeriesRenderer();
        renderer1.setColor(Color.YELLOW);
        renderer1.setLineWidth(3);
        renderer1.setPointStyle(PointStyle.POINT);
        return renderer1;
    }

    private void startLoading() {
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
                        xCounter++;
                        int seconds = xCounter * PERIOD_IN_SECONDS;
                        temperature.add(seconds, Double.parseDouble(protocol[0]));
                        humidity.add(seconds, Double.parseDouble(protocol[1]));
                        chartView.repaint();
                    }
                });
            }
        });
        //BluetoothSocket connect need to be on non-ui thread
        // Start with AsyncTask will result in service discovery failed exception[strange]
        new Thread() {
            @Override
            public void run() {
                try {
                    serialAdapter.start();
                } catch (SerialException e) {
                    e.printStackTrace();
                    return;
                }

                xCounter = 0;
                timer = new Timer();
                timer.schedule(new DataTask(), 100, PERIOD_IN_SECONDS * 1000);

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        loadButton.setVisibility(View.GONE);
                        stopButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        }.start();
    }

    private class DataTask extends TimerTask {
        @Override
        public void run() {
            try {
                StringSysexMessage message = new StringSysexMessage();
                message.setData("H;T");
                firmata.send(message);
                Log.e(TAG, "message sent " + message);
            } catch (SerialException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopLoading() {
        try {
            serialAdapter.stop();
        } catch (SerialException e) {
            e.printStackTrace();
        }
        temperature.clear();
        humidity.clear();
        chartView.repaint();
        if (timer != null) {
            timer.cancel();
        }
        loadButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);
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
}
