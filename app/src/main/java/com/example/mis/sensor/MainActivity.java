package com.example.mis.sensor;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.example.mis.sensor.FFT;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final float NS2S = 1.0f / 1000000000.0f;
    //example variables
    private double[] rndAccExamplevalues;
    private double[] freqCounts;
    private double[] magnitudeValues;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    //https://github.com/jjoe64/GraphView
    private GraphView accelerometerGraph;
    private LineGraphSeries<DataPoint> xLine, yLine, zLine, magnitudeLine, FFTLine;


    private Switch switchFFT;
    private SeekBar windowSizeBar;
    private SeekBar sampleRateBar;

    private int windowSize = 256; // enforce power of 2
    private int sampleRate = SensorManager.SENSOR_DELAY_NORMAL;
    private int mIndexFFT = 0;

    public float getMagnitude(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    public void toggleSeekbar(View view) {

        if (!switchFFT.isChecked()) {
            windowSizeBar.setVisibility(View.GONE);
            sampleRateBar.setVisibility(View.GONE);
        } else {
            windowSizeBar.setVisibility(View.VISIBLE);
            sampleRateBar.setVisibility(View.VISIBLE);
        }

        clearGraph(accelerometerGraph);
    }

    public void clearGraph(GraphView graph) {
        graph.removeAllSeries();
        DataPoint[] clear_array = new DataPoint[0]; // create empty point array
        xLine.resetData(clear_array);          // reset existing lines
        yLine.resetData(clear_array);
        zLine.resetData(clear_array);
        magnitudeLine.resetData(clear_array);
        FFTLine.resetData(clear_array);

        if (!switchFFT.isChecked()) {
            accelerometerGraph.addSeries(xLine);
            accelerometerGraph.addSeries(yLine);
            accelerometerGraph.addSeries(zLine);
            accelerometerGraph.addSeries(magnitudeLine);
        } else {
            accelerometerGraph.addSeries(FFTLine);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        accelerometer = null;
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // accelerometer exists
            sensorManager.registerListener((SensorEventListener) this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

            switchFFT = findViewById(R.id.switchFFT);
            windowSizeBar = findViewById(R.id.modFFT);
            sampleRateBar = findViewById(R.id.modSample);
        }
        if (!switchFFT.isChecked()) {
            windowSizeBar.setVisibility(View.GONE);
            sampleRateBar.setVisibility(View.GONE);
        }
        accelerometerGraph = findViewById(R.id.accelerometer);
        accelerometerGraph.setBackgroundColor(Color.LTGRAY);
        Viewport viewport = accelerometerGraph.getViewport();
        viewport.setScalable(true);
        viewport.setScalableY(true);
        viewport.setScrollable(true);
        viewport.setScrollableY(true);

        xLine = new LineGraphSeries<>();
        xLine.setTitle("x data");
        xLine.setColor(Color.RED);
        accelerometerGraph.addSeries(xLine);

        yLine = new LineGraphSeries<>();
        yLine.setTitle("y data");
        yLine.setColor(Color.GREEN);
        accelerometerGraph.addSeries(yLine);

        zLine = new LineGraphSeries<>();
        zLine.setTitle("z data");
        zLine.setColor(Color.BLUE);
        accelerometerGraph.addSeries(zLine);

        magnitudeLine = new LineGraphSeries<>();
        magnitudeLine.setTitle("magnitude data");
        magnitudeLine.setColor(Color.WHITE);
        accelerometerGraph.addSeries(magnitudeLine);

        FFTLine = new LineGraphSeries<>();
        FFTLine.setTitle("transformed magnitude data");
        FFTLine.setColor(Color.CYAN);
        accelerometerGraph.addSeries(FFTLine);

        windowSize = (int) Math.pow(2, windowSizeBar.getProgress() + 3);
        windowSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progress = progress + 3;
                windowSize = (int) Math.pow(2, progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                magnitudeValues = new double[windowSize];
            }
        });

        magnitudeValues = new double[windowSize];

        String delay_string = "";
        switch (sampleRateBar.getProgress()) {
            case 0:
                delay_string = "SENSOR_DELAY_FASTEST";
                break;
            case 1:
                delay_string = "SENSOR_DELAY_GAME";
                break;
            case 2:
                delay_string = "SENSOR_DELAY_NORMAL";
                break;
            case 3:
                delay_string = "SENSOR_DELAY_UI";
                break;
        }

        sampleRateBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int mProgress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mProgress = progress;
                switch (mProgress) {
                    case 0:
                        sampleRate = SensorManager.SENSOR_DELAY_FASTEST;
                        break;
                    case 1:
                        sampleRate = SensorManager.SENSOR_DELAY_GAME;
                        break;
                    case 2:
                        sampleRate = SensorManager.SENSOR_DELAY_NORMAL;
                        break;
                    case 3:
                        sampleRate = SensorManager.SENSOR_DELAY_UI;
                        break;
                }
                //Toast.makeText(getApplicationContext(), "Changing seekbar's progress to " + mProgress, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                String delay_string = "";
                switch (mProgress) {
                    case 0:
                        delay_string = "SENSOR_DELAY_FASTEST";
                        break;
                    case 1:
                        delay_string = "SENSOR_DELAY_GAME";
                        break;
                    case 2:
                        delay_string = "SENSOR_DELAY_NORMAL";
                        break;
                    case 3:
                        delay_string = "SENSOR_DELAY_UI";
                        break;
                }
                sensorManager.unregisterListener(MainActivity.this);
                sensorManager.registerListener(MainActivity.this, accelerometer, sampleRate);
            }
        });

        //initiate and fill example array with random values
        rndAccExamplevalues = new double[64];
        randomFill(rndAccExamplevalues);
        new FFTAsynctask(64).execute(rndAccExamplevalues);

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener((SensorEventListener) this, accelerometer, sampleRate);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener((SensorEventListener) this);
    }

    //https://stackoverflow.com/a/2441702
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("sampleRateBar_progress", sampleRateBar.getProgress());
        outState.putInt("windowSizeBar_progress", windowSizeBar.getProgress());
        outState.putInt("sampleRateBar_visibility", sampleRateBar.getVisibility());
        outState.putInt("mWindowSizeBar_visibility", windowSizeBar.getVisibility());
        outState.putInt("mWindowSize", windowSize);
        outState.putInt("mSampleRate", sampleRate);
        outState.putBoolean("useFFT", switchFFT.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        sampleRateBar.setProgress(savedInstanceState.getInt("mSampleRateBar_progress"));
        windowSizeBar.setProgress(savedInstanceState.getInt("mWindowSizeBar_progress"));
        sampleRateBar.setVisibility(savedInstanceState.getInt("mSampleRateBar_visibility"));
        windowSizeBar.setVisibility(savedInstanceState.getInt("mWindowSizeBar_visibility"));
        windowSize = savedInstanceState.getInt("mWindowSize");
        sampleRate = savedInstanceState.getInt("mSampleRate");
        switchFFT.setChecked(savedInstanceState.getBoolean("useFFT"));
    }

    @Override
    public void onSensorChanged(final SensorEvent event) {
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && accelerometer != null) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (!switchFFT.isChecked()) {
                        // graph: x direction time, y direction coor value
                        xLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[0]), true, 250);
                        yLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[1]), true, 250);
                        zLine.appendData(new DataPoint(event.timestamp * NS2S, event.values[2]), true, 250);
                        magnitudeLine.appendData(new DataPoint(event.timestamp * NS2S, getMagnitude(event.values[0], event.values[1], event.values[2])), true, 40);
                    } else {
                        if (mIndexFFT > windowSize) {        // reset array and index
                            magnitudeValues = new double[windowSize];
                            mIndexFFT = 0;
                        } else if (mIndexFFT < windowSize) { // add magnitude values to array
                            magnitudeValues[mIndexFFT] = getMagnitude(event.values[0], event.values[1], event.values[2]);
                        } else {                              // use fft on gathered data
                            new FFTAsynctask(windowSize).execute(magnitudeValues);
                        }
                        ++mIndexFFT;
                    }

                }
            });
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {


    }

    private class FFTAsynctask extends AsyncTask<double[], Void, double[]> {

        private int wsize; //window size must be power of 2

        // constructor to set window size
        FFTAsynctask(int wsize) {
            this.wsize = wsize;
        }

        @Override
        protected double[] doInBackground(double[]... values) {


            double[] realPart = values[0].clone(); // actual acceleration values
            double[] imagPart = new double[wsize]; // init empty

            /**
             * Init the FFT class with given window size and run it with your input.
             * The fft() function overrides the realPart and imagPart arrays!
             */
            FFT fft = new FFT(wsize);
            fft.fft(realPart, imagPart);
            //init new double array for magnitude (e.g. frequency count)
            double[] magnitude = new double[wsize];


            //fill array with magnitude values of the distribution
            for (int i = 0; wsize > i ; i++) {
                magnitude[i] = Math.sqrt(Math.pow(realPart[i], 2) + Math.pow(imagPart[i], 2));
            }

            return magnitude;

        }

        @Override
        protected void onPostExecute(double[] values) {
            //hand over values to global variable after background task is finished
            freqCounts = values;

            DataPoint[] fft_point = new DataPoint[this.wsize];
            for (int i = 0; i < this.wsize; ++i) {
                fft_point[i] = new DataPoint(i, freqCounts[i]);
            }
            FFTLine.resetData(fft_point);
        }

    }

    /**
     * little helper function to fill example with random double values
     */
    public void randomFill(double[] array){
        Random rand = new Random();
        for(int i = 0; array.length > i; i++){
            array[i] = rand.nextDouble();
        }
    }



}
