package fzn.projects.android.remotefilemanager;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.util.ArrayMap;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link ServerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ServerFragment extends Fragment {
    private static final String MAX_CONN = "maxConn";
    private static final String BSTART = "bStart";
    private static final String BSTOP = "bStop";

    private static final String IP = "IP";
    private static final String PORT = "Port";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private SaveFragmentStateCallbacks callbacks;
    private EditText etMaxConn;
    private Button btnStartServer, btnStopServer;
    private boolean bBtnStart, bBtnStop;
    private ListView clientsListView;
    private SimpleAdapter adapter;
    private ArrayList<Map<String, String>> clientList;
    private Thread serverThread;
    private ServerHandler handler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param sectionNumber Parameter 1.
     * @return A new instance of fragment ServerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ServerFragment newInstance(int sectionNumber) {
        ServerFragment fragment = new ServerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ServerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_server, container, false);
        View.OnClickListener listener = new ButtonListener();
        etMaxConn = (EditText) view.findViewById(R.id.etMaxConn);
        btnStartServer = (Button) view.findViewById(R.id.btnStartServer);
        btnStartServer.setOnClickListener(listener);
        btnStopServer = (Button) view.findViewById(R.id.btnStopServer);
        btnStopServer.setOnClickListener(listener);
        clientsListView = (ListView) view.findViewById(R.id.lvClient);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        callbacks = (SaveFragmentStateCallbacks) getActivity();
        handler = new ServerHandler(this);
        clientList = ((MainActivity) getActivity()).clientList;
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle savedState = callbacks.getState(ServerFragment.class) == null ?
                new Bundle() : callbacks.getState(ServerFragment.class);
        etMaxConn.setText(savedState.getString(MAX_CONN, "1"));
        bBtnStart = savedState.getBoolean(BSTART, true);
        btnStartServer.setEnabled(bBtnStart);
        bBtnStop = savedState.getBoolean(BSTOP, false);
        btnStopServer.setEnabled(bBtnStop);
        adapter = new SimpleAdapter(getActivity(), clientList, R.layout.item_client_list,
                new String[]{IP, PORT}, new int[]{R.id.tvIP, R.id.tvPort});
        clientsListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        ((MainActivity) activity).onSectionAttached(
                getArguments().getInt(ARG_SECTION_NUMBER));
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks.putState(saveState());
    }

    private Bundle saveState() {
        Bundle state = new Bundle();
        state.putString("Fragment", ServerFragment.class.getSimpleName());
        state.putString(MAX_CONN, etMaxConn.getText().toString());
        state.putBoolean(BSTART, bBtnStart);
        state.putBoolean(BSTOP, bBtnStop);
        return state;
    }

    public void notifyClientChanged() {
        adapter.notifyDataSetChanged();
    }

    private class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStartServer: {
                    if (etMaxConn.getText().toString().equals("")
                            || Integer.valueOf(etMaxConn.getText().toString()) < 1) {
                        Toast.makeText(getActivity(), "Max Connection is not set!", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Server server = new Server(Constants.PORT,
                            Integer.valueOf(etMaxConn.getText().toString()), (MainActivity) getActivity());
                    ((MainActivity) getActivity()).setServerRef(server);
                    serverThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) getActivity()).getServerRef().start();
                            while (!((MainActivity) getActivity()).getServerRef().getRunning()) ;
                            Message msg = handler.obtainMessage(R.id.btnStartServer);
                            msg.arg1 = 1;
                            handler.sendMessage(msg);
                        }
                    });
                    serverThread.start();
                }
                break;
                case R.id.btnStopServer: {
                    if (((MainActivity) getActivity()).getServerRef() != null) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ((MainActivity) getActivity()).getServerRef().stop();
                                while (((MainActivity) getActivity()).getServerRef().getRunning()) ;
                                Message msg = handler.obtainMessage(R.id.btnStopServer);
                                msg.arg1 = 1;
                                handler.sendMessage(msg);
                            }
                        }).start();
                    }
                }
                break;
            }
        }
    }

    private static class ServerHandler extends Handler {
        private final WeakReference<Fragment> fragmentRef;

        public ServerHandler(Fragment fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case R.id.btnStartServer:
                    if (fragmentRef.get() != null) {
                        ((ServerFragment) fragmentRef.get()).bBtnStart = false;
                        ((ServerFragment) fragmentRef.get()).btnStartServer.setEnabled(false);
                        ((ServerFragment) fragmentRef.get()).bBtnStop = true;
                        ((ServerFragment) fragmentRef.get()).btnStopServer.setEnabled(true);
                    }
                    break;
                case R.id.btnStopServer:
                    if (fragmentRef.get() != null) {
                        ((ServerFragment) fragmentRef.get()).bBtnStart = true;
                        ((ServerFragment) fragmentRef.get()).btnStartServer.setEnabled(true);
                        ((ServerFragment) fragmentRef.get()).bBtnStop = false;
                        ((ServerFragment) fragmentRef.get()).btnStopServer.setEnabled(false);
                    }
                    break;
            }
        }
    }
}
