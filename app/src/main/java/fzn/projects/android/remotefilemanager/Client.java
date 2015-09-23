package fzn.projects.android.remotefilemanager;

import android.app.Application;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * Created by FzN on 2015/9/20.
 */
public class Client {
    private static final String TAG = Client.class.getSimpleName();
    private String address;
    private int port;
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean bConnected;

    public Client(final String address, final int port) {
        this.address = address;
        this.port = port;
    }

    public void connect() {
        try {
            socket = new Socket(address, port);
            if (socket.isConnected()) {
                bConnected = true;
                Log.i(TAG, "Connected");
            }
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getConnected() {
        return bConnected;
    }

    public void transfer(String str) {
        try {
            writer.write(str);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receive() {
        String recv = null;
        try {
            recv = reader.readLine();
            Log.d(TAG, recv);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return recv;
    }

    public void disconnect() {
        if (socket == null || socket.isClosed())
            return;
        try {
            reader.close();
            writer.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bConnected = false;
            reader = null;
            writer = null;
            socket = null;
            Log.i(TAG, "Disconnected");
        }
    }
}
