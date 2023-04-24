/*
     This file is part of the Android app ch.bfh.securevote.
     (C) 2023 Benjamin Fehrensen (and other contributing authors)
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package ch.bfh.securevote;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.Executors;
import ch.bfh.securevote.databinding.FragmentRegisterBinding;
import ch.bfh.securevote.utils.Constants;
import ch.bfh.securevote.utils.HpcUtility;
import ch.bfh.securevote.utils.NetworkJsonReceiver;
import ch.bfh.securevote.utils.SharedData;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private static final String TAG = RegisterFragment.class.getName();
    private SharedPreferences prefs;
    private ArrayAdapter<CharSequence> ktAdapter;
    private ArrayAdapter<CharSequence> arrAdapter;
    private ArrayAdapter<CharSequence> cvAdapter;
    private HpcUtility hpcUtility;
    private SharedData sharedData;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        sharedData = new ViewModelProvider(requireActivity()).get(SharedData.class);
        try {
            prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
            Log.i(TAG, String.format("Attestation key alias %s.", prefs.getString(Constants.settings_key_name,Constants.defaultKeyAliasName)));
            hpcUtility = HpcUtility.getInstance(prefs);
        }catch (Exception ex){
            Log.e(TAG, "Error loading preferences: "+ex);
        }

        binding = FragmentRegisterBinding.inflate(inflater, container, false);

        //get challenge online
        if (sharedData.isConnected()) {
            NetworkJsonReceiver net = new NetworkJsonReceiver(Constants.UUID_URL, 200);
            net.setResultListener(result -> requireActivity().runOnUiThread(() -> setChallenge(result)));
            Executors.newSingleThreadExecutor().execute(net);
        } else {
            setChallenge(null);
        }

        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonRegister.setOnClickListener(view1 -> {
            // Check if we have already a key pair
            if (HpcUtility.hasKey()){
                Snackbar.make(view1, R.string.we_have_already_a_key, Snackbar.LENGTH_LONG)
                        .setAction(R.string.key_available, null).show();
                binding.buttonRegister.setEnabled(false);
                return;
            }
            // Check for key name
            if (TextUtils.isEmpty(binding.inputKeyName.getText())){
                binding.inputKeyName.setText(prefs.getString("attestation_key_alias", Constants.defaultKeyAliasName));
            }
            // Check for APC setting
            if (!binding.checkboxApc.isChecked()) {
                binding.checkboxApc.setChecked(true);
                Snackbar.make(view1, R.string.must_set_apc, Snackbar.LENGTH_LONG)
                        .setAction("Sorry", null).show();
            }
            String keyType=binding.keyTypeSelector.getSelectedItem().toString();
            if (keyType.contains("EC")) {
                // Check if the ec type is supported. Strongbox only supports secp256r1 keys
                if (binding.checkboxHw.isChecked()) {
                    if (!binding.keyAttrSelector.getSelectedItem().toString().contains("256")) {
                        Snackbar.make(view1, R.string.select_ec_default, Snackbar.LENGTH_LONG)
                                .setAction("Note", null).show();
                        binding.keyAttrSelector.setSelection(arrAdapter.getPosition(Constants.settings_ec_curve_default));
                    }
                }
            } else {
                //RSA key length check
                if (binding.checkboxHw.isChecked()) {
                    if (!binding.keyAttrSelector.getSelectedItem().toString().contains("4096")) {
                        Snackbar.make(view1, R.string.select_rsa_default, Snackbar.LENGTH_LONG)
                                .setAction("Note", null).show();
                        binding.keyAttrSelector.setSelection(arrAdapter.getPosition(Constants.settings_rsa_key_length_default));
                    }
                }
            }
            // Store values displayed
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(Constants.settings_strong_box_required, binding.checkboxHw.isChecked());
            editor.putBoolean(Constants.settings_user_authentication_required, binding.checkboxAuth.isChecked());
            editor.putBoolean(Constants.settings_unlock_device_required, binding.checkboxUnlock.isChecked());
            editor.putString(Constants.settings_key_name,binding.inputKeyName.getText().toString());
            String[] validityArr = getResources().getStringArray(R.array.cert_validity_values);
            editor.putString(Constants.settings_cert_validity, validityArr[binding.certValiditySelector.getSelectedItemPosition()]);
            editor.putString(Constants.settings_key_type,keyType);
            if (keyType.contains("EC")) {
                editor.putString(Constants.settings_ec_curve, binding.keyAttrSelector.getSelectedItem().toString());
            } else {
                editor.putString(Constants.settings_rsa_key_length, binding.keyAttrSelector.getSelectedItem().toString());
            }
            editor.apply();

            // Generate key pair
            String ret = hpcUtility.register(binding.challenge.getText().toString().getBytes());
            binding.textviewStatus.setText(ret);
            if (ret.contains("Congratulation")){
                Snackbar.make(view1, R.string.key_generated, Snackbar.LENGTH_LONG)
                            .setAction(R.string.success, null).show();
                binding.buttonRegister.setEnabled(false);
                // go back
                NavHostFragment.findNavController(RegisterFragment.this)
                        .navigate(R.id.action_HpcFragment);

            }else{
                Snackbar.make(view1, R.string.key_generation_failed, Snackbar.LENGTH_LONG)
                        .setAction("Sorry", null).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        binding.checkboxUnlock.setChecked(prefs.getBoolean(Constants.settings_unlock_device_required, Constants.settings_unlock_device_required_default));
        binding.checkboxAuth.setChecked(prefs.getBoolean(Constants.settings_user_authentication_required, Constants.settings_user_authentication_required_default));
        binding.checkboxHw.setChecked(prefs.getBoolean(Constants.settings_strong_box_required, Constants.settings_strong_box_required_default));
        binding.inputKeyName.setText(prefs.getString(Constants.settings_key_name, Constants.defaultKeyAliasName));
        //binding.challenge.setText(getChallenge()); // retrieve online
        //key type adapter
        ktAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.key_types, android.R.layout.simple_spinner_item);
        ktAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.keyTypeSelector.setAdapter(ktAdapter);
        String keyType=prefs.getString(Constants.settings_key_type, Constants.settings_key_type_default);
        binding.keyTypeSelector.setSelection(ktAdapter.getPosition(keyType));
        binding.keyTypeSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setupKeySelection(ktAdapter.getItem(position).toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        setupKeySelection(keyType);
        //cert validity length
        cvAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.cert_validity_options, android.R.layout.simple_spinner_item);
        cvAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.certValiditySelector.setAdapter(cvAdapter);
        String[] valArr = getResources().getStringArray(R.array.cert_validity_values);
        binding.certValiditySelector.setSelection(Arrays.asList(valArr).indexOf(prefs.getString(Constants.settings_cert_validity, Constants.settings_cert_validity_default)));

    }

    /**
     * Setup the key selection based on the key type. APC supports EC or RSA key pairs.
     * @param type
     */
    protected void setupKeySelection(String type){
        if (type.contains("EC")){
            binding.labelSelection.setText(R.string.select_ec_type);
            arrAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.ec_type, android.R.layout.simple_spinner_item);
            arrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.keyAttrSelector.setAdapter(arrAdapter);
            binding.keyAttrSelector.setSelection(arrAdapter.getPosition(prefs.getString(Constants.settings_ec_curve, Constants.settings_ec_curve_default)));
        } else if (type.contains("RSA")){
            //rsa settings
            binding.labelSelection.setText(R.string.select_rsa_key_length);
            arrAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.rsa_key_length, android.R.layout.simple_spinner_item);
            arrAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            binding.keyAttrSelector.setAdapter(arrAdapter);
            binding.keyAttrSelector.setSelection(arrAdapter.getPosition(prefs.getString(Constants.settings_rsa_key_length, Constants.settings_rsa_key_length_default)));
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Set the challenge to the textview. This challenge is used for the APC key.
     * @param uuid
     */
    protected void setChallenge(String uuid){
        String challenge = null;
        if (uuid != null){
            try{
                challenge = parseJson(uuid, "uuid");
            } catch (Exception ex){
                Log.e(TAG, String.format("Failed to parse uuid '%s'", uuid));
            }
        }
        if (challenge != null){
            Log.i(TAG, String.format("Setting internet challenge '%s'", challenge));
            binding.challenge.setText(challenge);
        }else{
            Log.e(TAG, "Setting local challenge");
            binding.challenge.setText(getChallenge());
        }
    }

    public String parseJson(String jsonIn, String field) throws JSONException {
        JSONObject jo = new JSONObject(jsonIn);
        return jo.get(field).toString();
    }

    private String getChallenge(){
        return UUID.randomUUID().toString();
    }
}