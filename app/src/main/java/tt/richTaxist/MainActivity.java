package tt.richTaxist;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import tt.richTaxist.DB.Sources.BillingsSource;
import tt.richTaxist.DB.Sources.OrdersSource;
import tt.richTaxist.DB.Sources.ShiftsSource;
import tt.richTaxist.Fragments.OrdersListFragment;
import tt.richTaxist.Units.Order;
import tt.richTaxist.Units.Shift;
import tt.richTaxist.Fragments.OrderFragment;
/**
 * Created by Tau on 08.06.2015.
 */
public class MainActivity extends AppCompatActivity implements
        OrderFragment.OrderFragmentInterface,
        OrdersListFragment.OrdersListInterface {
    //TODO: remove static
    public static Shift currentShift;
    private Order currentOrder = null;
    public final static String CURRENT_ORDER_EXTRA = "currentOrder";
    private final static int TAXIMETER_CALLBACK = 1111;
    private final static int ORDERS_LIST_CALLBACK = 2222;
    private ShiftsSource shiftsSource;
    private OrdersSource ordersSource;
    private BillingsSource billingsSource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        Util.measureScreenWidth(getApplicationContext(), (ViewGroup) findViewById(R.id.container_main));

        shiftsSource = new ShiftsSource(getApplicationContext());
        ordersSource = new OrdersSource(getApplicationContext());
        billingsSource = new BillingsSource(getApplicationContext());

        if (savedInstanceState != null) {
            currentOrder = savedInstanceState.getParcelable(CURRENT_ORDER_EXTRA);
        }

        //фрагментная логика
        if (getResources().getBoolean(R.bool.screenWiderThan450)){
            //TODO: remove
            //we can't statically add OrdersListFragment because OrdersListFragment.adapter.notifyDataSetChanged()
            //doesn't work properly with RecyclerView
            addOrdersListFragment();
        }
        showDetails(currentOrder);
    }

    private void addOrdersListFragment(){
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        OrdersListFragment fragment = new OrdersListFragment();
        ft.replace(R.id.container_orders_list, fragment);
        ft.commit();
    }

    @Override
    public void returnToOrderFragment(Order selectedOrder) {
        currentOrder = selectedOrder;
        showDetails(selectedOrder);
    }

    @Override
    public void addOrder(Order order){
        //order.orderID == -1 if it is newly created order
        boolean saveSuccess;
        if (order.orderID == -1) {
            order.orderID = ordersSource.create(order);
            saveSuccess = order.orderID != -1;
        } else {
            saveSuccess = ordersSource.update(order);
        }
        String msg = saveSuccess ? getResources().getString(R.string.orderSaved) : getResources().getString(R.string.orderNotSaved);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
        currentShift.calculateShiftTotals(0, order.taxoparkID, shiftsSource, ordersSource, billingsSource);
        if (getResources().getBoolean(R.bool.screenWiderThan450)) {
            addOrdersListFragment();
        }
        currentOrder = null;
    }

    @Override
    public Order getOrder() {
        return currentOrder;
    }

    @Override
    public void resetOrder() {
        currentOrder = null;
    }

    @Override
    public void startTaximeter(){
        startActivityForResult(new Intent(this, TaximeterActivity.class), TAXIMETER_CALLBACK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        OrderFragment orderFragment = (OrderFragment) getSupportFragmentManager().findFragmentById(R.id.orderFragment);
        if (orderFragment != null && resultCode == Activity.RESULT_OK) {
            if (requestCode == TAXIMETER_CALLBACK){
                //выполняется после возврата из OrdersListActivity
                int distance = data.getIntExtra(Order.PARAM_DISTANCE, 0);
                long travelTime = data.getIntExtra(Order.PARAM_TRAVEL_TIME, 0);
                addOrder(orderFragment.wrapDataIntoOrder(currentOrder, distance, travelTime));
                currentOrder = null;
            }
            if (requestCode == ORDERS_LIST_CALLBACK){
                //выполняется после возврата из OrdersListActivity
                Order selectedOrder = data.getParcelableExtra(Order.ORDER_KEY);
                if (selectedOrder != null) {
                    showDetails(selectedOrder);
                }
            }
        }
    }

    private void showDetails(Order order) {
        currentOrder = order;
        if (getResources().getBoolean(R.bool.screenWiderThan450)) {
            //ветка только для планшетов или телефонов в ландшафте. точнее для всего, что имеет ширину экрана 450dp+
            OrderFragment orderFragment = new OrderFragment();
            orderFragment.setOrder(currentOrder);
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.container_order, orderFragment);
            ft.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out, android.R.anim.fade_in, android.R.anim.fade_out);
            ft.commit();
        } else {
            OrderFragment orderFragment = (OrderFragment) getSupportFragmentManager().findFragmentById(R.id.orderFragment);
            if (orderFragment != null) {
                orderFragment.setOrder(currentOrder);
            } else {
                Log.d(Constants.LOG_TAG, "MainActivity.showDetails() failed to find OrderFragment");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_show_orders_list).setVisible(!getResources().getBoolean(R.bool.screenWiderThan450));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.action_show_orders_list:
                //item is shown only in portrait
                if (ordersSource.getOrdersListCount(currentShift.shiftID) != 0){
                    startActivityForResult(new Intent(this, OrdersListActivity.class), ORDERS_LIST_CALLBACK);
                } else {
                    Toast.makeText(getApplicationContext(), R.string.noOrdersMSG, Toast.LENGTH_SHORT).show();
                }
                return true;


            case R.id.main_menu:
                startActivity(new Intent(this, FirstScreenActivity.class));
                finish();
                return true;

            case R.id.action_shift_totals:
                Intent intent = new Intent(this, ShiftTotalsActivity.class);
                intent.putExtra("author", "MainActivity");
                startActivity(intent);
                finish();
                return true;

            case R.id.action_grand_totals:
                Intent intent2 = new Intent(this, GrandTotalsActivity.class);
                intent2.putExtra(GrandTotalsActivity.AUTHOR, "MainActivity");
                startActivity(intent2);
                finish();
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(CURRENT_ORDER_EXTRA, currentOrder);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, FirstScreenActivity.class));
        finish();
    }
}
