package tt.richCabman.activities;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import tt.richCabman.R;
import tt.richCabman.util.Logger;
import tt.richCabman.util.Util;
import tt.richCabman.model.Order;
import tt.richCabman.model.Coordinates;
import tt.richCabman.services.GPSService;
/**
 * Created by AlexShredder on 07.07.2015.
 */
public class TaximeterActivity extends AppCompatActivity{
    static Context context;
    private int distance;
    private long travelTime;
    private Date startTime;
    private List<Coordinates> coordinatesList;

    private UpdateTimeTask updateTimeTask;
    private TextView travelTimeTextView;
    private TextView distanceTextView;

    private ServiceConnection serviceConnection;
    private GPSService gpsService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taximeter);
        context = getApplicationContext();
        Util.measureScreenWidth(context, (ViewGroup) findViewById(R.id.activity_order));

        distance = 0;
        travelTime = 0;
        coordinatesList = new ArrayList<>();
        Logger.d("onCreate");

        try {
            stopService(new Intent(this, GPSService.class));
        } catch (Exception e) {
            Logger.d("Ошибка остановки сервиса");
        }

       Intent intent = new Intent(TaximeterActivity.this, GPSService.class);//.putExtra(GPSHelper.PARAM_PINTENT, pi);
        // стартуем сервис
        startService(intent);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                gpsService = ((GPSService.MyBinder) service).getService();
                // Создаем PendingIntent
                PendingIntent pi = createPendingResult(GPSHelper.GPS_REQUEST_FROM_ORDER, getIntent(), 0);
                gpsService.setPendingIntent(pi);
                gpsService.restart();
                Logger.d("Service connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Logger.d("Service disconnected");
            }
        };
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);


        startTime = new Date();
        travelTimeTextView = (TextView) findViewById(R.id.travelTimeTextView);
        distanceTextView = (TextView) findViewById(R.id.distanceTextView);

        Button buttonCloseOrder = (Button) findViewById(R.id.buttonCloseOrder);
        if (buttonCloseOrder != null) {
            buttonCloseOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {
                    Intent response = new Intent();
                    response.putExtra(Order.PARAM_DISTANCE, distance);
                    response.putExtra(Order.PARAM_TRAVEL_TIME, travelTime);
                    setResult(RESULT_OK, response);//RESULT_CANCELED если закрываем аппаратным возвратом
                    finish();
                }
            });
        }

        Button buttonOpenMap = (Button) findViewById(R.id.buttonOpenMap);
        if (buttonOpenMap != null) {
            buttonOpenMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View p1) {
                    startActivity(new Intent(TaximeterActivity.this, GPSHelper.getLocActivityClass()));
                }
            });
        }

        updateTimeTask = new UpdateTimeTask();
        updateTimeTask.execute();
    }

    @Override
    protected void onDestroy() {
        Logger.d("On destroy");

        try {
            unbindService(serviceConnection);
            serviceConnection = null;
            stopService(new Intent(this, GPSService.class));
        } catch (Exception e) {
            Logger.d("Ошибка остановки сервиса");
        }
        if (updateTimeTask != null) updateTimeTask.cancel(true);

        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.d("on activity result: " + requestCode + " " + resultCode);
        if (requestCode == GPSHelper.GPS_REQUEST_FROM_ORDER && resultCode == GPSHelper.PARAM_RETURN_DATA){
            distance   = data.getIntExtra(GPSHelper.PARAM_DISTANCE, 0);
            double lat = data.getDoubleExtra(GPSHelper.PARAM_LAT, 0);
            double lon = data.getDoubleExtra(GPSHelper.PARAM_LON, 0);
            coordinatesList.add(new Coordinates(lon,lat));
            distanceTextView.setText(String.format("%d km %d m", distance / 1000, distance % 1000));
        }
    }

    @Override
    protected void onResume() {
        Logger.d("onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Logger.d("OnPause");
        super.onPause();
    }


    class UpdateTimeTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            travelTimeTextView.setText("---");
            Logger.d("UTT: Preexecute");
        }

        @Override
        protected Void doInBackground(Void... params) {
            Logger.d("UTT: doInBackground");
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(1);
                    publishProgress();
                    if (isCancelled()) break;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            Date endTime = new Date();
            Calendar diff = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            diff.setTimeInMillis(endTime.getTime() - startTime.getTime());
            travelTimeTextView.setText(getCoolTime(diff));
        }

        @Override
        protected void onPostExecute(Void result) {
            Logger.d("UTT: onPostExecute");
            super.onPostExecute(result);
            travelTimeTextView.setText("stop travel");
        }

        @Override
        protected void onCancelled() {
            Logger.d("UTT: cancelled");
            super.onCancelled();
        }

        private String getCoolTime(Calendar time) {
            travelTime = time.getTimeInMillis();

            int day  = (time.get(Calendar.DAY_OF_YEAR) - 1) * 24;
            int hour   = time.get(Calendar.HOUR_OF_DAY);
            int minute = time.get(Calendar.MINUTE);
            int second = time.get(Calendar.SECOND);
            return String.format("%dд  %02d:%02d:%02d", day, hour, minute, second);
        }
    }
}
