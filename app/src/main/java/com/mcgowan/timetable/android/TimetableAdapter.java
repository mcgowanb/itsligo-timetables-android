package com.mcgowan.timetable.android;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mcgowan.timetable.android.utility.Utility;

public class TimetableAdapter extends CursorAdapter {
    private static final String LOG_TAG = TimeTableWeekFragment.class.getSimpleName();

    public TimetableAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    private final int VIEW_TYPE_NEXT = 0;
    private final int VIEW_TYPE_ALL = 1;


    private String convertCursorRowToUXFormat(Cursor cursor) {
        // get row indices for our cursor
        String result = String.format("%s : %s : %s : %s : %s : %s : %s",
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_DAY),
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_TIME),
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_SUBJECT),
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_LECTURER),
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_ROOM),
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_ID),
                cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_DAY_ID)
        );

        return result;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_NEXT : VIEW_TYPE_ALL;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    /*
            Remember that these views are reused as needed.
         */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {

        int layoutId = R.layout.list_item_class;

        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);

        return view;
    }

    /*
        This is where we fill-in the views with the contents of the cursor.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        String day = cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_DAY);
        viewHolder.iconView.setImageResource(Utility.getDayIcon(context, day));

        String startTime = cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_START_TIME);
        viewHolder.startTime.setText(startTime);

        String endTime = cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_END_TIME);
        viewHolder.endTime.setText(endTime);

        String subject = cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_SUBJECT);
        viewHolder.subject.setText(subject);

        String room = cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_ROOM);
        viewHolder.room.setText(room);



        String lecturer = cursor.getString(TimeTableWeekFragment.COL_TIMETABLE_LECTURER);
        viewHolder.lecturer.setText(lecturer);

    }

    public static class ViewHolder{
        public final ImageView iconView;
        public final TextView startTime;
        public final TextView endTime;
        public final TextView subject;
        public final TextView lecturer;
        public final TextView room;

        public ViewHolder(View view) {
            this.iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            this.startTime = (TextView) view.findViewById( R.id.list_item_start_time);
            this.endTime = (TextView) view.findViewById( R.id.list_item_end_time);
            this.subject = (TextView) view.findViewById(R.id.list_item_subject);
            this.lecturer = (TextView) view.findViewById(R.id.list_item_lecturer);
            this.room = (TextView) view.findViewById(R.id.list_item_room);
        }
    }




}
