package com.mcgowan.timetable.itsligotimetables.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;
import android.util.Log;

import com.mcgowan.timetable.itsligotimetables.MainActivity;
import com.mcgowan.timetable.itsligotimetables.R;
import com.mcgowan.timetable.itsligotimetables.Utility;
import com.mcgowan.timetable.itsligotimetables.data.TimetableContract;
import com.mcgowan.timetable.scraper.TimeTable;

import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Vector;


public class TimetableSyncAdapter extends AbstractThreadedSyncAdapter {
    public final String LOG_TAG = TimetableSyncAdapter.class.getSimpleName();


    public static final int SYNC_INTERVAL = 60 * 720;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL / 3;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SERVER_STATUS_OK,
            SERVER_STATUS_SERVER_DOWN,
            SERVER_STATUS_SERVER_INVALID,
            SERVER_STATUS_UNKNOWN
    })
    public @interface ServerStatus {
    }

    public static final int SERVER_STATUS_OK = 0;
    public static final int SERVER_STATUS_SERVER_DOWN = 1;
    public static final int SERVER_STATUS_SERVER_INVALID = 2;
    public static final int SERVER_STATUS_UNKNOWN = 3;

    public TimetableSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.d(LOG_TAG, "onPerformSync Called.");
        //todo check here for null for student id

        String url = MainActivity.TIMETABLE_URL;
        String studentID = Utility.getStudentId(getContext());

//        if (studentID.equals("")) {
//            setServerStatus(getContext(), SERVER_STATUS_UNKNOWN);
//            return;
//        }

        try {
            TimeTable t = new TimeTable(url, studentID);
            Vector<ContentValues> cVector = Utility.convertTimeTableToVector(t, studentID);

            if (cVector.size() > 0) {
                Utility.deleteRecordsFromDatabase(getContext(), studentID);
                Utility.addRecordsToDatabase(getContext(), cVector);
                createCursorFromDatabase(studentID);
                setServerStatus(getContext(), SERVER_STATUS_OK);
            } else {
                Utility.deleteAllRecordsFromDatabase(getContext());
            }
        } catch (HttpStatusException e) {
            Log.e(LOG_TAG, "Error connecting to ITS website", e);
            e.printStackTrace();
            setServerStatus(getContext(), SERVER_STATUS_SERVER_DOWN);
        } catch (IOException e) {
            e.printStackTrace();
            setServerStatus(getContext(), SERVER_STATUS_UNKNOWN);
        }

    }

    private void createCursorFromDatabase(String studentID) {
        Uri timeTableUri = TimetableContract.TimetableEntry.buildTimetableWithStudentId(studentID);

        Cursor cursor = getContext().getContentResolver().query(timeTableUri, null, null, null, null);


        Vector<ContentValues> cvVector = new Vector<ContentValues>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cursor, cv);
                cvVector.add(cv);
            } while (cursor.moveToNext());
        }
    }

    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        TimetableSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }

    /**
     * Helper method to have the sync adapter sync immediately
     *
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if (null == accountManager.getPassword(newAccount)) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */
            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    /**
     * Sets the server status to shared preferences. This function shouldn't be called from
     * the UI thread because it uses commit to write to the shared preferences.
     *
     * @param context
     * @param serverStatus
     */
    private static void setServerStatus(Context context, @ServerStatus int serverStatus) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor spe = prefs.edit();
        spe.putInt(context.getString(R.string.server_status_key), serverStatus);
        spe.commit();
    }
}
