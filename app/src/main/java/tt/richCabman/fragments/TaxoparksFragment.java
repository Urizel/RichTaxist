package tt.richCabman.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;
import java.util.ArrayList;
import tt.richCabman.database.DataSource;
import tt.richCabman.model.Taxopark;
import tt.richCabman.R;

public class TaxoparksFragment extends ListFragment {
    ArrayList<Taxopark> taxoparks = new ArrayList<>();
    private Context context;
    private ArrayAdapter taxoparksAdapter;
    private DataSource dataSource;

    public TaxoparksFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context = getContext();
        dataSource = new DataSource(context);
        taxoparks.addAll(dataSource.getTaxoparksSource().getAllTaxoparks());
        taxoparksAdapter = new TaxoparksAdapter(context);
        setListAdapter(taxoparksAdapter);

        View rootView = inflater.inflate(R.layout.fragment_taxoparks, container, false);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        rootView.setLayoutParams(layoutParams);

        (rootView.findViewById(R.id.addTaxopark)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Taxopark taxopark = new Taxopark("", false, 0);
                taxopark.taxoparkID = dataSource.getTaxoparksSource().create(taxopark);
                taxoparks.add(taxopark);
                taxoparksAdapter.notifyDataSetChanged();
            }
        });
        return rootView;
    }


    class TaxoparksAdapter extends ArrayAdapter<Taxopark> {
        public TaxoparksAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1, taxoparks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_entry_taxoparks_editable, parent, false);
            }
            final Taxopark taxopark = getItem(position);

            final EditText etTaxoparkName = (EditText) convertView.findViewById(R.id.etTaxoparkName);
            etTaxoparkName.setText(taxopark.taxoparkName);
            //моментом сохранения считается нажатие enter
            etTaxoparkName.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        saveTaxoparkName(taxopark, etTaxoparkName);
                        return true;
                    }
                    return false;
                }
            });

            //если таксопарк по умолчанию не назначен ни для одного члена списка, назначим текущий парк парком по умолчанию
            boolean defaultTaxoparkDefined = false;
            for (Taxopark taxoparkIter : taxoparks) {
                if (taxoparkIter.isDefault) {
                    defaultTaxoparkDefined = true;
                }
            }
            if (!defaultTaxoparkDefined) {
                taxopark.isDefault = true;
            }

            RadioButton rbDefault = (RadioButton) convertView.findViewById(R.id.rbDefault);
            rbDefault.setChecked(taxopark.isDefault);
            rbDefault.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //сбрасываем все таксопарки в недефолтные и устанавливаем дефолтным текущий
                    for (Taxopark taxoparkIter : taxoparks) {
                        taxoparkIter.isDefault = false;
                        dataSource.getTaxoparksSource().update(taxoparkIter);
                    }
                    taxopark.isDefault = true;
                    dataSource.getTaxoparksSource().update(taxopark);
                    notifyDataSetChanged();
                }
            });

            (convertView.findViewById(R.id.delTaxopark)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    taxoparks.remove(taxopark);
                    dataSource.getTaxoparksSource().remove(taxopark);
                    notifyDataSetChanged();
                }
            });
            return convertView;
        }
        
        private void saveTaxoparkName(Taxopark currentTaxopark, EditText taxoparkName){
            String newName = taxoparkName.getText().toString();
            boolean isInTheList = false;
            for (Taxopark taxoparkIter : dataSource.getTaxoparksSource().getAllTaxoparks()) {
                if (newName.equals(taxoparkIter.taxoparkName) && currentTaxopark.taxoparkID != taxoparkIter.taxoparkID) {
                    isInTheList = true;
                }
            }
            if (isInTheList) {
                Toast.makeText(context, getResources().getString(R.string.taxopark) + " " + String.valueOf(newName) + " " +
                        getResources().getString(R.string.isInTheList), Toast.LENGTH_SHORT).show();
                taxoparkName.setText("");
            }
            else {
                currentTaxopark.taxoparkName = newName;
                dataSource.getTaxoparksSource().update(currentTaxopark);
            }
        }
    }
}
