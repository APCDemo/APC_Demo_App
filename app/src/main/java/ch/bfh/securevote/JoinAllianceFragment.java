/* Copyright 2019, The Android Open Source Project, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import static ch.bfh.securevote.utils.HpcUtility.getCert;
import static ch.bfh.securevote.utils.HpcUtility.getSignatureAlgorithm;
import static ch.bfh.securevote.utils.HpcUtility.requiresAuthentication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.security.ConfirmationAlreadyPresentingException;
import android.security.ConfirmationCallback;
import android.security.ConfirmationNotAvailableException;
import android.security.ConfirmationPrompt;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.google.android.material.snackbar.Snackbar;

import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import ch.bfh.securevote.databinding.FragmentJoinAllianceBinding;
import ch.bfh.securevote.utils.Constants;
import ch.bfh.securevote.utils.HpcUtility;
import ch.bfh.securevote.utils.NetworkPostMessage;
import ch.bfh.securevote.utils.PKCS7Builder;
import ch.bfh.securevote.utils.SharedData;


public class JoinAllianceFragment extends Fragment {

    private static final String TAG = JoinAllianceFragment.class.getName();
    private SharedData sharedData;
    private ArrayList<CheckBox> checkBoxes = new ArrayList<>();

    private FragmentJoinAllianceBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentJoinAllianceBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        sharedData = new ViewModelProvider(requireActivity()).get(SharedData.class);
        binding.buttonConfirm.setOnClickListener(view1 -> {
            // Check if APC is supported
            if (!ConfirmationPrompt.isSupported(getContext())) {
                Snackbar.make(view1, R.string.apc_not_supported, Snackbar.LENGTH_LONG)
                        .setAction(R.string.sorry, null).show();
                binding.buttonConfirm.setEnabled(false);
                Log.w(TAG, "Confirmation Prompt is not supported on this device");
                return;
            }
            // Check if we have keys
            if (!HpcUtility.hasKey()){
                NavHostFragment.findNavController(getParentFragment())
                        .navigate(R.id.action_RegisterFragment);
                return;
            }
            // Check is anything is selected
            String inputAddress=null;
            inputAddress = checkInput();

            if (inputAddress == null){
                Snackbar.make(view1, R.string.incomplete_input, Snackbar.LENGTH_LONG)
                        .setAction("No Selection", null).show();
            } else {
                confirm(inputAddress, view1);
            }
        });
    }


    private String checkInput(){
        boolean error = false;
        StringBuilder sb = new StringBuilder();
        if (binding.givenName.getText().length() > 0){
            sb.append(binding.givenName.getText());
        } else {
            binding.givenName.setError(getResources().getString(R.string.please_provide_given_name));
            error = true;
        }
        if (binding.inputSurname.getText().length() > 0){
            sb.append(" ");
            sb.append(binding.inputSurname.getText());
        } else {
            binding.inputSurname.setError(getResources().getString(R.string.please_provide_surname));
            error = true;
        }
        if (binding.inputEmail.getText().length() > 0){
            if (android.util.Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText()).matches()) {
                sb.append(", ");
                sb.append(binding.inputEmail.getText());
            } else {
                binding.inputEmail.setError(getResources().getString(R.string.please_provide_email));
                error = true;
            }
        } else {
            binding.inputEmail.setError(getResources().getString(R.string.email_please));
            error = true;
        }
        if (binding.inputCompanyName.getText().length() > 0){
            sb.append(", ");
            sb.append(binding.inputCompanyName.getText());
        } else {
            binding.inputCompanyName.setError(getResources().getString(R.string.please_provide_company_name));
            error = true;
        }
        if (error){
            return null;
        }
        return sb.toString();
    }


    public void confirm(String promptText, View view) {
        Log.i(TAG, String.format("Prompt text is %d char long.", promptText.length()));
        String nonce = UUID.randomUUID().toString();
        //byte[] extraData = (promptText + ":" + nonce).getBytes();
        byte[] extraData = (nonce).getBytes();

        // Create confirmation prompt
        // Language must be English ... APC only works with en
        Locale.setDefault(Locale.US);
        Log.i(TAG, "Default language "+ Locale.getDefault().toLanguageTag());
        ConfirmationPrompt confirmationPrompt = new ConfirmationPrompt.Builder(getContext())
                .setPromptText(promptText)
                .setExtraData(extraData)
                .build();

        try {
            confirmationPrompt.presentPrompt(getActivity().getMainExecutor(), new ConfirmationCallback() {
                @Override
                public void onConfirmed(byte[] dataThatWasConfirmed) {
                    super.onConfirmed(dataThatWasConfirmed);
                    // the signed data is a RFC 8949 Concise Binary Object Representation (CBOR) blob.
                    CBORMapper cborMapper = new CBORMapper();
                    try {
                        Map mdata = cborMapper.readValue(dataThatWasConfirmed, Map.class);
                        Log.i(TAG, mdata.toString());
                    }catch (Exception ex){
                        Log.e(TAG, String.format("Failed parsing confirmed data from CBOR: %s", ex));
                    }
                    Log.i(TAG, String.format("Confirmed Data: %s",new String(dataThatWasConfirmed)));
                    try {
                        Signature signature = initSignature(Constants.KEY_NAME);
                        if (requiresAuthentication()){

                            BiometricPrompt.AuthenticationCallback authenticationCallback = new BiometricPrompt.AuthenticationCallback() {
                                    @Override
                                    public void onAuthenticationError(int errorCode,
                                                                      CharSequence errString) {
                                        notifyUser(getResources().getString(R.string.authentication_error)+ errString);
                                        super.onAuthenticationError(errorCode, errString);
                                    }

                                    @Override
                                    public void onAuthenticationFailed() {
                                        super.onAuthenticationFailed();
                                    }

                                    @Override
                                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                                        Log.i(TAG, "onAuthenticationSucceeded");
                                        super.onAuthenticationSucceeded(result);
                                        if (result.getCryptoObject() != null &&
                                                result.getCryptoObject().getSignature() != null) {
                                            try {
                                                Signature signature = result.getCryptoObject().getSignature();
                                                signature.update(dataThatWasConfirmed);
                                                byte[] signatureBytes = signature.sign();
                                                processSignature(view, dataThatWasConfirmed, signatureBytes);
                                            } catch (SignatureException e) {
                                                //throw new RuntimeException();
                                                Log.e(TAG, String.format("Signature exception: %s", e));
                                                Snackbar.make(view, getResources().getString(R.string.signing_failed) + e, Snackbar.LENGTH_LONG)
                                                        .setAction(R.string.error, null).show();
                                            }
                                        } else {
                                            // Error
                                            Log.e(TAG, "Failed to get result.");
                                        }
                                    }
                                };
                            BiometricPrompt mBiometricPrompt = new BiometricPrompt( requireActivity(), requireActivity().getApplicationContext().getMainExecutor(), authenticationCallback);

                            // Set prompt info
                            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                                    .setDescription(getResources().getString(R.string.protected_confirmation))
                                    .setTitle(getResources().getString(R.string.protected_confirmation))
                                    .setSubtitle(getResources().getString(R.string.with_fingerprint))
                                    .setNegativeButtonText(getResources().getString(R.string.cancel))
                                    .build();
                            mBiometricPrompt.authenticate(promptInfo, new BiometricPrompt.CryptoObject(signature));
                        } else {
                            assert signature != null;
                            signature.update(dataThatWasConfirmed);
                            byte[] signatureBytes = signature.sign();
                            processSignature(view, dataThatWasConfirmed, signatureBytes);
                        }
                    } catch (Exception e) {
                        Snackbar.make(view, getResources().getString(R.string.signing_failed) + e, Snackbar.LENGTH_LONG)
                                .setAction(getResources().getString(R.string.error), null).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDismissed() {
                    super.onDismissed();
                    Snackbar.make(view, R.string.confirmation_dismissed, Snackbar.LENGTH_LONG)
                            .setAction(R.string.dismissed, null).show();
                }

                @Override
                public void onCanceled() {
                    super.onCanceled();
                    Snackbar.make(view, R.string.confirmation_cancelled, Snackbar.LENGTH_LONG)
                            .setAction(R.string.cancelled, null).show();
                }

                @Override
                public void onError(Throwable e) {
                    super.onError(e);
                    Log.e(TAG,"Error on Confirmation: "+e.toString());
                    Snackbar.make(view, getResources().getString(R.string.authentication_error) + e, Snackbar.LENGTH_LONG)
                            .setAction(R.string.error, null).show();
                }
            });
        } catch (ConfirmationAlreadyPresentingException e) {
            Log.e(TAG, "Protected confirmation already present: " + e);
            notifyUser(getResources().getString(R.string.confirmation_already_present));
        } catch (ConfirmationNotAvailableException e) {
            Log.w(TAG, "Protected Confirmation is not supported on this device");
            notifyUser(getResources().getString(R.string.protected_confirmation_not_supported));
        } catch (Exception ex){
            notifyUser(getResources().getString(R.string.cannot_confirm_message));
            Log.e(TAG, "Unexpected error on presenting the confirmation.");
        }
    }

    private KeyPair getKeyPair(String keyName) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(keyName)) {
            // Get public key
            PublicKey publicKey = keyStore.getCertificate(keyName).getPublicKey();
            // Get private key
            PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyName, null);
            // Return a key pair
            return new KeyPair(publicKey, privateKey);
        }
        return null;
    }

    private void processSignature(View view, byte[] dataThatWasConfirmed, byte[] signatureBytes){
        byte[] p7m = PKCS7Builder.generatePkcs7(dataThatWasConfirmed, signatureBytes, getSignatureAlgorithm());
        String pkcs7msg = PKCS7Builder.generatePkcs7Pem(p7m);
        sharedData.setP7mPem(pkcs7msg);
        if (sharedData.isConnected()) {
            try {
                String post = String.format("%s=%s", "p7m", URLEncoder.encode(pkcs7msg, "UTF-8"));
                NetworkPostMessage net = new NetworkPostMessage(Constants.P7M_URL, post.getBytes(), 2000);
                Executors.newSingleThreadExecutor().execute(net);
            } catch (Exception e) {
                Log.e(TAG, "Failed to send p7m: " + e);
            }
        }
        else {
            copyToClipboard("PKCS7", pkcs7msg);
        }

        //Log.i(TAG, "dataThatWasConfirmed: " + pkcs7msg);
        //Log.i(TAG, "signature: " + pkcs7msg);
        Snackbar sb = Snackbar.make(view, R.string.successfully_signed
                + pkcs7msg, Snackbar.LENGTH_LONG)
                .setAction(R.string.success, null);
        if (sharedData.isConnected()) {
            sb.setAction(R.string.show_p7m, v -> {
                //show signature
                NavHostFragment.findNavController(JoinAllianceFragment.this).navigate(R.id.action_BrowserFragment);
            });
        }
        sb.show();
    }

    private Signature initSignature(String keyName) throws Exception {
        KeyPair keyPair = getKeyPair(keyName);
        if (keyPair != null) {
            Signature signature = Signature.getInstance(getSignatureAlgorithm());
            signature.initSign(keyPair.getPrivate());
            return signature;
        }
        return null;
    }

    private void notifyUser(String message) {
        Toast.makeText(getContext(),
                message,
                Toast.LENGTH_LONG).show();
    }

    private void copyToClipboard(String label, String msg){
        ClipboardManager clipboard = (ClipboardManager) requireActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, msg);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * Verify a signature
     * @param data data to be signed
     * @param signature signature bytes
     * @return true or false
     */
    public boolean verifySignature (byte[] data, byte[] signature) {
        boolean result = false;
        try {
            Signature s = Signature.getInstance(getSignatureAlgorithm());
            s.initVerify(getCert().getPublicKey());
            s.update(data);
            result = s.verify(signature);
            if (result) {
                Log.i(TAG, "Signature successfully verified.");
            }
            else {
                Log.i(TAG, "Verification failed.");
            }
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Failed verifying with no such algorithm: " + e);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "Failed verifying with invalid key exception: " + e);
        } catch (Exception e) {
            Log.e(TAG, "Failed verifying: "+ e);
        }
        return result;
    }
}
