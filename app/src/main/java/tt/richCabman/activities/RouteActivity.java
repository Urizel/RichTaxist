package tt.richCabman.activities;

import android.app.FragmentTransaction;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.google.android.gms.maps.SupportMapFragment;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import tt.richCabman.fragments.bricks.RangeSeekBar;
import tt.richCabman.util.Constants;
import tt.richCabman.database.DataSource;
import tt.richCabman.R;
import tt.richCabman.model.Shift;
import tt.richCabman.fragments.MapPathFragment;

public class RouteActivity extends FragmentActivity {
    //private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private MapPathFragment mapFragment;
    private AsyncTask updateTask;
    private DataSource dataSource;
    private Shift currentShift;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gps_activity_route);
        setUpMapIfNeeded();
        dataSource = new DataSource(getApplicationContext());

        mapFragment = new MapPathFragment();
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.add(R.id.mapFragment, mapFragment);
        ft.commit();

        if (savedInstanceState == null) {
            //при первом создании активити прочитаем интент и найдем смену в БД
            long shiftID = getIntent().getLongExtra(Constants.SHIFT_ID_EXTRA, -1);
            if (shiftID != -1){
                currentShift = dataSource.getShiftsSource().getShiftByID(shiftID);
            }
        } else {
            long shiftID = savedInstanceState.getLong(Constants.SHIFT_ID_EXTRA);
            currentShift = dataSource.getShiftsSource().getShiftByID(shiftID);
        }

        //add RangeSeekBar to map window
        final Calendar rangeStart = Calendar.getInstance();
        rangeStart.setTime(currentShift.beginShift);
        final Calendar rangeEnd = Calendar.getInstance();
        if (currentShift.isClosed()) rangeEnd.setTime(currentShift.endShift);

        mapFragment.showPath(dataSource.getLocationsSource().getLocationsByShift(currentShift));
        final TextView tvRangeStart = (TextView) findViewById(R.id.tvRangeStart);
        final TextView tvRangeEnd   = (TextView) findViewById(R.id.tvRangeEnd);
        tvRangeStart.setText(getStringDateTimeFromCal(rangeStart));
        tvRangeEnd  .setText(getStringDateTimeFromCal(rangeEnd));

        final RangeSeekBar<Long> seekBar = new RangeSeekBar<>(rangeStart.getTimeInMillis(), rangeEnd.getTimeInMillis(), this);
        seekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener<Long>() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar<?> bar, Long minValue, Long maxValue) {
                // handle changed range values
                if (updateTask != null) updateTask.cancel(true);
                rangeStart.setTimeInMillis(minValue);
                rangeEnd.setTimeInMillis(maxValue);
                tvRangeStart.setText(getStringDateTimeFromCal(rangeStart));
                tvRangeEnd.setText(getStringDateTimeFromCal(rangeEnd));
                mapFragment.showPath(dataSource.getLocationsSource().getLocationsByPeriod(rangeStart.getTime(), rangeEnd.getTime()));
            }
        });

        // add RangeSeekBar to pre-defined layout
        FrameLayout layout = (FrameLayout) findViewById(R.id.seekBarPlaceHolderInRouteActivity);
        layout.addView(seekBar);


        updateTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                while (true) {
                    try {
                        if (isCancelled()) break;
                        TimeUnit.SECONDS.sleep(4);
                        publishProgress();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Object[] values) {
                //super.onProgressUpdate(values);
                //Logger.d("Updating map");
                try {
                    seekBar.setNormalizedMaxValue(Calendar.getInstance().getTimeInMillis());
                    mapFragment.showPath(dataSource.getLocationsSource().getLocationsByPeriod
                            (rangeStart.getTime(), Calendar.getInstance().getTime()));
                } catch (Exception e) {
                    e.printStackTrace();
                    updateTask.cancel(true);
                }
            }
        };
        updateTask.execute();
    }

    private String getStringDateTimeFromCal(Calendar cal){
        return String.format("%02d.%02d.%02d %02d:%02d", cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR) % 100,
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTask!=null) updateTask.cancel(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {link mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        /*if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }*/
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that mMap is not null.
     */
    private void setUpMap() {
        //mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(Constants.SHIFT_ID_EXTRA, currentShift.shiftID);
    }
}
