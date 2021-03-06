package tt.richCabman.fragments;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;
import java.util.Calendar;
import tt.richCabman.fragments.bricks.CustomSpinner;
import tt.richCabman.fragments.bricks.CustomSpinner.TypeOfSpinner;
import tt.richCabman.fragments.bricks.DateTimeButtons;
import tt.richCabman.model.Order;
import tt.richCabman.model.TypeOfPayment;
import tt.richCabman.R;
import tt.richCabman.activities.Settings4ParksAndBillingsActivity;
import tt.richCabman.util.Logger;
import tt.richCabman.util.Util;

public class OrderFragment extends Fragment implements DateTimeButtons.DateTimeButtonsInterface {
    private OrderFragmentInterface mListener;

    private Calendar arrivalDateTime;
    private RadioGroup typeOfPaymentUI;
    private EditText etPrice, etNote;
    private CustomSpinner spnTaxopark, spnBilling;

    public void setOrder(Order order){
        Logger.d("OrderFragment.setOrder() " + String.valueOf(order == null ? "null" : order.price));
        try { refreshWidgets(order);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try { mListener = (OrderFragmentInterface) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement " + mListener.getClass().getName());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Logger.d("OrderFragment.onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_order, container, false);

        typeOfPaymentUI = (RadioGroup)    rootView.findViewById(R.id.payTypeRadioGroup);
        etPrice         = (EditText)      rootView.findViewById(R.id.etPrice);
        etNote          = (EditText)      rootView.findViewById(R.id.etNote);
        spnTaxopark     = (CustomSpinner) rootView.findViewById(R.id.spnTaxopark);
        spnBilling      = (CustomSpinner) rootView.findViewById(R.id.spnBilling);

        rootView.findViewById(R.id.btnAddNewOrder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Util.showTaxometer){
                    mListener.startTaximeter();
                } else {
                    Order order = wrapDataIntoOrder(mListener.getOrder(), 0, 0);
                    mListener.addOrder(order);
                    refreshWidgets(null);
                }
            }
        });
        rootView.findViewById(R.id.btnClearForm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshWidgets(null);
                Toast.makeText(getContext(), R.string.formClearedMSG, Toast.LENGTH_SHORT).show();
            }
        });

        rootView.findViewById(R.id.btnTaxopark).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), Settings4ParksAndBillingsActivity.class));
            }
        });
        rootView.findViewById(R.id.btnBilling).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), Settings4ParksAndBillingsActivity.class));
            }
        });
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        createTaxoparkSpinner();
        createBillingSpinner();
        refreshWidgets(mListener.getOrder());
    }

    public void createTaxoparkSpinner(){
        spnTaxopark.createSpinner(TypeOfSpinner.TAXOPARK, false);
        spnTaxopark.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View itemSelected, int selectedItemPosition, long selectedId) {
                spnTaxopark.saveSpinner(TypeOfSpinner.TAXOPARK);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {/*NOP*/}
        });
    }
    public void createBillingSpinner(){
        spnBilling.createSpinner(TypeOfSpinner.BILLING, false);
        spnBilling.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View itemSelected, int selectedItemPosition, long selectedId) {
                spnBilling.saveSpinner(TypeOfSpinner.BILLING);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {/*NOP*/}
        });
    }

    private TypeOfPayment getRadioState(){
        switch (typeOfPaymentUI.getCheckedRadioButtonId()){
            case R.id.choiceCash:   return TypeOfPayment.CASH;
            case R.id.choiceCard:   return TypeOfPayment.CARD;
            case R.id.choiceBonus:  return TypeOfPayment.TIP;
            default:                throw new IllegalArgumentException("unknown TypeOfPayment");
        }
    }

    private void refreshWidgets(Order receivedOrder){
        if (receivedOrder != null) {
            arrivalDateTime = Calendar.getInstance();
            arrivalDateTime.setTime(receivedOrder.arrivalDateTime);
            etPrice.setText(String.valueOf(receivedOrder.price));
            switch (TypeOfPayment.getById(receivedOrder.typeOfPaymentID)) {
                case CASH:  typeOfPaymentUI.check(R.id.choiceCash); break;
                case CARD:  typeOfPaymentUI.check(R.id.choiceCard);  break;
                case TIP:   typeOfPaymentUI.check(R.id.choiceBonus); break;
                default:    typeOfPaymentUI.check(R.id.choiceCash);
            }
            etNote.setText(receivedOrder.note);
            spnTaxopark.setPositionOfSpinner(TypeOfSpinner.TAXOPARK, receivedOrder.taxoparkID);
            spnBilling.setPositionOfSpinner(TypeOfSpinner.BILLING, receivedOrder.billingID);
        } else{
            //is used to clear widgets if receivedOrder == null
            mListener.resetOrder();
            arrivalDateTime = Calendar.getInstance();
            etPrice.setText("");
            typeOfPaymentUI.check(R.id.choiceCash);
            etNote.setText("");
            spnTaxopark.setPositionOfSpinner(TypeOfSpinner.TAXOPARK, -2);
            spnBilling.setPositionOfSpinner(TypeOfSpinner.BILLING, 0);
        }

        //Nested fragments are only supported when added to a fragment dynamically.
        FragmentManager fragmentManager = getChildFragmentManager();
        DateTimeButtons buttonsFragment = (DateTimeButtons) fragmentManager.findFragmentByTag(DateTimeButtons.FRAGMENT_TAG);
        if (buttonsFragment == null) {
            buttonsFragment = new DateTimeButtons();
            Bundle args = new Bundle();
            args.putLong(DateTimeButtons.DATE_TIME_EXTRA, arrivalDateTime.getTimeInMillis());
            buttonsFragment.setArguments(args);
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.replace(R.id.dateTimePlaceHolder, buttonsFragment, DateTimeButtons.FRAGMENT_TAG);
            ft.commit();
        } else {
            buttonsFragment.setDateTime(arrivalDateTime);
        }
    }

    public Order wrapDataIntoOrder(Order order, int distance, long travelTime) {
        int price;
        try {
            price = Integer.parseInt(etPrice.getText().toString());
        } catch (NumberFormatException e) {
            Logger.d(getResources().getString(R.string.wrongPriceErrMsg));
            Toast.makeText(getContext(), String.valueOf(getResources().getString(R.string.wrongPriceErrMsg)), Toast.LENGTH_LONG).show();
            price = 0;
        }
        String note;
        try {
            note = etNote.getText().toString();
        } catch (Exception e) {
            Logger.d(getResources().getString(R.string.wrongNoteErrMsg));
            Toast.makeText(getContext(), String.valueOf(getResources().getString(R.string.wrongNoteErrMsg)), Toast.LENGTH_LONG).show();
            note = "";
        }
        if (order == null) {
            order = new Order(arrivalDateTime.getTime(), price, getRadioState().id, mListener.getCurrentShiftId(), note,
                    distance, travelTime, spnTaxopark.taxoparkID, spnBilling.billingID);
        } else {
            order.update(arrivalDateTime.getTime(), price, getRadioState().id, mListener.getCurrentShiftId(), note,
                    distance, travelTime, spnTaxopark.taxoparkID, spnBilling.billingID);
        }
        //fragment does not encapsulate db work. it only gets data from user and wraps it in Order
        //it doesn't know what to do with Order. this is job for fragment host
        return order;
    }

    public void onDateOrTimeSet(Calendar cal){
        arrivalDateTime.setTimeInMillis(cal.getTimeInMillis());
    }

    public interface OrderFragmentInterface {
        void addOrder(Order order);
        Order getOrder();
        void resetOrder();
        void startTaximeter();
        long getCurrentShiftId();
    }
}
