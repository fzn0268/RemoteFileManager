package fzn.projects.android.remotefilemanager;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link ClientFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ClientFragment extends Fragment {
    private static final String ADD = "Address";
    private static final String BCONN = "bConnect";
    private static final String BDISCONN = "bDisconnect";
    private static final String OUTMSG = "outMessage";
    private static final String BSEND = "bSend";
    private static final String BRECV = "bRecv";
    private static final String INMSG = "inMessage";
    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    private SaveFragmentStateCallbacks callbacks;
    private EditText etAddress, etMessage;
    private Button btnConnect, btnDisconnect, btnSend, btnRecv;
    private boolean bConnect, bDisconnect, bSend, bRecv;
    private TextView tvMessage;
    private ClientHandler clientHandler;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param sectionNumber Parameter 1.
     * @return A new instance of fragment ClientFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ClientFragment newInstance(int sectionNumber) {
        ClientFragment fragment = new ClientFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    public ClientFragment() {
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
        View view = inflater.inflate(R.layout.fragment_client, container, false);
        View.OnClickListener listener = new ButtonListener();
        etAddress = (EditText) view.findViewById(R.id.etAddress);
        etMessage = (EditText) view.findViewById(R.id.etMessage);
        btnConnect = (Button) view.findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(listener);
        btnDisconnect = (Button) view.findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(listener);
        btnSend = (Button) view.findViewById(R.id.btnSend);
        btnSend.setOnClickListener(listener);
        tvMessage = (TextView) view.findViewById(R.id.tvRcev);
        btnRecv = (Button) view.findViewById(R.id.btnRecv);
        btnRecv.setOnClickListener(listener);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        callbacks = (SaveFragmentStateCallbacks) getActivity();
        clientHandler = new ClientHandler(this);
    }

    @Override
    public void onStart() {
        super.onStart();
        Bundle savedState = callbacks.getState(ClientFragment.class) == null ?
                new Bundle() : callbacks.getState(ClientFragment.class);
        etAddress.setText(savedState.getString(ADD, ""));
        etMessage.setText(savedState.getString(INMSG, ""));
        bConnect = savedState.getBoolean(BCONN, true);
        btnConnect.setEnabled(bConnect);
        bDisconnect = savedState.getBoolean(BDISCONN, false);
        btnDisconnect.setEnabled(bDisconnect);
        bSend = savedState.getBoolean(BSEND, false);
        btnSend.setEnabled(bSend);
        tvMessage.setText(savedState.getString(INMSG, ""));
        bRecv = savedState.getBoolean(BRECV, false);
        btnRecv.setEnabled(bRecv);
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
        state.putString("Fragment", ClientFragment.class.getSimpleName());
        state.putString(ADD, etAddress.getText().toString());
        state.putBoolean(BCONN, bConnect);
        state.putBoolean(BDISCONN, bDisconnect);
        state.putBoolean(BSEND, bSend);
        state.putBoolean(BRECV, bRecv);
        state.putString(OUTMSG, etMessage.getText().toString());
        state.putString(INMSG, tvMessage.getText().toString());
        return state;
    }

    private class ButtonListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnConnect: {
                    if (etAddress.getText().toString().equals("")) {
                        Toast.makeText(getActivity(), "Address is empty.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Client client = new Client(etAddress.getText().toString(), Constants.PORT);
                    ((MainActivity) getActivity()).setClientRef(client);
                    Thread clientThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            ((MainActivity) getActivity()).getClientRef().connect();
                            while (!((MainActivity) getActivity()).getClientRef().getConnected()) ;
                            Message msg = clientHandler.obtainMessage(R.id.btnConnect);
                            msg.arg1 = 1;
                            clientHandler.sendMessage(msg);
                        }
                    });
                    clientThread.start();
                }
                break;
                case R.id.btnDisconnect: {
                    if (((MainActivity) getActivity()).getClientRef() != null) {
                        ((MainActivity) getActivity()).getClientRef().disconnect();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                while (((MainActivity) getActivity()).getClientRef().getConnected())
                                    ;
                                Message msg = clientHandler.obtainMessage(R.id.btnDisconnect);
                                msg.arg1 = 1;
                                clientHandler.sendMessage(msg);
                            }
                        }).start();
                    }
                }
                break;
                case R.id.btnSend: {
                    if (etMessage.getText().toString().equals("")) {
                        Toast.makeText(getActivity(), "Message is empty.", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    ((MainActivity) getActivity()).getClientRef().transfer(etMessage.getText().toString() + System.lineSeparator());
                    btnRecv.setEnabled(true);
                }
                break;
                case R.id.btnRecv: {
                    Thread recvThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String str = ((MainActivity) getActivity()).getClientRef().receive();
                            Message msg = clientHandler.obtainMessage(R.id.tvRcev);
                            msg.obj = str;
                            clientHandler.sendMessage(msg);
                        }
                    });
                    recvThread.start();
                    btnRecv.setEnabled(false);
                }
                break;
            }
        }
    }

    private static class ClientHandler extends Handler {
        private final WeakReference<Fragment> fragmentRef;

        public ClientHandler(Fragment fragment) {
            fragmentRef = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case R.id.tvRcev:
                    if (fragmentRef.get() != null)
                        ((ClientFragment) fragmentRef.get()).tvMessage
                                .setText((String) msg.obj);
                    break;
                case R.id.btnConnect:
                    if (msg.arg1 == 1 && fragmentRef.get() != null) {
                        ((ClientFragment) fragmentRef.get()).bDisconnect = true;
                        ((ClientFragment) fragmentRef.get()).btnDisconnect.setEnabled(true);
                        ((ClientFragment) fragmentRef.get()).bSend = true;
                        ((ClientFragment) fragmentRef.get()).btnSend.setEnabled(true);
                        ((ClientFragment) fragmentRef.get()).bConnect = false;
                        ((ClientFragment) fragmentRef.get()).btnConnect.setEnabled(false);
                    }
                    break;
                case R.id.btnDisconnect:
                    if (msg.arg1 == 1 && fragmentRef.get() != null) {
                        ((ClientFragment) fragmentRef.get()).bConnect = true;
                        ((ClientFragment) fragmentRef.get()).btnConnect.setEnabled(true);
                        ((ClientFragment) fragmentRef.get()).bDisconnect = true;
                        ((ClientFragment) fragmentRef.get()).btnDisconnect.setEnabled(false);
                        ((ClientFragment) fragmentRef.get()).bSend = false;
                        ((ClientFragment) fragmentRef.get()).btnSend.setEnabled(false);
                        ((ClientFragment) fragmentRef.get()).bRecv = false;
                        ((ClientFragment) fragmentRef.get()).btnRecv.setEnabled(false);
                    }
                    break;
            }
        }
    }

    public interface ClientReference {
        void setClientRef(Client client);

        Client getClientRef();
    }
}
