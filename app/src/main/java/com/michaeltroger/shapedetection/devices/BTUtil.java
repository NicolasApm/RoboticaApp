package com.michaeltroger.shapedetection.devices;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class BTUtil {
    //private static Handler bluetoothIn;
    final int handlerState = 0;
    private StringBuilder DataStringIN = new StringBuilder();
    private BluetoothAdapter mBtAdapter;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothSocket btSocket;
    private BTCallback cb;
    private boolean isRunning = false;

    private final Handler bluetoothIn = new Handler() {
        public void handleMessage(android.os.Message msg) {
            if (msg.what == handlerState) {
                String readMessage = (String) msg.obj;
                DataStringIN.append(readMessage);

                int endOfLineIndex = DataStringIN.indexOf("#");

                if (endOfLineIndex > 0) {
                    String dataInPrint = DataStringIN.substring(0, endOfLineIndex);
                    cb.onNext(dataInPrint);
                    //IdBufferIn.setText("Dato: " + dataInPrint);//<-<- PARTE A MODIFICAR >->->
                    DataStringIN.delete(0, DataStringIN.length());
                }
            }
        }
    };
    private ConnectedThread MyConexionBT;

    public BTUtil() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();



    }

    /**
     * Comprueba que el dispositivo tiene Bluetooth y que está encendido.
     *
     * @return
     * @throws Exception
     */

    /**
     * @return
     */
    public Set<BluetoothDevice> getDevices() {
        return mBtAdapter.getBondedDevices();
    }

    /**
     * Obtiene el dispositivo por la dirección
     *
     * @param address
     * @return
     */
    public BluetoothDevice getRemoteDevice(String address) {
        return mBtAdapter.getRemoteDevice(address);
    }

    public void connect(String address, BTCallback cb) throws IOException {
        connect(getRemoteDevice(address), cb);
    }

    public void connect(BluetoothDevice device, BTCallback cb) throws IOException {
        this.cb = cb;
       // if (btSocket == null)
      //      btSocket = createBluetoothSocket(device);
        // Establece la conexión con el socket Bluetooth.
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            //Toast.makeText(getBaseContext(), "La creacción del Socket fallo", Toast.LENGTH_LONG).show();
        }
        // Establece la conexión con el socket Bluetooth.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) {
            }
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    public void close() {
        if (btSocket == null)
            return;
        try {
            isRunning = false;
            btSocket.close();
            btSocket = null;
        } catch (Exception e2) {

        }
    }

    public void write(String data){
        MyConexionBT.write(data);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        //crea un conexion de salida segura para el dispositivo
        //usando el servicio UUID
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    //Crea la clase que permite crear el evento de conexion
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

       /* public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Se mantiene en modo escucha para determinar el ingreso de datos
            while (isRunning) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    // Envia los datos obtenidos hacia el evento via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    cb.onError(e);
                    break;
                }
            }
        }*/

        //Envio de trama
        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                //si no es posible enviar datos se cierra la conexión
                //Toast.makeText(getBaseContext(), "La Conexión fallo", Toast.LENGTH_LONG).show();
                //finish();
            }
        }
    }
}
