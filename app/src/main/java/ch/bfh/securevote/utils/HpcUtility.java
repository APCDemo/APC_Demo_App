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

package ch.bfh.securevote.utils;

import static com.google.android.attestation.ParsedAttestationRecord.createParsedAttestationRecord;

import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.StrongBoxUnavailableException;
import android.util.Base64;
import android.util.Log;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;


import javax.security.auth.x500.X500Principal;
import com.google.android.attestation.AuthorizationList;
import com.google.android.attestation.ParsedAttestationRecord;

public class HpcUtility {

    private static final String TAG = HpcUtility.class.getName();
    private static HpcUtility instance;
    protected SharedPreferences prefs;

    private HpcUtility(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static HpcUtility getInstance(SharedPreferences prefs) {
        if (instance == null) {
            instance = new HpcUtility(prefs);
        }
        return instance;
    }

    public String register(byte[] challenge) {
        String info = generateKeyPair(Constants.KEY_NAME, challenge);
        try {
            Certificate[] certificateChain = getAttestationCertificateChain(Constants.KEY_NAME);
            // First entry of certificate chain is for is for the id
            // This certificate chain should be sent to RP, and then verified.
            // If verification is succeeded, RP should maintain Kpub.
            for (int i = 0; i < certificateChain.length; i++) {
                Log.i(TAG, "Cert " + i + ": " + certificateChain[i].toString());
                Log.i(TAG, "PubKey " + i + ": " + Base64.encodeToString(certificateChain[i].getPublicKey().getEncoded(), Base64.URL_SAFE));
            }
        } catch (Exception ex) {
            Log.e(TAG, "Certificates are not available: " + ex);
        }
        return info;
    }


    /**
     * Generate APC key pair for signing and verification. The key parameter are taken from the settings.
     *
     * @param keyName the name key alias under which the key will be stored
     * @param challenge This challenge will be included in the attestation certificate
     * @return result as a string
     */
    protected String generateKeyPair(String keyName, byte[] challenge) {


        StringBuilder genlog = new StringBuilder();

        // Start date
        int years = Integer.parseInt(prefs.getString("cert_validity", "1"));
        Date startDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        if (years > 0) {
            c.add(Calendar.YEAR, years);
        }else{
            c.add(Calendar.HOUR, 24);
        }
        Date endDate = c.getTime();

        //Key Name
        String keyAliasName = prefs.getString( Constants.settings_key_name, Constants.defaultKeyAliasName);


        boolean tryGeneration = true;

        // Read preferred key settings
        boolean unlockDeviceRequired = prefs.getBoolean(Constants.settings_unlock_device_required, Constants.settings_unlock_device_required_default);
        boolean userConfirmationRequired = prefs.getBoolean(Constants.settings_user_confirmation_required, Constants.settings_user_confirmation_required_default);
        boolean strongBoxRequired = prefs.getBoolean(Constants.settings_strong_box_required, Constants.settings_strong_box_required_default);
        boolean userAuthenticationRequired = prefs.getBoolean(Constants.settings_user_authentication_required, Constants.settings_user_authentication_required_default);
        String keyType = prefs.getString(Constants.settings_key_type, Constants.settings_key_type_default);
        String ecCurve = prefs.getString(Constants.settings_ec_curve, Constants.settings_ec_curve_default);
        String rsaKeylength = prefs.getString(Constants.settings_rsa_key_length, Constants.settings_rsa_key_length_default);
        int keyLength=2048;
        try {
            keyLength=Integer.parseInt(rsaKeylength);
        }catch (Exception ex){
            Log.e(TAG, "Reading key length failed: "+ ex);
        }

        while (tryGeneration) {
            X500Principal cname = new X500Principal(String.format("CN=%s, OU=SecureVote, OU=BFH, C=CH", keyAliasName));
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_SIGN)
                    .setCertificateSubject(cname)
                    .setCertificateNotBefore(startDate)
                    .setCertificateNotAfter(endDate)
                    .setKeyValidityStart(startDate)
                    .setKeyValidityEnd(endDate)
                    .setIsStrongBoxBacked(strongBoxRequired)  // generate on HSM
                    //.setAttestKeyAlias(prefs.getString("attestation_key_alias","SecureVote"))
                    .setDigests(KeyProperties.DIGEST_SHA256,
                            KeyProperties.DIGEST_SHA384,
                            KeyProperties.DIGEST_SHA512)
                    .setUnlockedDeviceRequired(unlockDeviceRequired)
                    .setUserConfirmationRequired(userConfirmationRequired)
                    .setUserAuthenticationRequired(userAuthenticationRequired)
                    //.setUserPresenceRequired(userPresenceRequired)  // conflicting ... do not use
                    //.setDevicePropertiesAttestationIncluded(true)   // requires API 31 (Android 12) Note: Prevents key generation if device is unlocked
                    //.setMaxUsageCount(max_usage)  //could be set to limit the number of key usages
                    .setAttestationChallenge(challenge);
            try {
                KeyPairGenerator keyPairGenerator;
                if (keyType.contains("EC")){
                    keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore");
                    builder.setAlgorithmParameterSpec(new ECGenParameterSpec(ecCurve));
                } else {
                    keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
                    builder.setAlgorithmParameterSpec(new RSAKeyGenParameterSpec(keyLength, RSAKeyGenParameterSpec.F4))
                            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1);
                }
                // Generate the APC key pair
                keyPairGenerator.initialize(builder.build());
                KeyPair kp = keyPairGenerator.generateKeyPair();
                genlog.append("Congratulation! The key pair was successfully generated.");
                return genlog.toString();
            } catch (StrongBoxUnavailableException ex_sb) {
                if (strongBoxRequired) {
                    strongBoxRequired = false;
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.putBoolean("strong_box_required", false).apply();
                    edit.apply();
                    genlog.append("Your device has no Secure Element. I had to set 'StrongBox required' to false.\n");
                } else {
                    genlog.append("Failed to generate key pair: ").append(ex_sb);
                    tryGeneration = false;
                }
            } catch (NoSuchAlgorithmException ex_no_such_alg) {
                genlog.append("Failed to generate key pair: ").append(ex_no_such_alg);
                tryGeneration = false;
            } catch (InvalidAlgorithmParameterException ex_inv_alg) {
                genlog.append("Failed to generate key pair: ").append(ex_inv_alg);
                tryGeneration = false;
            } catch (ProviderException ex_pro) {
                genlog.append("Provider Exception. Failed to generate key pair: ").append(ex_pro);
                tryGeneration = false;
            } catch (Exception ex) {
                ex.printStackTrace();
                genlog.append("Failed to generate key pair: ").append(ex);
                tryGeneration = false;
            }
        }
        return genlog.toString();
    }

    private static Certificate[] getAttestationCertificateChain(String alias) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
        keyStore.load(null);
        return keyStore.getCertificateChain(alias);
    }

    public static boolean deleteKey(String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
            keyStore.load(null);
            keyStore.deleteEntry(alias);
            return true;
        } catch (KeyStoreException | CertificateException | IOException
                | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static KeyPair getKeyPair(String keyName) throws Exception {
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

    public static Certificate getCert() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(Constants.KEY_NAME)) {
                // Get public key
                return keyStore.getCertificate(Constants.KEY_NAME);
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to get cert for %s: %s", Constants.KEY_NAME, e));
        }
        return null;
    }

    public static Certificate getCert(String keyName) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(keyName)) {
                // Get public key
                return keyStore.getCertificate(keyName);
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to get cert for %s: %s", keyName, e));
        }
        return null;
    }

    public static KeyStore.Entry getEntry(String keyName) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(keyName)) {
                // Get public key
                return keyStore.getEntry(keyName, null);
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to get entry for %s: %s", keyName, e));
        }
        return null;
    }

    public static String getKeyType(){
        Certificate cert = getCert();
        assert cert != null;
        return cert.getPublicKey().getAlgorithm();
    }

    public static String getKeyType(String alias){
        Certificate cert = getCert(alias);
        assert cert != null;
        return cert.getPublicKey().getAlgorithm();
    }

    public static String getSignatureAlgorithm(){
        if (getKeyType().contains("RSA")) {
            return "SHA256withRSA";
        }
        return "SHA256withECDSA";
    }

    public static Certificate[] getCertChain(String keyName) {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);
            if (keyStore.containsAlias(keyName)) {
                // Get public key
                return keyStore.getCertificateChain(keyName);
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to get cert chain for %s: %s", keyName, e));
        }
        return null;
    }

    public static Enumeration<String> getKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(Constants.KEY_STORE_TYPE);
            keyStore.load(null);
            return keyStore.aliases();

        } catch (Exception e) {
            Log.e(TAG, String.format("Failed to get KeyStore objects %s: %s", Constants.KEY_STORE_TYPE, e));
        }
        return null;
    }

    public static boolean hasKey(){
        int count=0;
        Enumeration<String> aliases = getKeyStore();
        if (aliases!=null){
            while(aliases.hasMoreElements()) {
                count++;
                String alias = aliases.nextElement();
                Log.i(TAG, alias);
            }
        }
        return (count > 0);
    }

    public static boolean requiresAuthentication(){
        Certificate cert = getCert(Constants.KEY_NAME);
        ParsedAttestationRecord parsedAttestationRecord=null;
        try {
            parsedAttestationRecord = createParsedAttestationRecord((X509Certificate) cert);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (parsedAttestationRecord!=null) {
            AuthorizationList teeEnforced = parsedAttestationRecord.teeEnforced;
            return !teeEnforced.noAuthRequired;
        }
        return false;
    }

    public static String getDeviceInformation(){
        StringBuilder deviceInfo = new StringBuilder();

        deviceInfo.append("VERSION.RELEASE {" + Build.VERSION.RELEASE + "}");
        deviceInfo.append("\nVERSION.INCREMENTAL {" + Build.VERSION.INCREMENTAL + "}");
        deviceInfo.append("\nVERSION.SDK {" + Build.VERSION.SDK + "}");
        deviceInfo.append("\nBOARD {" + Build.BOARD + "}");
        deviceInfo.append("\nBRAND {" + Build.BRAND + "}");
        deviceInfo.append("\nDEVICE {" + Build.DEVICE + "}");
        deviceInfo.append("\nFINGERPRINT {" + Build.FINGERPRINT + "}");
        deviceInfo.append("\nHOST {" + Build.HOST + "}");
        deviceInfo.append("\nID {" + Build.ID + "}");
        deviceInfo.append("\nDISPLAY {" + Build.DISPLAY + "}");

        return deviceInfo.toString();
    }

}