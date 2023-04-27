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

import static java.nio.charset.StandardCharsets.UTF_8;
import static com.google.android.attestation.ParsedAttestationRecord.createParsedAttestationRecord;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.util.encoders.Base64;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Locale;
import java.util.Optional;

import ch.bfh.securevote.databinding.FragmentCheckKeyBinding;
import ch.bfh.securevote.utils.CertificateParser;
import ch.bfh.securevote.utils.Constants;
import ch.bfh.securevote.utils.HpcUtility;
import com.google.android.attestation.AttestationApplicationId;
import com.google.android.attestation.AuthorizationList;
import com.google.android.attestation.ParsedAttestationRecord;
import com.google.android.attestation.RootOfTrust;

public class CheckKeyFragment extends Fragment {

    private FragmentCheckKeyBinding binding;
    private static final String TAG = CheckKeyFragment.class.getName();
    private TableLayout tableLayout;
    protected StringBuffer log;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentCheckKeyBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            //Log.i(TAG, getCertPem());
            Certificate[] certChain = HpcUtility.getCertChain(Constants.KEY_NAME);
            binding.sectionLabel.append(getResources().getString(R.string.certificate_details));
            // Populate fields
            populate(Constants.KEY_NAME, certChain);
        } catch (Exception ex) {
            Log.e(TAG, "Error getting certificate chain: " + ex);
            binding.sectionLabel.append(getResources().getString(R.string.no_key_found));
        }
        binding.buttonCopy.setOnClickListener(view1 -> {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText(Constants.KEY_NAME, log.toString() + "\n" + getCertPem());
            clipboard.setPrimaryClip(clip);
            Snackbar.make(view1, R.string.copied, Snackbar.LENGTH_LONG)
                    .setAction(R.string.copy, null).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void addCaption(String label, int indent) {
        log.append(String.format("%s*%s \n", getIndent(indent), label));
        final TableRow trl = new TableRow(getActivity());
        final TextView tvLabel = new TextView(getActivity());
        tvLabel.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tvLabel.setPadding(indent, 20, 0, 0);
        tvLabel.setTypeface(null, Typeface.BOLD);
        tvLabel.setText(label);
        trl.addView(tvLabel, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.addView(trl, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));
    }

    public String getIndent(int indent){
        if (indent < 10){
            return "";
        }else if (indent < 20){
            return "  ";
        }else if (indent < 30){
            return "    ";
        }
        return "      ";
    }

    public void addLabelLine(String label, int indent, boolean critical) {
        log.append(String.format("%s- %s: ", getIndent(indent), label));
        final TableRow trl = new TableRow(getActivity());
        final TextView tvLabel = new TextView(getActivity());
        tvLabel.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        tvLabel.setPadding(10 + indent, 10, 0, 0);
        if (critical) {
            tvLabel.setBackgroundColor(getContext().getColor(R.color.medium_red));
            tvLabel.setTextColor(getContext().getColor(R.color.bfh_orange));
        }
        tvLabel.setText(label);
        trl.addView(tvLabel, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));
        tableLayout.addView(trl, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT));
    }

    public void addValueLine(String value, int indent, boolean critical) {
        log.append(String.format("%s\n", value));
        final TableRow trv = new TableRow(getActivity());
        final TextView tvValue = new TextView(getActivity());
        tvValue.setPadding(20 + indent, 0, 20, 0);
        tvValue.setTextColor(getContext().getColor(R.color.bfh_orange));
        if (critical) tvValue.setBackgroundColor(getContext().getColor(R.color.medium_red));
        tvValue.setLayoutParams(new TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, TableLayout.LayoutParams.WRAP_CONTENT, 1f));
        trv.addView(tvValue, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT, 1f));
        tvValue.setText(value);
        tableLayout.addView(trv, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT, 1f));
    }

    public void addKeyVal(String key, String val, int indent, boolean critical) {
        addLabelLine(key, indent, critical);
        if (val.length() > 0) addValueLine(val, indent, critical);
    }

    public void parseAttestationExtension(X509Certificate cert, int indent) {
        try {
            ParsedAttestationRecord parsedAttestationRecord = createParsedAttestationRecord(cert);

            addKeyVal(getResources().getString(R.string.attestation_version), Integer.toString(parsedAttestationRecord.attestationVersion), indent, false);
            addKeyVal(
                    getResources().getString(R.string.attestation_security_level), parsedAttestationRecord.attestationSecurityLevel.name(), indent, true);
            addKeyVal(getResources().getString(R.string.keymaster_version), Integer.toString(parsedAttestationRecord.keymasterVersion), indent, false);
            addKeyVal(getResources().getString(R.string.keymaster_security_level), parsedAttestationRecord.keymasterSecurityLevel.name(), indent, true);

            addKeyVal(
                    getResources().getString(R.string.attestation_challenge), new String(parsedAttestationRecord.attestationChallenge, UTF_8), indent, true);
            if (parsedAttestationRecord.uniqueId.length > 0) addKeyVal("Unique ID", new String(parsedAttestationRecord.uniqueId, UTF_8), indent, false);

            addCaption(getResources().getString(R.string.software_enforced_authorization_list), indent);
            AuthorizationList softwareEnforced = parsedAttestationRecord.softwareEnforced;
            printAuthorizationList(softwareEnforced, indent + 20);

            addCaption(getResources().getString(R.string.tee_enforced_authorization_list), indent);
            AuthorizationList teeEnforced = parsedAttestationRecord.teeEnforced;
            printAuthorizationList(teeEnforced, indent + 20);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void printAuthorizationList(AuthorizationList authorizationList, int indent) {
        // Detailed explanation of the keys and their values can be found here:
        // https://source.android.com/security/keystore/tags
        printOptional(authorizationList.purpose, getResources().getString(R.string.purpose), indent, true);
        printOptional(authorizationList.algorithm, getResources().getString(R.string.algorithm), indent, false);
        printOptional(authorizationList.keySize, getResources().getString(R.string.key_size_cap), indent, false);
        printOptional(authorizationList.digest, getResources().getString(R.string.diget), indent, false);
        printOptional(authorizationList.padding, getResources().getString(R.string.padding), indent, false);
        printOptional(authorizationList.ecCurve, getResources().getString(R.string.ec_curve), indent, false);
        if (authorizationList.rollbackResistance) addKeyVal(getResources().getString(R.string.rollback_resistance), "", indent, false);
        printOptional(authorizationList.activeDateTime, getResources().getString(R.string.active_date_time), indent, false);
        printOptional(
                authorizationList.originationExpireDateTime, getResources().getString(R.string.origination_expire_date_time), indent, false);
        printOptional(authorizationList.usageExpireDateTime, getResources().getString(R.string.usage_expire_date_time), indent, true);
        if (authorizationList.noAuthRequired) addKeyVal(getResources().getString(R.string.no_auth_required), "", indent, false);
        printOptional(authorizationList.userAuthType, getResources().getString(R.string.user_auth_type), indent, false);
        printOptional(authorizationList.authTimeout, getResources().getString(R.string.auth_timeout), indent, false);
        if (authorizationList.allowWhileOnBody) addKeyVal(getResources().getString(R.string.allow_while_on_body), "", indent, false);
        if (authorizationList.trustedUserPresenceRequired) addKeyVal(getResources().getString(R.string.trusted_user_presence_required), "", indent, false);
        if (authorizationList.trustedConfirmationRequired) addKeyVal(getResources().getString(R.string.trusted_confirmation_required), "", indent, true);
        if (authorizationList.unlockedDeviceRequired) addKeyVal(getResources().getString(R.string.unlocked_device_required), "", indent, true);
        if (authorizationList.allApplications) addKeyVal(getResources().getString(R.string.all_applications), "", indent, false);
        printOptional(authorizationList.applicationId, getResources().getString(R.string.application_id), indent, false);
        printOptional(authorizationList.creationDateTime, getResources().getString(R.string.creatin_date_time), indent, false);
        printOptional(authorizationList.origin, getResources().getString(R.string.origin), indent, true);
        if (authorizationList.rollbackResistant) addKeyVal(getResources().getString(R.string.rollback_resistant), "", indent, false);
        if (authorizationList.rootOfTrust.isPresent()) {
            addCaption(getResources().getString(R.string.root_of_trust), indent);
            printRootOfTrust(authorizationList.rootOfTrust, indent);
        }
        printOptional(authorizationList.osVersion, getResources().getString(R.string.os_version), indent, false);
        printOptional(authorizationList.osPatchLevel, getResources().getString(R.string.os_patch_level), indent, false);
        if (authorizationList.attestationApplicationId.isPresent()) {
            addCaption(getResources().getString(R.string.attestation_application_id), indent);
            printAttestationApplicationId(authorizationList.attestationApplicationId, indent);
        }
        printOptional(
                authorizationList.attestationApplicationIdBytes,
                getResources().getString(R.string.attestation_application_id_bytes), indent, false);
        printOptional(authorizationList.attestationIdBrand, getResources().getString(R.string.attestation_id_brand), indent, false);
        printOptional(authorizationList.attestationIdDevice, getResources().getString(R.string.attestation_id_device), indent, false);
        printOptional(authorizationList.attestationIdProduct, getResources().getString(R.string.attestation_id_product), indent, false);
        printOptional(authorizationList.attestationIdSerial, getResources().getString(R.string.attestation_id_serial), indent, false);
        printOptional(authorizationList.attestationIdImei, getResources().getString(R.string.attestation_id_imei), indent, false);
        printOptional(authorizationList.attestationIdMeid, getResources().getString(R.string.attestation_id_meid), indent, false);
        printOptional(
                authorizationList.attestationIdManufacturer, getResources().getString(R.string.attestation_id_manufacturer), indent, false);
        printOptional(authorizationList.attestationIdModel, getResources().getString(R.string.attestation_id_model), indent, false);
        printOptional(authorizationList.vendorPatchLevel, getResources().getString(R.string.vendor_patch_level), indent, false);
        printOptional(authorizationList.bootPatchLevel, getResources().getString(R.string.boot_patch_level), indent, false);
    }

    private void printRootOfTrust(Optional<RootOfTrust> rootOfTrust, int indent) {
        if (rootOfTrust.isPresent()) {
            addKeyVal(
                    getResources().getString(R.string.verified_boot_key)
                    , Base64.toBase64String(rootOfTrust.get().verifiedBootKey), indent, false);
            addKeyVal(getResources().getString(R.string.device_locked), Boolean.toString(rootOfTrust.get().deviceLocked), indent, true);
            addKeyVal(
                    getResources().getString(R.string.verified_boot_state), rootOfTrust.get().verifiedBootState.name(), indent, true);
            addKeyVal(
                    getResources().getString(R.string.verified_boot_hash), Base64.toBase64String(rootOfTrust.get().verifiedBootHash), indent, false);
        }
    }

    private void printAttestationApplicationId(
            Optional<AttestationApplicationId> attestationApplicationId, int indent) {
        if (attestationApplicationId.isPresent()) {
            addLabelLine(getResources().getString(R.string.package_infos), indent, true);
            for (AttestationApplicationId.AttestationPackageInfo info : attestationApplicationId.get().packageInfos) {
                addValueLine(info.packageName + ", " + info.version, indent, true);
            }
            addLabelLine(getResources().getString(R.string.signature_digests), indent, true);
            for (byte[] digest : attestationApplicationId.get().signatureDigests) {
                addValueLine(Base64.toBase64String(digest), indent, true);
            }
        }
    }

    private <T> void printOptional(Optional<T> optional, String caption, int indent, boolean critical) {
        if (optional.isPresent()) {
            addLabelLine(caption, indent, critical);
            if (optional.get() instanceof byte[]) {
                if (isPrintable((byte[])optional.get())){
                    addValueLine(new String((byte[])optional.get()), indent, critical);
                }else {
                    addValueLine(Base64.toBase64String((byte[]) optional.get()), indent, critical);
                }
            } else {
                String val = String.format("%s", optional.get());
                addValueLine(val, indent, critical);
            }
        }else{
            log.append(String.format("%s- %s: UNDEFINED\n", getIndent(indent), caption));
        }
    }

    /**
     * Checks if the byte array is printable
     * @param data
     * @return
     */
    public static boolean isPrintable(byte[] data){
        for (byte b : data)
            if ( b < 32 ) return false;
        return true;
    }

    /**
     * Writes the certificate information to the UI and generates a log file
     */
    public void populate(String certName, Certificate[] certChain) {
        log = new StringBuffer();

        X509Certificate certificate = (X509Certificate) certChain[0];

        // UI field linking
        tableLayout = binding.tablelayoutCert;
        tableLayout.setStretchAllColumns(true);
        TextView textViewAlias = binding.alias;

        // Variables for calculating RSA key info
        RSAPublicKey rsaPublicKey;
        BigInteger pubModulus;
        BigInteger exponent;

        textViewAlias.setText(certName);
        int indent = 20; //default
        addKeyVal(getResources().getString(R.string.x509_version), String.valueOf(certificate.getVersion()), indent, false);
        addKeyVal(getResources().getString(R.string.serial_number), certificate.getSerialNumber().toString(16).replaceAll("(?<=..)(..)", ":$1").toUpperCase(Locale.US), indent, false);

        // Display the distinguished names
        try {
            addKeyVal(getResources().getString(R.string.subject),String.valueOf(PrincipalUtil.getSubjectX509Principal(certificate)), indent, false);
            addKeyVal(getResources().getString(R.string.issuer_subject),String.valueOf(PrincipalUtil.getIssuerX509Principal(certificate)), indent, false);
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "Certificate Encoding Exception" + e);
        }
        addCaption(getResources().getString(R.string.validity), indent);
        addKeyVal(getResources().getString(R.string.issued_on),certificate.getNotBefore().toString(), indent+10, true);
        addKeyVal(getResources().getString(R.string.expires_on),certificate.getNotAfter().toString(), indent+10, true);

        addCaption(getResources().getString(R.string.key_specifics), indent);
        byte[] signature = certificate.getSignature();
        String algorithm="";

        // These calculations differ if key is RSA/EC
        if (certificate.getPublicKey().getAlgorithm().matches("RSA")) {
            // cast the public key
            algorithm="RSA";
            rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
            pubModulus = rsaPublicKey.getModulus();
            exponent = rsaPublicKey.getPublicExponent();
            int pubKeySize = rsaPublicKey.getModulus().bitLength();

            // Because the modulus is usually large, we're only displaying a subset of the string - the first 30 characters or total length, which ever is less.
            String pubMod = pubModulus.toString().substring(0, Math.min(pubModulus.toString().length(), 30)) + "...";
            addKeyVal(getResources().getString(R.string.public_modulus), pubMod, indent+10, false);
            String exponentStr = String.format(Locale.US, "%d", exponent);
            addKeyVal(getResources().getString(R.string.public_exponent), exponentStr, indent+10, false);
            addKeyVal(getResources().getString(R.string.key_alg),certificate.getPublicKey().getAlgorithm(),indent+10, false);
            addKeyVal(getResources().getString(R.string.key_size),String.valueOf(pubKeySize),indent+10, false);
        } else if (certificate.getPublicKey().getAlgorithm().matches(getString(R.string.EC))) {
            addKeyVal(getResources().getString(R.string.key_alg),getResources().getString(R.string.elliptic_curve),indent+10, false);
            algorithm = CertificateParser.getEcType(certificate);
        }
        addKeyVal(getResources().getString(R.string.key_usage), CertificateParser.getKeyUsageString(certificate),indent+10, false);
        addKeyVal(getResources().getString(R.string.signature_algorithm), algorithm, indent, false);
        addKeyVal(getResources().getString(R.string.signature), new BigInteger(signature).toString(16), indent, true);

        // Calculate the key fingerprints
        try {
            addKeyVal("SHA-256 fingerprint", getThumbPrint(certificate, "SHA-256"), indent, false);
            addKeyVal("SHA-1 fingerprint", getThumbPrint(certificate, "SHA-1"), indent, false);
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            addKeyVal("SHA-256 fingerprint", getResources().getString(R.string.fingerprint_calc_error), indent, false);
        }
        addCaption(getResources().getString(R.string.attestation_extension), indent);
        parseAttestationExtension(certificate, indent);
    }

    /**
     * Returns a string value of the certificate thumbprint in hex format.
     *
     * @param cert Certificate to calculate thumbprint of.
     * @return String value of certificate thumbprint in hex.
     * @throws NoSuchAlgorithmException when hashing algorithm is not found
     * @throws CertificateEncodingException when DER encoding fails
     */
    public static String getThumbPrint(X509Certificate cert, String algorithm)
            throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] der = cert.getEncoded();
        md.update(der);
        byte[] digest = md.digest();
        String hex = hexify(digest);

        // Add colons to the hex string
        return hex.replaceAll("(?<=..)(..)", ":$1").toUpperCase(Locale.US);
    }

    /**
     * Converts the input byte array to hex format and returns
     * the string value of the new format.
     *
     * @param bytes Byte array to convert to hex.
     * @return String of hex conversion.
     */
    public static String hexify(byte[] bytes) {
        char[] hexDigits = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for (byte aByte : bytes) {
            buf.append(hexDigits[(aByte & 0xf0) >> 4]);
            buf.append(hexDigits[aByte & 0x0f]);
        }
        return buf.toString();
    }

    /**
     * Returns the PEM encoded APC certificate.
     */
    public static String getCertPem(){
        StringBuilder sb = new StringBuilder();
        try{
            sb.append("-----BEGIN CERTIFICATE-----\n");
            sb.append(android.util.Base64.encodeToString(HpcUtility.getCert().getEncoded(), android.util.Base64.DEFAULT));
            sb.append("-----END CERTIFICATE----- \n");
            return sb.toString();
        }catch (Exception ex){
            Log.e(TAG, String.format("Failed to get PEM encoded certificate: %s", ex));
        }
        return "-----FAILED TO GET CERTIFICATE-----";
    }
}
