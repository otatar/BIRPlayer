package net.komiso.otatar.birplayer;


import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dgreenhalgh.android.simpleitemdecoration.linear.DividerItemDecoration;

import net.komiso.otatar.biplayer.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import es.claucookie.miniequalizerlibrary.EqualizerView;


/**
 * A simple {@link Fragment} subclass.
 */
public class RadioStationListFragment extends Fragment {

    //Log tag
    private static final String LOG_TAG = "RadioStationListFrag";

    private static String SERVER_URL = "http://10.0.2.2:8081/get_stations";
    private static String REMOTE_SERVER_URL = "http://82.118.0.14:8081/get_stations";

    /* If we are bound to service */
    private Boolean bound = false;

    //Messenger
    private Messenger fromServiceMessenger;

    //Reference to service messenger
    private Messenger toServiceMessenger;

    //Swipe refresh
    private SwipeRefreshLayout swipeRefresh;

    private ActionMode actionMode;

    //Recycler View
    private RecyclerView recyclerView;

    //Recycler View Adapter
    private RadioStationAdapter radioStationAdapter;

    //Recycler View Adapter (for recorded files)
    private RecordedRadioStationAdapter recordedRadioStationAdapter;

    /* Array list that contains all radio station objects */
    private ArrayList<RadioStation> radioStationsList = new ArrayList<>();

    /* Array list that contains all recorded radio station files */
    private ArrayList<File> recordedRadioStationList = new ArrayList<>();

    /* Flag selector for list type */
    private Boolean isRadioStationListPartial = false;

    /* Reference to radio station lab object */
    private RadioStationLab radioStationLab;

    /* Container Activity */
    private Main2Activity containerActivity;

    /* Radio Station selected listener */
    private OnRadioStationSelectedListener radioStationSelectedListener;

    /* Recording radio station selected listener */
    private OnRecordingSelectedListener recordingSelectedListener;

    /* */
    public static int list_type = 0;

    /* Variable to track selected items in recycler view of stations*/
    private static RadioStation selectedRadioStation = null;

    /* List of selected recordings in action mode */
    private HashSet<Integer> selectedRecordings = new HashSet<>();

    private static boolean recordingPlaying = false;


    /**
     * Interface implemented by container activity
     */
    public interface OnRadioStationSelectedListener {
        void onRadioStationSelected(int position, ArrayList<RadioStation> radioStationList, Boolean startPlayer);
    }

    public interface OnRadioStationSelectedAndPlayListener {
        void onRadioStationSelectedAndPlay(RadioStation radioStation, Boolean stopRadioStation);
    }

    public interface OnRecordingPlayListener {
        void onRecordingPlay(String recFilePath, Boolean stopRadioStation);
    }

    public interface OnRecordingSelectedListener {
        void onRecordingSelected(int postition, ArrayList<File> recordedRadioStationList);
    }

    /**
     * Class needed for recycler view
     */
    private class RadioStationHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView listStationName;
        private TextView listStationLocation;
        private TextView listStationGenre;
        private CheckBox listFavorite;
        private EqualizerView listEqualizer;
        private ToggleButton playPauseToggle;

        ArrayList<RadioStation> currentRadioStationList;

        public RadioStationHolder(View itemView) {
            super(itemView);

            listStationName = (TextView) itemView.findViewById(R.id.list_radio_name);
            listStationGenre = (TextView) itemView.findViewById(R.id.list_radio_genre);
            listStationLocation = (TextView) itemView.findViewById(R.id.list_radio_location);
            listFavorite = (CheckBox) itemView.findViewById(R.id.list_favorite);
            listEqualizer = (EqualizerView) itemView.findViewById(R.id.mini_equalizer);
            playPauseToggle = (ToggleButton) itemView.findViewById(R.id.button_play_pause);

            //Register holder for onClick events
            itemView.setOnClickListener(this);

        }

        void bindRadioStation(final RadioStation radioStation, ArrayList<RadioStation> list) {

            currentRadioStationList = list;

            // Reset typeface
            listStationName.setTypeface(null, Typeface.NORMAL);
            listStationGenre.setTypeface(null, Typeface.NORMAL);
            listStationLocation.setTypeface(null, Typeface.NORMAL);

            // Set the content of views
            listStationName.setText(radioStation.getRadioStationName());
            listStationLocation.setText(radioStation.getRadioStationLocation());
            listStationGenre.setText(radioStation.getRadioStationGenre());
            listFavorite.setChecked(radioStation.getFavorite());

            //If we are connecting to this station
            if (radioStation.getConnecting()) {
                listEqualizer.setVisibility(View.VISIBLE);
                listEqualizer.stopBars();
                if (!playPauseToggle.isChecked()) playPauseToggle.toggle();
            }

            //If we are playing this station
            //Log.d(LOG_TAG, "Radio: " + radioStation.getRadioStationName() + ": " + radioStation.getSelected());
            if (radioStation.getPlaying()) {
                listEqualizer.animateBars();
                listEqualizer.setVisibility(View.VISIBLE);
                if (!playPauseToggle.isChecked()) playPauseToggle.toggle();

            } else if (!radioStation.getPlaying() && !radioStation.getConnecting()) {
                listEqualizer.stopBars();
                listEqualizer.setVisibility(View.GONE);
                if (playPauseToggle.isChecked()) playPauseToggle.toggle();
            }

            // If this station is selected
            if (selectedRadioStation != null) {
                if (selectedRadioStation.getRadioStationName().equals(radioStation.getRadioStationName())) {

                    Log.d(LOG_TAG, "Highlighting: " + listStationName.getText());
                    listStationName.setTypeface(null, Typeface.BOLD);
                    listStationGenre.setTypeface(null, Typeface.BOLD);
                    listStationLocation.setTypeface(null, Typeface.BOLD);

                }
            }

            // Handle clicks on favorite star
            listFavorite.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(LOG_TAG, getClass().getName() + ":onClick");

                    radioStation.setFavorite(listFavorite.isChecked());
                    RadioStationLab.updateFavoriteRadioStation(radioStation, listFavorite.isChecked());
                }
            });

            // Handle clicks on play icon
            playPauseToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.d(LOG_TAG, "Clicked play button on " + radioStation.getRadioStationName() + " station");
                    RadioPlayerFragment.deselectAllRadioStations(radioStationsList);
                    RadioPlayerFragment.stopAllRadioStations(radioStationsList);
                    radioStation.setSelected(true);
                    selectedRadioStation = radioStation;
                    containerActivity.onRadioStationSelected(radioStationsList.indexOf(radioStation), radioStationsList, false);

                    if (playPauseToggle.isChecked()) {

                        Log.d(LOG_TAG, "Play " + radioStation.getRadioStationName());
                        containerActivity.onRadioStationSelectedAndPlay(radioStation, false);
                        radioStation.setConnecting(true);


                    } else {

                        Log.d(LOG_TAG, "Stop " + radioStation.getRadioStationName());
                        containerActivity.onRadioStationSelectedAndPlay(radioStation, true);
                        radioStation.setConnecting(false);

                    }

                    recyclerView.getAdapter().notifyDataSetChanged();
                }
            });

        }

        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, getClass().getName() + ":onClick");
            Log.d(LOG_TAG, "Clicked on: " + listStationName.getText() + " at position " +
                    getAdapterPosition());

            //Call method on activity
            radioStationSelectedListener.onRadioStationSelected(getAdapterPosition(), currentRadioStationList, true);

        }
    }


    /**
     * Handler of incoming messages from service.
     */
    class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(LOG_TAG, "RadioStationList:handleMessage() ");

            if (msg.what == RadioPlayerFragment.UPDATE_STATUS) {

                Bundle bundle = msg.getData();
                Log.d(LOG_TAG, String.valueOf(bundle.getString(RadioPlayerFragment.STATUS)));

                if (bundle.getString(RadioPlayerFragment.STATUS).equals(RadioPlayerService.MP_PLAYING)) {

                    Log.d(LOG_TAG, "Selected radio station is playing");
                    recordingPlaying = true;
                    if (selectedRadioStation != null) {
                        selectedRadioStation.setPlaying(true);
                        selectedRadioStation.setConnecting(false);
                    }

                } else if ((bundle.getString(RadioPlayerFragment.STATUS).equals(RadioPlayerService.MP_PAUSED))
                        || (bundle.getString(RadioPlayerFragment.STATUS).equals(RadioPlayerService.MP_STOPPED))) {

                    Log.d(LOG_TAG, "Selected radio station is stopped");
                    recordingPlaying = false;
//                    selectedRadioStation.setPlaying(false);
                    if (selectedRadioStation != null) {
                        selectedRadioStation.setConnecting(false);
                        RadioPlayerFragment.stopAllRadioStations(radioStationsList);
                    }

                } else if (bundle.getString(RadioPlayerFragment.STATUS).equals(RadioPlayerService.MP_CONNECTING)) {

                    Log.d(LOG_TAG, "Connecting to streaming server...");
                    selectedRadioStation.setConnecting(true);
                }

            } else if (msg.what == RadioPlayerFragment.SEND_ERROR) {

                recordingPlaying = false;
                selectedRadioStation.setConnecting(false);
                selectedRadioStation.setPlaying(false);

            } else if (msg.what == RadioPlayerFragment.SEND_COMPLETION) {

                Log.d(LOG_TAG, "Completion");
                recordingPlaying = false;

            }

            recyclerView.getAdapter().notifyDataSetChanged();
        }
    }


    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            toServiceMessenger = new Messenger(service);
            fromServiceMessenger = new Messenger(new IncomingHandler());
            bound = true;
            Log.d(LOG_TAG, "Connected to service");

            //Register to service for messages
            try {
                Message msg = Message.obtain(null,
                        RadioPlayerFragment.REGISTER);
                msg.replyTo = fromServiceMessenger;
                toServiceMessenger.send(msg);

            } catch (RemoteException e) {
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;

        }
    };


    /**
     * Recycler Adapter Class
     */
    private class RadioStationAdapter extends RecyclerView.Adapter<RadioStationHolder> {

        private ArrayList<RadioStation> radioStationList;
        private Activity activity;

        public RadioStationAdapter(Activity activity, ArrayList<RadioStation> radioStationList) {
            this.radioStationList = radioStationList;
            this.activity = activity;

        }

        //Setter for radioStationList
        public void setRadioStationList(ArrayList<RadioStation> radioStationList) {
            this.radioStationList = radioStationList;
        }

        @Override
        public RadioStationHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(activity);
            View view = layoutInflater.inflate(R.layout.list_radio_station, parent, false);

            return new RadioStationHolder(view);
        }

        @Override
        public void onBindViewHolder(final RadioStationHolder holder, final int position) {

            final RadioStation radioStation = radioStationList.get(position);

            //Click listener
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Highlight selection
                    selectedRadioStation = radioStation;
                    notifyItemChanged(position);

                    //Let the holder deal further with click
                    holder.onClick(v);

                }
            });

            holder.bindRadioStation(radioStation, this.radioStationList);

        }

        @Override
        public int getItemCount() {
            return radioStationList.size();
        }
    }


    /**
     * Recycler Adapter Class for recorded radio stations
     */
    private class RecordedRadioStationAdapter extends RecyclerView.Adapter<RecordedRadioStationHolder> {

        private ArrayList<File> recordedRadioStationList;
        private Activity activity;

        public RecordedRadioStationAdapter(Activity activity, ArrayList<File> recordedRadioStationList) {
            this.recordedRadioStationList = recordedRadioStationList;
            this.activity = activity;

        }

        //Setter for radioStationList
        public void seRecordedRadioStationList(ArrayList<File> recordedRadioStationList) {
            this.recordedRadioStationList = recordedRadioStationList;
        }

        @Override
        public RecordedRadioStationHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(activity);
            View view = layoutInflater.inflate(R.layout.list_recorded_radio_station, parent, false);

            return new RecordedRadioStationHolder(view);
        }

        @Override
        public void onBindViewHolder(final RecordedRadioStationHolder holder, final int position) {

            final File recordedRadioStation = recordedRadioStationList.get(position);


            //Click listener
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //Let the holder deal further with click
                    holder.onClick(v);

                }
            });

            holder.bindRecordedRadioStation(recordedRadioStation, this.recordedRadioStationList, position);

        }

        @Override
        public int getItemCount() {
            return recordedRadioStationList.size();
        }
    }

    /**
     * Class needed for recycler view
     */
    private class RecordedRadioStationHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView listRecordingName;
        private TextView listRecordingDuration;
        private TextView listRecordingDate;
        private ToggleButton playPauseToggle;
        private TextView listFormatInfo;
        private CheckBox listCheckBox;

        public RecordedRadioStationHolder(final View itemView) {
            super(itemView);

            listRecordingName = (TextView) itemView.findViewById(R.id.list_recording_filename);
            listRecordingDuration = (TextView) itemView.findViewById(R.id.list_recording_duration);
            listRecordingDate = (TextView) itemView.findViewById(R.id.list_recording_date);
            playPauseToggle = (ToggleButton) itemView.findViewById(R.id.button_play_pause);
            listFormatInfo = (TextView) itemView.findViewById(R.id.list_format_info);
            listCheckBox = (CheckBox) itemView.findViewById(R.id.list_checkbox);

            //Register holder for onClick events
            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Log.d(LOG_TAG, "onLongClick");
                    if (actionMode == null) actionMode = getActivity().startActionMode(mActionModeCallback);
                    Log.d(LOG_TAG, "Long pressed on position: " + recyclerView.getChildAdapterPosition(itemView));
                    selectedRecordings.add(recyclerView.getChildAdapterPosition(itemView));
                    recyclerView.getAdapter().notifyDataSetChanged();
                    return false;
                }
            });

        }


        void bindRecordedRadioStation(final File recordingRadioStation, ArrayList<File> list, final int position) {

            Log.d(LOG_TAG, "bindRecordedRadioStation");

            // Reset typeface
            listRecordingName.setTypeface(null, Typeface.NORMAL);
            listRecordingDuration.setTypeface(null, Typeface.NORMAL);
            listRecordingDate.setTypeface(null, Typeface.NORMAL);

            if (!recordingPlaying) {
                if (playPauseToggle.isChecked()) {
                    Log.d(LOG_TAG, "Checked!");
                    playPauseToggle.setChecked(false);
                }
            }


            // Set the content of views
            listRecordingName.setText(recordingRadioStation.getName().substring(0, recordingRadioStation.getName().length() - 4));
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
            listRecordingDate.setText(sdf.format(recordingRadioStation.lastModified()));
            //Lets compute the duration of mp3
            MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
            metaRetriever.setDataSource(recordingRadioStation.getAbsolutePath());
            String duration = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            String bitrate = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE);
            long dur = Long.parseLong(duration) / 1000;
            long hour = dur / 3600;
            long min = (dur % 3600) / 60;
            long res_sec = dur % 60;
            String time = String.format("%02d:%02d:%02d", hour, min, res_sec);

            listRecordingDuration.setText("Duration: " + time);
            listFormatInfo.setText(bitrate + " bps, " + (recordingRadioStation.getName().substring(recordingRadioStation.getName().length() - 3)));

            //If we are in action mode, show check boxes
            if (actionMode != null) {
                listCheckBox.setVisibility(View.VISIBLE);
                //And check it it they are selected
                if (selectedRecordings.contains(position)) {
                    listCheckBox.setChecked(true);
                }
            } else {
                listCheckBox.setVisibility(View.GONE);
                listCheckBox.setChecked(false);
            }


            // Handle clicks on play icon
            playPauseToggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Log.d(LOG_TAG, "Clicked play button on " + recordingRadioStation.getName() + " recording");
                    //RadioPlayerFragment.deselectAllRadioStations(radioStationsList);
                    //RadioPlayerFragment.stopAllRadioStations(radioStationsList);
                   /* radioStation.setSelected(true);
                    selectedRadioStation = radioStation;
                    containerActivity.onRadioStationSelected(radioStationsList.indexOf(radioStation), radioStationsList, false);*/

                    if (playPauseToggle.isChecked()) {

                        Log.d(LOG_TAG, "Play " + recordingRadioStation.getName());
                        containerActivity.onRecordingPlay(recordingRadioStation.getAbsolutePath(), false);
                       // radioStation.setConnecting(true);


                    } else {

                        Log.d(LOG_TAG, "Pause " + recordingRadioStation.getName());
                        containerActivity.onRecordingPlay(recordingRadioStation.getAbsolutePath(), true);
                        //radioStation.setConnecting(false);

                    }

                    //recyclerView.getAdapter().notifyDataSetChanged();
                }
            });


            //Handle click on checkbox
            listCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        selectedRecordings.add(position);
                    } else {
                        selectedRecordings.remove(position);
                    }
                }
            });

        }

        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, getClass().getName() + ":onClick");
            Log.d(LOG_TAG, "Clicked on: " + listRecordingName.getText() + " at position " +
                    getAdapterPosition());


            //Call method on activity
            recordingSelectedListener.onRecordingSelected(getAdapterPosition(), recordedRadioStationList);

        }

    }


    /**
     * Method to instantiate and initialize new RadioStationListFragment object
     *
     * @param refresh This param tells if we are going to refresh station list
     * @return RadioStationListFragment
     */
    public static RadioStationListFragment newInstance(Boolean refresh, RadioStation selectedRadioStation) {

        Bundle args = new Bundle();
        args.putBoolean("refresh", refresh);
        args.putSerializable("radiostation", selectedRadioStation);
        RadioStationListFragment fragment = new RadioStationListFragment();
        fragment.setArguments(args);
        return fragment;

    }


    public RadioStationListFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        selectedRadioStation = (RadioStation) getArguments().getSerializable("radiostation");

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View v = inflater.inflate(R.layout.fragment_radio_station_list, container, false);

        // Retain this fragment across configuration changes.
        setRetainInstance(true);

        recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_fragment);
        swipeRefresh = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh_fragment);

        /* Reference to radio station lab object */
        radioStationLab = RadioStationLab.get(getActivity());

        if (getArguments().getBoolean("refresh")) {

            //Start SwipeRefreshLayout animation (this is workaround, cause there is a bug in api: https://code.google.com/p/android/issues/detail?id=77712)
            swipeRefresh.post(new Runnable() {
                @Override
                public void run() {
                    if (Main2Activity.getInternetConnection() != NetworkUtil.TYPE_NOT_CONNECTED) {
                        swipeRefresh.setRefreshing(true);
                    }
                }
            });

            // Load radio stations from web into database
            radioStationLab.getRadioStationsWeb(REMOTE_SERVER_URL);

            /* Register listener for database refresh */
            radioStationLab.setRadioStationRefreshable(new RadioStationLab.RadioStationRefreshable() {

                //When database is refreshed
                @Override
                public void onRadioStationRefreshed(String s) {

                    //If we didn't receive json object, then it is probably internet problem
                    try {
                        JSONObject jsonObject = new JSONObject(s);
                    } catch (JSONException e) {

                        if (getActivity() != null) {
                            //Notify user via snack bar
                            Snackbar snackbar = Snackbar
                                    .make(v.findViewById(R.id.frame_fragment_layout),
                                            getString(R.string.server_unreachable), Snackbar.LENGTH_INDEFINITE)
                                    .setAction(R.string.ok, new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                        }
                                    });

                            // Changing action text color
                            snackbar.setActionTextColor(Color.YELLOW);

                            // Changing action button text color
                            View sbView = snackbar.getView();
                            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                            textView.setTextColor(Color.WHITE);

                            //In verison v1.0.0 we don't have a server, so we don't show this message
                            //snackbar.show();
                        }

                    }
                    radioStationsList.clear();
                    radioStationsList.addAll(RadioStationLab.getRadioStationsAll());
                    Log.d(LOG_TAG, "Radio station data refreshed!" + radioStationsList.size());

                /* Inform Viewpager's adapter about change */
                    recyclerView.getAdapter().notifyDataSetChanged();

                    //If refreshing animation is running, stop it
                    if (swipeRefresh.isRefreshing()) {
                        Log.d(LOG_TAG, "Refreshing...");
                        swipeRefresh.setRefreshing(false);
                    }


                    //Show recycler view
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        }


        radioStationsList.clear();
        radioStationsList.addAll(RadioStationLab.getRadioStationsAll());
        radioStationAdapter = new RadioStationAdapter(getActivity(), radioStationsList);

        //If the list is partial
        if (list_type != 0) {
            filterRadioStations(list_type);
            radioStationAdapter.notifyDataSetChanged();
        }

        //Setting up recycler view
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        // Setting list divider
        Drawable dividerDrawable = ContextCompat.getDrawable(getActivity(), R.drawable.recycler_list_divider);
        recyclerView.addItemDecoration(new DividerItemDecoration(dividerDrawable));

        //Setting adapter
        recyclerView.setAdapter(radioStationAdapter);
        (recyclerView.getLayoutManager()).scrollToPosition(radioStationsList.indexOf(selectedRadioStation));
        recyclerView.setVisibility(View.VISIBLE);

        // Register listener for swipe refresh, refreshes radio list in background
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d(LOG_TAG, getClass().getName() + ":onRefresh");

                if (isRadioStationListPartial) {
                    //Well do nothing
                    swipeRefresh.setRefreshing(false);
                } else {
                    // Load radio stations from web into database
                    radioStationLab.getRadioStationsWeb(REMOTE_SERVER_URL);
                }
            }
        });


        return v;
    }

    /**
     * Filters list of radio stations using RadioStationLab's functions
     *
     * @param type Type of filtering
     */
    public void filterRadioStations(int type) {

        final TabLayout tabs = (TabLayout)getActivity().findViewById(R.id.tabs);
        if (tabs.getVisibility() == View.GONE) {
            tabs.setVisibility(View.VISIBLE);
        }

        switch (type) {

            case -1:
                //Just return
                return;
            case R.id.all_radio_stations:
                //All, show all radio stations
                radioStationsList.clear();
                radioStationsList.addAll(RadioStationLab.getRadioStationsAll());
                radioStationAdapter.setRadioStationList(radioStationsList);
                radioStationAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(radioStationAdapter);
                break;
            case R.id.favorite_radio_stations:
                //Favorite
                radioStationsList.clear();
                radioStationsList.addAll(RadioStationLab.filterRadioStationByFavorite());
                isRadioStationListPartial = true;
                radioStationAdapter.setRadioStationList(radioStationsList);
                radioStationAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(radioStationAdapter);
                break;
            case R.id.pop_radio_stations:
                //Pop
                radioStationsList.clear();
                radioStationsList.addAll(RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenrePop));
                isRadioStationListPartial = true;
                radioStationAdapter.setRadioStationList(radioStationsList);
                radioStationAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(radioStationAdapter);
                break;
            case R.id.folk_radio_stations:
                //Folk
                radioStationsList.clear();
                radioStationsList.addAll(RadioStationLab.filterRadioStationsByGenre(RadioStationLab.radioGenreFolk));
                isRadioStationListPartial = true;
                radioStationAdapter.setRadioStationList(radioStationsList);
                radioStationAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(radioStationAdapter);
                break;
            case R.id.sarajevo_radio_stations:
                //Location
                radioStationsList.clear();
                radioStationsList.addAll(RadioStationLab.filterRadioStationsByLocation("Sarajevo"));
                isRadioStationListPartial = true;
                radioStationAdapter.setRadioStationList(radioStationsList);
                radioStationAdapter.notifyDataSetChanged();
                recyclerView.setAdapter(radioStationAdapter);
                break;
            case R.id.recordings:
                //This is list of recordings
                recordedRadioStationList.clear();
                String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                String recDirString = baseDir + "/" + RadioStreamRecorder.RECORD_DIR;

                //List files in recording directory
                File recDir = new File(recDirString);
                if (recDir.listFiles() != null) {
                    recordedRadioStationList.addAll(Arrays.asList(recDir.listFiles()));
                }
                recordedRadioStationAdapter = new RecordedRadioStationAdapter(getActivity(), recordedRadioStationList);
                recyclerView.setAdapter(recordedRadioStationAdapter);

                //Hide tab bar
                tabs.setVisibility(View.GONE);

                 /* Stop media player if it is playing */
                Intent stopIntent = new Intent(getActivity(), RadioPlayerService.class);
                stopIntent.setAction(RadioPlayerFragment.ACTION_STOP);
                getActivity().startService(stopIntent);
                break;

            default:
                //Do nothing

        }

    }


    public void filterRadioStationByName(String name) {

        Log.d(LOG_TAG, "filterRadioStationsByName()");

        /*radioStationsList.clear();
        radioStationsList = RadioStationLab.filterRadioStationByName(name);
        isRadioStationListPartial = true;
        radioStationAdapter.setRadioStationList(radioStationsList);
        radioStationAdapter.notifyDataSetChanged();*/

        ArrayList<RadioStation> list = new ArrayList<>();

        //Go through stations
        for (RadioStation radioStation : radioStationsList) {

            if (radioStation.getRadioStationName().toLowerCase().contains(name.toLowerCase())) {
                list.add(radioStation);
            }

        }

        isRadioStationListPartial = true;
        radioStationAdapter.setRadioStationList(list);
        radioStationAdapter.notifyDataSetChanged();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Activity a = null;

        if (context instanceof Activity) {
            a = (Activity) context;
        }

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            radioStationSelectedListener = (OnRadioStationSelectedListener) a;
            recordingSelectedListener = (OnRecordingSelectedListener) a;
            containerActivity = (Main2Activity) a;
        } catch (ClassCastException e) {
            throw new ClassCastException(a.toString()
                    + " must implement OnHeadlineSelectedListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //Bind to service
        Intent intent = new Intent(getActivity(), RadioPlayerService.class);
        getActivity().bindService(intent, serviceConnection, 0);
        bound = true;
    }


    @Override
    public void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");

        if (bound) {
            getActivity().unbindService(serviceConnection);
            bound = false;
        }
    }


    /**
     * Contextual action toolbar
     */
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.action_mode_menu, menu);
            //context_menu = menu;
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

            if (item.getItemId() == R.id.action_delete) {

                //Delete pressed recordings
                int former_s = 0;
                Log.d(LOG_TAG, "blb: " +  selectedRecordings);
                for (Integer s : selectedRecordings) {
                    if (s >  former_s) {
                        s--;
                    }
                    Log.d(LOG_TAG, "Recording: " + recordedRadioStationList.get(s) + " is going to be deleted");
                    //Delete the file from file system
                    recordedRadioStationList.get(s).delete();
                    recordedRadioStationList.remove(recordedRadioStationList.get(s));
                    former_s = s;
                }
                actionMode.finish();
                recordedRadioStationAdapter.notifyDataSetChanged();
                //Notify the user
                Snackbar snackbar = Snackbar
                        .make(getActivity().findViewById(R.id.frame_fragment_layout),
                                getString(R.string.recordings_deleted), Snackbar.LENGTH_SHORT);

                // Changing action button text color
                View sbView = snackbar.getView();
                TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
                textView.setTextColor(Color.WHITE);
                snackbar.show();
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            selectedRecordings.clear();
            recyclerView.getAdapter().notifyDataSetChanged();

        }
    };

}
