package fzn.projects.android.remotefilemanager;

import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

/**
 * Created by FzN on 2015/9/20.
 */
public class Server {
    private static final String TAG = Server.class.getSimpleName();
    private int port;
    private int ctConn, maxConn;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private Map<Pair<InetAddress, Integer>, Socket> sockets = new ArrayMap<>();
    private MainActivity activity;
    private boolean bRunning = false;

    public Server(final int port, final int maxConn, final MainActivity activity) {
        this.port = port;
        this.maxConn = maxConn;
        this.activity = activity;
    }

    public void start() {
        serverThread = new Thread(new ServerRunnable());
        serverThread.start();
    }

    public void stop() {
        Socket toStop;
        try {
            toStop = new Socket("localhost", port);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (serverSocket.isClosed()) {
                serverSocket = null;
                serverThread = null;
                bRunning = false;
                Log.i(TAG, "Stopped");
            }
        }
    }

    public boolean getRunning() {
        return bRunning;
    }

    private class ServerRunnable implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(port);
                bRunning = true;
                Log.i(TAG, "Running");
                while (bRunning) {
                    if (ctConn < maxConn) {
                        Socket socket = serverSocket.accept();
                        Pair<InetAddress, Integer> pair = new Pair<>(socket.getInetAddress(), socket.getPort());
                        synchronized (sockets) {
                            sockets.put(pair, socket);
                            if (activity != null)
                                activity.onConnectionChanged(sockets.keySet());
                        }
                        new Thread(new CommRunnable(socket)).start();
                        ctConn++;
                        Log.i(TAG, "Count of connections: " + ctConn);
                    }
                }
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class CommRunnable implements Runnable {
        private Socket socket;
        private BufferedReader reader;
        private BufferedWriter writer;

        public CommRunnable(final Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            if (socket == null || socket.isClosed())
                return;
            try {
                String inComing;
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                while ((inComing = reader.readLine()) != null && socket.isConnected()) {
                    if (inComing.equals(Constants.EXIT))
                        break;
                    Log.d(TAG, inComing);
                    writer.write(inComing + System.lineSeparator());
                    writer.flush();
                }
                Log.d(TAG, "remove " + sockets.remove(new Pair<>(socket.getInetAddress(), socket.getPort())).toString());
                reader.close();
                writer.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                synchronized (sockets) {
                    ctConn--;
                    Log.i(TAG, "Count of connections: " + ctConn);
                    if (activity != null)
                        activity.onConnectionChanged(sockets.keySet());
                }
                reader = null;
                writer = null;
                socket = null;
            }
        }
    }

}
