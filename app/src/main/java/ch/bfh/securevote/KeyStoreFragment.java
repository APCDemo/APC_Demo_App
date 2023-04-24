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

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.security.cert.Certificate;
import java.util.Enumeration;
import ch.bfh.securevote.databinding.FragmentKeyStoreBinding;
import ch.bfh.securevote.gui.SingleKeyLayout;
import ch.bfh.securevote.utils.CertificateParser;
import ch.bfh.securevote.utils.HpcUtility;
import com.google.android.attestation.ParsedAttestationRecord;

public class KeyStoreFragment extends Fragment {

    private FragmentKeyStoreBinding binding;
    private static final String TAG = MainActivity.class.getName();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentKeyStoreBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.textviewKeystore.append(getKeyStore());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public String getKeyStore(){
        StringBuilder sb = new StringBuilder();
        Enumeration<String> aliases = HpcUtility.getKeyStore();
        if (aliases!=null){
            int count=0;
            while(aliases.hasMoreElements()) {
                count++;
                String alias = aliases.nextElement();
                Log.i(TAG, alias);
                Certificate cert = HpcUtility.getCert(alias);
                addAliases(alias, cert);
            }
            sb.append(String.format(getResources().getString(R.string.number_of_objects), count));
        }else{
            sb.append(getResources().getString(R.string.no_object_found));
        }
        return sb.toString();
    }

    protected void addAliases(String alias, Certificate cert){
        SingleKeyLayout entry = new SingleKeyLayout(getContext());
        binding.layoutKeys.addView(entry);
        entry.labelTextView.setText(alias);
        String keyType=CertificateParser.getKeyType(cert);
        entry.keyTypeView.setText(keyType);
        if (keyType.contains("RSA")){
            entry.keyAttrView.setText(R.string.rsa_key_length);
            entry.keyAttrValView.setText(CertificateParser.getRsaKeyLength(cert));
        }else{
            entry.keyAttrView.setText(R.string.ec_algorithm);
            entry.keyAttrValView.setText(CertificateParser.getEcType(cert));
        }

        ParsedAttestationRecord attestationRecords = CertificateParser.getAttestationExtension(cert);
        entry.keyStoreView.setText(attestationRecords.keymasterSecurityLevel.name());
        if (attestationRecords.keymasterSecurityLevel.name().contains("STRONG")){
            entry.keyStoreView.setText(R.string.SE);
        }else if (attestationRecords.keymasterSecurityLevel.name().contains("TRUSTED")){
            entry.keyStoreView.setText(R.string.TEE);
        }
        if (attestationRecords.teeEnforced.userAuthType.isPresent()){
            entry.keyAuthenticationView.setText(attestationRecords.teeEnforced.userAuthType.get().toString());
        }else{
            entry.keyAuthenticationView.setText("-");
        }

        entry.removeButton.setOnClickListener(view -> {
            HpcUtility.deleteKey(alias);
            entry.removeButton.setEnabled(false);

            // go back
            NavHostFragment.findNavController(KeyStoreFragment.this)
                    .navigate(R.id.action_HpcFragment);
        });
    }
}