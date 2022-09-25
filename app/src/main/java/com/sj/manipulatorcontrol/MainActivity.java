package com.sj.manipulatorcontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button search;
    private Button connect;
    private ListView listView;
    private BluetoothAdapter mBTAdapter;
    public UUID mDeviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final String DEVICE_EXTRA = "com.sj.manipulatorcontrol.SOCKET";
    public static final String DEVICE_UUID = "com.sj.manipulatorcontrol.uuid";
    private static final String DEVICE_LIST = "com.sj.manipulatorcontrol.devicelist";
    private static final String DEVICE_LIST_SELECTED = "com.sj.manipulatorcontrol.devicelistselected";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // wywołanie metody łączącej kod java z widokiem xml

        search = (Button) findViewById(R.id.search); // definicja przycisku do wyswietlenia listy sparowanych urzadzen BT
        connect = (Button) findViewById(R.id.connect); // definicja przycisku do polaczenia z wybranym urzadzeniem BT z listy

        listView = (ListView) findViewById(R.id.listview); // definicja listy urzadzen BT

        if (savedInstanceState != null) {
            ArrayList<BluetoothDevice> list = savedInstanceState.getParcelableArrayList(DEVICE_LIST); //wczytanie listy w przypadku zapisanego stanu aplikacji
            if (list != null) {
                initList(list); // inicjalizacja listy urzadzen BT
                MyAdapter adapter = (MyAdapter) listView.getAdapter();
                int selectedIndex = savedInstanceState.getInt(DEVICE_LIST_SELECTED);
                if (selectedIndex != -1) {
                    adapter.setSelectedIndex(selectedIndex);
                    connect.setEnabled(true);
                }
            } else {
                initList(new ArrayList<BluetoothDevice>()); // inicjalizacja listy urzadzen BT w przypadku braku zapisanej listy urzadzen BT
            }

        } else {
            initList(new ArrayList<BluetoothDevice>()); // inijcalizacja listy urzadzen BT w przypadku braku zapisanego stanu aplikacji
        }
        search.setOnClickListener(new View.OnClickListener() { // ustawienie listenera klikniecia w przycisk listy urzadzen

            @Override
            public void onClick(View arg0) {
                mBTAdapter = BluetoothAdapter.getDefaultAdapter();

                if (mBTAdapter == null) {
                    Toast.makeText(getApplicationContext(), "Nie znaleziono modulu Bluetooth", Toast.LENGTH_SHORT).show(); // informacja uzytkownika ze nie znaleziono modulu BT
                } else if (!mBTAdapter.isEnabled()) {
                    msg("Bluetooth jest wyłączony - włącz go!"); // informacja uzytkownika ze nie wlaczono modulu BT
                } else {
                    new SearchDevices().execute(); // szukanie sparowanych urzadzen BT
                }
            }
        });

        connect.setOnClickListener(new View.OnClickListener() { // ustawienie listenera klikniecia w przycisk polaczenia z urzadzeniem BT

            @Override
            public void onClick(View arg0) {
                BluetoothDevice device = ((MyAdapter) (listView.getAdapter())).getSelectedItem();
                Intent intent = new Intent(getApplicationContext(), ControlActivity.class);
                intent.putExtra(DEVICE_EXTRA, device);
                intent.putExtra(DEVICE_UUID, mDeviceUUID.toString());
                startActivity(intent); // uruchamianie aktywnosci szklarni z przekazaniem informacji o urzadzeniu BT z ktorym ma sie polaczyc (z Arduino)
            }
        });

    }

    private void msg(String str) { //funckja do szybkiego wypisywania toastow
        Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
    }

    private void initList(List<BluetoothDevice> objects) { //funkcja do inicjalizacji listy urzadzen
        final MyAdapter adapter = new MyAdapter(getApplicationContext(), R.layout.list_item, R.id.listContent, objects);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) { //listener klikniecia na urzadzenie na liscie
                adapter.setSelectedIndex(position);
                connect.setEnabled(true);
            }
        });
    }

    private class SearchDevices extends AsyncTask<Void, Void, List<BluetoothDevice>> {
        // klasa do wyszukiwania urzadzen BT sparowanych

        @Override
        protected List<BluetoothDevice> doInBackground(Void... params) {
            Set<BluetoothDevice> pairedDevices = mBTAdapter.getBondedDevices();
            List<BluetoothDevice> listDevices = new ArrayList<BluetoothDevice>();
            for (BluetoothDevice device : pairedDevices) {
                listDevices.add(device);
            }
            return listDevices;

        }

        @Override
        protected void onPostExecute(List<BluetoothDevice> listDevices) {
            super.onPostExecute(listDevices);
            if (listDevices.size() > 0) {
                MyAdapter adapter = (MyAdapter) listView.getAdapter();
                adapter.replaceItems(listDevices);
            } else {
                msg("Nie znaleziono sparowanych urzadzen Bluetooth, najpierw sparuj telefon z Arduino!");
            }
        }

    }

    private class MyAdapter extends ArrayAdapter<BluetoothDevice> { //adapter do listy sparowanych urzadzen BT
        private int selectedIndex;
        private Context context;
        private int selectedColor = Color.parseColor("#abcdef");
        private List<BluetoothDevice> myList;

        public MyAdapter(Context ctx, int resource, int textViewResourceId, List<BluetoothDevice> objects) { //konstruktor adaptera
            super(ctx, resource, textViewResourceId, objects);
            context = ctx;
            myList = objects;
            selectedIndex = -1;
        }

        public void setSelectedIndex(int position) {
            selectedIndex = position;
            notifyDataSetChanged();
        }

        public BluetoothDevice getSelectedItem() {
            return myList.get(selectedIndex);
        }

        @Override
        public int getCount() {
            return myList.size();
        }

        @Override
        public BluetoothDevice getItem(int position) {
            return myList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private class ViewHolder {
            TextView tv;
        }

        public void replaceItems(List<BluetoothDevice> list) {
            myList = list;
            notifyDataSetChanged();
        }

        public List<BluetoothDevice> getEntireList() {
            return myList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View vi = convertView;
            ViewHolder holder;
            if (convertView == null) {
                vi = LayoutInflater.from(context).inflate(R.layout.list_item, null);
                holder = new ViewHolder();

                holder.tv = (TextView) vi.findViewById(R.id.listContent);

                vi.setTag(holder);
            } else {
                holder = (ViewHolder) vi.getTag();
            }

            if (selectedIndex != -1 && position == selectedIndex) {
                holder.tv.setBackgroundColor(selectedColor);
            } else {
                holder.tv.setBackgroundColor(Color.WHITE);
            }
            BluetoothDevice device = myList.get(position);
            holder.tv.setText(device.getName() + "\n " + device.getAddress());

            return vi;
        }

    }

}