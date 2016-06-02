package com.mcgowan.timetable.itsligotimetables;

import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mcgowan.timetable.scraper.*;

/**
 * A placeholder fragment containing a simple view.
 */
public class TimeTableFragment extends Fragment {

    private String timetableUrl;
    private String studentID;
    ArrayAdapter<String> mTimetableAdapter;

    public TimeTableFragment() {
        this.timetableUrl = "https://itsligo.ie/student-hub/my-timetable/";
        this.studentID = "S00165159";
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //enable fragment to handle menu events
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_timetablefragmemt, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            FetchTimeTableTask task = new FetchTimeTableTask();
            task.execute(timetableUrl, studentID);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        List<String> sampleTimetable = new ArrayList<>();
        sampleTimetable.add("Class One");
        sampleTimetable.add("Class Two");
        sampleTimetable.add("Class Three");
        sampleTimetable.add("Class Four");
        sampleTimetable.add("Class Five");

        mTimetableAdapter = new ArrayAdapter<>(
                getActivity(),
                R.layout.list_item_class,
                R.id.list_item_class_textview,
                sampleTimetable);

        ListView lv = (ListView) rootView.findViewById(R.id.listview_timetable);
        lv.setAdapter(mTimetableAdapter);


        return rootView;
    }
    class FetchTimeTableTask extends AsyncTask<String, Void, List<String>> {
        private final String LOG_TAG = FetchTimeTableTask.class.getSimpleName();


        @Override
        protected List<String> doInBackground(String... params) {
            String timetableData;

            if (params.length == 0) {
                return null;
            }

            String url = params[0];
            String studentID = params[1];

            try {
                TimeTable t = new TimeTable(url, studentID);
                timetableData = t.toString();
                List<String> classes = getClassesAsArray(t);

                for (String s: classes) {
                    Log.e(LOG_TAG, s);
                }

                return classes;
                //return here

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error", e);
                return null;
            }
//        return null;
        }

        private List<String> getClassesAsArray(TimeTable t) {
            List<String> classes = new ArrayList<String>();
            Map<String, List<Course>> days = t.getDays();

            for (Map.Entry<String, List<Course>> entry : days.entrySet()) {
                for (Course c : entry.getValue()) {
                    String line = c.toString();
                    classes.add(line);
                }
            }
            return classes;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            if (result != null) {
                mTimetableAdapter.clear();
                for (String record: result) {
                    mTimetableAdapter.add(record);
                }
            }
        }
    }
}

