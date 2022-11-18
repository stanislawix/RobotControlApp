package com.sj.manipulatorcontrol;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public class ControlActivity extends AppCompatActivity {

    private UUID mDeviceUUID;
    private BluetoothSocket mBTSocket;
    private ReadInput mReadThread = null;
    private BluetoothDevice mDevice;

    private boolean mIsUserInitiatedDisconnect = false;
    private boolean mIsBluetoothConnected = false;

    private TextView katC0, katC1;
    private Button c0Button1, c0Button2, c0Button3, c0Button4;
    private Button c1Button1, c1Button2, c1Button3, c1Button4;
    private Button skretZeroingButton;
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
        mDevice = b.getParcelable(MainActivity.DEVICE_EXTRA); //urzadzenie BT z ktorym sie laczymy
        mDeviceUUID = UUID.fromString(b.getString(MainActivity.DEVICE_UUID)); //identyfkator urzadzenia

        katC0 = (TextView) findViewById(R.id.katSkretuTextView); //powiazania pol tekstowych w pliku xml z kodem java
        katC1 = (TextView) findViewById(R.id.velocityTextView);

        c0Button1 = (Button) findViewById(R.id.katSkretuButton1);
        c0Button2 = (Button) findViewById(R.id.katSkretuButton2);
        c0Button3 = (Button) findViewById(R.id.katSkretuButton3);
        c0Button4 = (Button) findViewById(R.id.katSkretuButton4);
        c1Button1 = (Button) findViewById(R.id.velocityButton1);
        c1Button2 = (Button) findViewById(R.id.velocityButton2);
        c1Button3 = (Button) findViewById(R.id.velocityButton3);
        c1Button4 = (Button) findViewById(R.id.velocityButton4);
        skretZeroingButton = (Button) findViewById(R.id.skretZeroingButton);

        joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setFixedCenter(true);

        commandFilter = new CommandFilter(300L);

        // metoda wysylajaca komende obrotu podstawy manipulatora o 5° przeciwnie do wskazowek zegara po wcisnieciu danego przycisku
        c0Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("S0-45;");
            }
        });

        c0Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("S0-15;");
            }
        });

        c0Button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("S015;");
            }
        });

        c0Button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("S045;");
            }
        });

        c1Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("V20;");
            }
        });

        c1Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("V50;");
            }
        });

        c1Button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("V75;");
            }
        });

        c1Button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("V100;");
            }
        });

        skretZeroingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendCommandThroughBluetooth("S000");
            }
        });

        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(int angle, int strength) {
//                double skret = (strength * Math.cos(Math.toRadians(angle)));
                int skret = (int) (strength * Math.cos(Math.toRadians(angle)));
//                double predkosc = (strength * Math.sin(Math.toRadians(angle)));
                int predkosc = (int) (strength * Math.sin(Math.toRadians(angle)));
                skret = (skret / 10) * 10;
                predkosc = (predkosc / 10) * 10;
                if(skret >= 100) skret = 99;
                if(skret <= -100) skret = -99;
                if(predkosc >= 100) predkosc = 99;
                if(predkosc <= -100) predkosc = -99;;
                komenda = new Komenda(predkosc, skret);
                System.out.print(komenda);
                if(commandFilter.isRecommendedToSendCommand(komenda)) {
                    sendCommandThroughBluetooth(commandFilter.sendCommand());
                    System.out.println(" -- SENT!");
                } else
                    System.out.println("");
            }
        },25);
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
                        if (informacje.length != 5) { // sprawdzenie czy wiadomosc z Arduino jest kompletna
                            msg("Odebrano niepoprawną wiadomość z Arduino!"); // alert o błędnej wiadomości z Arduino
                        } else {
                            runOnUiThread(new Runnable() { // wywołanie funkcji do modyfikacji widoku aktywnosci z innego watku (tego watku)
                                @Override
                                public void run() { // aktualizacja kątów manipulatora wyświetlanych w aplikacji
                                    katC0.setText(informacje[0] + "°");
                                    katC1.setText(informacje[1] + "°");
//                                    katC2.setText(informacje[2] + "°");
//                                    katC3.setText(informacje[3] + "°");
//                                    katC4.setText(informacje[4] + "°");
                                }
                            });
                        }
                    }
                    Thread.sleep(500);
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
                msg("Nie udało się połączyć z Arduino :(");
                finish();
            } else {
                msg("Połączono z Arduino!");
                mIsBluetoothConnected = true;
//                mReadThread = new ReadInput(); // Kick off input reader
            }

            progressDialog.dismiss();
        }

    }

}