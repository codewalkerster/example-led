package com.hardkernel.wiringpi;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.ProgressBar;

public class MainActivity extends Activity {
    private final int DATA_UPDATE_PERIOD = 100; // 100ms
    private final int PORT_ADC1 = 0;   // ADC.AIN0
    private ProgressBar mADC;
    private final int ledPorts[] = {
        97, // GPIOX.BIT0(#97)
        108, // GPIOX.BIT11(#108)
        100, // GPIOX.BIT3(#100)
        101, // GPIOX.BIT4(#101)
        105, // GPIOX.BIT8(#105)
        106, // GPIOX.BIT9(#106)
        107, // GPIOX.BIT10(#107)
        115,  // GPIOX.BIT18(#115)
        116,  // GPIOX.BIT19(#116)
        88,  // GPIOY.BIT8(#88)
        83,  // GPIOY.BIT3(#83)
        87,  // GPIOY.BIT7(#87)
        104,  // GPIOX.BIT7(#104)
        102,  // GPIOX.BIT5(#102)
        103,  // GPIOX.BIT6(#103)
        117, // GPIOX.BIT20(#117)
        99, // GPIOX.BIT2(#99)
        118, // GPIOX.BIT21(#118)
        98, // GPIOX.BIT1(#98)
    };

    private static final int[] CHECKBOX_IDS = {
        R.id.led01, R.id.led02, R.id.led03, R.id.led04, R.id.led05,
        R.id.led06, R.id.led07, R.id.led08, R.id.led09, R.id.led10,
        R.id.led11, R.id.led12, R.id.led13, R.id.led14, R.id.led15,
        R.id.led16, R.id.led17, R.id.led18, R.id.led19
    };

    private List<CheckBox>mLeds;
    private final static String TAG = "example-led";
    private boolean mStop;
    private Process mProcess;

    private Handler handler = new Handler();
    Runnable runnable = new Runnable() {

        @Override
        public void run() {
            // TODO Auto-generated method stub
            update();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mADC = (ProgressBar) findViewById(R.id.adc);

        mLeds = new ArrayList<CheckBox>();

        try {
            mProcess = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        for (int id: CHECKBOX_IDS) {
            CheckBox cb = (CheckBox) findViewById(id);
            mLeds.add(cb);
        }

        wiringPiSetupSys();
    }

    public void update() {
        int i = 0;
        int adcValue = 0;
        int ledPos = 0;
        if ((adcValue = analogRead (PORT_ADC1)) > 0) {
            ledPos = (adcValue * ledPorts.length * 1000) / 1024;
            ledPos = (ledPorts.length - (ledPos / 1000));
            mADC.setProgress(adcValue);
        } else
            ledPos = 0;

        for (i = 0; i < ledPorts.length; i++) {
            digitalWrite (ledPorts[i], 0);
            mLeds.get(i).setChecked(false);
        }

        for (i = 0; i < ledPos; i++) {
            digitalWrite (ledPorts[i], 1);
            mLeds.get(i).setChecked(true);
        }

        if (!mStop)
            handler.postDelayed(runnable, 100);
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        exportGPIO();       

        mStop = false;
        handler.postDelayed(runnable, 100);
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        mStop = true;
        unexportGPIO();        
        for (int i = 0; i < ledPorts.length; i++) {
            digitalWrite (ledPorts[i], 0);
        }
    }

    boolean exportGPIO() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            for (int port: ledPorts) {
                os.writeBytes("echo " +  port + " > /sys/class/gpio/export\n");
                os.writeBytes("chmod 666 /sys/class/gpio/gpio" + port + "/direction\n");
                os.writeBytes("echo out > /sys/class/gpio/gpio" + port + "/direction\n");
                os.writeBytes("chmod 666 /sys/class/gpio/gpio" + port + "/value\n");
            }
            os.flush();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return false;
        }

        return true;
    }

    boolean unexportGPIO() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            for (int port : ledPorts) {
                os.writeBytes("echo " + port + " > /sys/class/gpio/unexport\n");
            }
            os.flush();
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            return false;
        }

        return true;
    }

    public native int wiringPiSetupSys();
    public native int analogRead(int port);
    public native void digitalWrite(int port, int onoff);

    static {
        System.loadLibrary("wpi_android");
    }
}
