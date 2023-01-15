package com.sj.manipulatorcontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ControlActivity extends AppCompatActivity {

    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;
    private BluetoothDevice mDevice;

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;

    private TextView distanceFront, distanceRight, distanceRear, distanceLeft;
    private JoystickView joystick;

    private ProgressDialog progressDialog;

    private CommandFilter commandFilter;
    private Komenda komenda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // wymuszenie orientacji pionowej

        Intent intent = getIntent(); //pobranie danych z intencji ktora uruchomila tą aktywnosc
        Bundle b = intent.getExtras();
        mDevice = b.getParcelable(ConnectActivity.DEVICE_EXTRA); //urzadzenie BT z ktorym sie laczymy
        mDeviceUUID = UUID.fromString(b.getString(ConnectActivity.DEVICE_UUID)); //identyfkator urzadzenia

        distanceFront = (TextView) findViewById(R.id.frontSensorDistanceTextView); //powiazania pol tekstowych w pliku xml z kodem java
        distanceRight = (TextView) findViewById(R.id.rightSensorDistanceTextView);
        distanceRear = (TextView) findViewById(R.id.rearSensorDistanceTextView);
        distanceLeft = (TextView) findViewById(R.id.leftSensorDistanceTextView);

        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setFixedCenter(true);

        commandFilter = new CommandFilter(300L);

        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
                int skret = (int) (strength * Math.cos(Math.toRadians(angle)));
                int predkosc = (int) (strength * Math.sin(Math.toRadians(angle)));
                skret = (skret / 10) * 10;
                predkosc = (predkosc / 10) * 10;
                if (skret >= 100) skret = 99;
                if (skret <= -100) skret = -99;
                if (predkosc >= 100) predkosc = 99;
                if (predkosc <= -100) predkosc = -99;
                komenda = new Komenda(predkosc, skret);
                System.out.print(komenda);
                if (commandFilter.isRecommendedToSendCommand(komenda)) {
                    sendCommandThroughBluetooth(commandFilter.sendCommand());
                    System.out.println(" -- SENT!");
                } else
                    System.out.println("");
            }
        }, 25);
    }

    private void sendCommandThroughBluetooth(String command) {
        try {
            mBTSocket.getOutputStream().write(command.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ReadInput implements Runnable { // klasa do odbierania informacji od Arduino przez Bluetooth

        private boolean bStop = false; // zmienna informująca o poleceniu zatrzymania komunikacji
        private Thread t; // wątek odpowiedzialny za obsługę odbierania danych

        // konstruktor obiektu tworzący nowy wątek do odbierania danych
        public ReadInput() {
            t = new Thread(this, "Input Thread");
            t.start();
        }

        // metoda informująca, czy wątek działa
        public boolean isRunning() {
            return t.isAlive();
        }

        // metoda odpowiedzialna za odbiór, parsowanie i wyświetlanie danych o kątach manipulatora, odebranych przez BT
        @Override
        public void run() {
            InputStream inputStream; // deklaracja wejściowego strumienia danych z Bluetooth

            try {
                inputStream = mBTSocket.getInputStream(); // otwarcie strumienia danych z bluetooth
                while (!bStop) {
                    byte[] buffer = new byte[256];
                    if (inputStream.available() > 0) {
                        inputStream.read(buffer); // odczyt ze strumienia Bluetooth do bufora wiadomości
                        int i = 0;
                        for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
                        }
                        final String strInput = new String(buffer, 0, i);

                        String[] informacje = strInput.split(";"); // parsowanie danych z bufora
                        Log.d("ReadInput", String.format("informacje[%d]: %s", informacje.length, strInput));

                        if(informacje.length == 5) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    distanceFront.setText(informacje[0] + " cm");
                                    distanceRight.setText(informacje[1] + " cm");
//                                    distanceRear.setText(informacje[2]);
                                    distanceLeft.setText(informacje[3] + " cm");
                                }
                            });
                        } else if(informacje.length < 5) {
                            Log.d("ReadInputError", String.format("informacje[%d]: %s", informacje.length, strInput));
                        }
                    }
                    Thread.sleep(200);
                }

                // obsługa wyjątków
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        public void stop() {
            bStop = true;
        } // metoda do zatrzymywania wątku

    }

    private class DisConnectBT extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected Void doInBackground(Void... params) {

            if (mReadThread != null) {
                mReadThread.stop();
                while (mReadThread.isRunning())
                    ; // Wait until it stops
                mReadThread = null;
            }

            try {
                mBTSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            mIsBluetoothConnected = false;
            if (mIsUserInitiatedDisconnect) {
                finish();
            }
        }
    }

    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() { //pauza aktywnosci - rozlaczenie z arduino
        if (mBTSocket != null && mIsBluetoothConnected) {
            new DisConnectBT().execute();
        }
        super.onPause();
    }

    @Override
    protected void onResume() { //pauza aktywnosci - ponowne laczenie z Arduino
        if (mBTSocket == null || !mIsBluetoothConnected) {
            new ConnectBT().execute();
        }
        super.onResume();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean mConnectSuccessful = true;

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ControlActivity.this, "Czekaj", "Łączenie..."); // ekran oczekiwania na polaczenie
        }

        @Override
        protected Void doInBackground(Void... devices) {

            try {
                if (mBTSocket == null || !mIsBluetoothConnected) {
                    mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID); //nawiazywanie polaczenia BT z arduino
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    mBTSocket.connect();
                }
            } catch (IOException e) {
                // Unable to connect to device
                e.printStackTrace();
                mConnectSuccessful = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            if (!mConnectSuccessful) {
                msg("Nie udało się połączyć z robotem :(");
                finish();
            } else {
                msg("Połączono z robotem!");
                mIsBluetoothConnected = true;
                mReadThread = new ReadInput(); // Kick off input reader
            }

            progressDialog.dismiss();
        }

    }

}