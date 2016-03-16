package eduxguan.imsa.powerpitch2k16;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener, Runnable {

    //int clicked = 0;
    private SensorManager senSensorManager;
    private Sensor senAccelerometer;
    private long lastUpdate = 0;
    static private double previousXAcc, previousYAcc, previousZAcc, previousXVel, previousYVel, previousZVel, previousXPos, previousYPos, previousZPos;
    private static final int SHAKE_THRESHOLD = 600;//don't know if we need this
    static private int frequency = 200;//frequency every second
    static private double timeInterval = 1/(frequency);
    private boolean running = false;
    private double lineardistance;
    private TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Below is sample FAB implementation
        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        final ImageButton button = (ImageButton) findViewById(R.id.button);
        text = (TextView) findViewById(R.id.distanceText);

        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        final Thread thread = new Thread(this);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                running = !running;
                if(distanceThing.isAlive()) {
                    try {
                        distanceThing.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if(running){
                    previousXAcc = 0;
                    previousYAcc = 0;
                    previousZAcc = 0;
                    previousXVel = 0;
                    previousYVel = 0;
                    previousZVel = 0;
                    previousXPos = 0;
                    previousYPos = 0;
                    previousZPos = 0; //above, resets the values when accelerometer is "started up" again
                    try{
                    distanceThing.start();} catch(Exception e){}//we don't know why we need the try catch, but it works

                } else {

                    calculateTotalDistance();

                }
                text.setText(previousXVel + "");//doesn't print out velocity correctly
            }

        });

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {//for something else?
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {//do we have to do something with this because its not activated when the sensor changes?
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            updateAcc(sensorEvent);

            long currentTime = System.currentTimeMillis();

            //Below, to make sure we don't use too many data
            /*if ((currentTime - lastUpdate) > 50) {
                timeInterval = (currentTime - lastUpdate);
                lastUpdate = currentTime;
            }*/
        }
    }

    static public void update() {//updates the "previous" acc, vel, pos, values to the current one. But is this method actually called?
        updateVel();
        updatePos();
    }

    static private void updatePos() {
        previousXPos += timeInterval*previousXVel;
        previousYPos += timeInterval*previousYVel;
        previousZPos += timeInterval*previousZVel;
    }

    static private void updateVel() {
        previousXVel += timeInterval*previousXAcc;
        previousYVel += timeInterval*previousYAcc;
        previousZVel += timeInterval*previousZAcc;
    }

    private void updateAcc(SensorEvent sensorEvent) {//we need to use gyroscope data in conjunction with this, because accelerating upwards, is the same as rotating the phone 90 degrees and accelerating rightwards.
        previousXAcc = sensorEvent.values[0];
        previousYAcc = sensorEvent.values[1];
        previousZAcc = sensorEvent.values[2];

        //double linearacceleration = Math.sqrt(previousXAcc* previousXAcc + previousYAcc*previousYAcc + previousZAcc*previousZAcc);
        //text.setText(previousXAcc+"x ac\n"+previousYAcc+" y acc\n"+previousZAcc+"z acc \n"+linearacceleration + " meters per second squared.");
    }

    private void calculateTotalDistance(){
        lineardistance = Math.sqrt(Math.pow(previousXPos,2)+Math.pow(previousYPos,2)+Math.pow(previousZPos,2));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    protected void onPause() {
        super.onPause();
        senSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void run() {//This doesn't work.
        long then = System.nanoTime();
        long timer = System.currentTimeMillis();
        final double nanos = 100000000.0 / frequency;
        double delta = 0;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                text.setText(lineardistance + " meters");//use whatever to display total distance onscreen
            }
        });
        while (running) {
            long now = System.nanoTime();
            delta += (now - then) / nanos;
            then = now;

            while (delta >= 1) {
                update();
                delta--;
            }

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
            }
        }
    }

    private final Thread distanceThing = new Thread( new Runnable() {
        @Override
        public void run() {
            long then = System.nanoTime();
            long timer = System.currentTimeMillis();
            final double nanos = 100000000.0 / frequency;
            double delta = 0;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    text.setText(lineardistance + " meters");//use whatever to display total distance onscreen
                }
            });
            while (running) {
                long now = System.nanoTime();
                delta += (now - then) / nanos;
                then = now;

                while (delta >= 1) {
                    update();
                    delta--;
                }

                if (System.currentTimeMillis() - timer > 1000) {
                    timer += 1000;
                }
            }
        }
    });
}
