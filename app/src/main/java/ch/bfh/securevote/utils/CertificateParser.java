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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.util.ArrayList;

import com.google.android.attestation.AuthorizationList;
import com.google.android.attestation.ParsedAttestationRecord;

public class CertificateParser {

    public static String getKeyType(Certificate certificate){
        return certificate.getPublicKey().getAlgorithm();
    }

    public static String getEcType(Certificate certificate){
        String algorithm = getKeyType(certificate);
        if (!algorithm.contains("EC")){
            return "";
        }
        ECPublicKey ecPublicKey = (ECPublicKey) certificate.getPublicKey();
        // Unfortunately the method getCurveName() is not always implemented ...
        // Therefore we call it with reflection ...
        try {
            ECParameterSpec spec = ecPublicKey.getParams();
            Method methodToFind = spec.getClass().getMethod("getCurveName", (Class<?>[]) null);
            if (methodToFind != null) algorithm = (String) methodToFind.invoke(spec, (Object[]) null);
        } catch (IllegalAccessException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
            // ignore
        }
        return algorithm;
    }

    public static String getRsaKeyLength(Certificate certificate){
        String algorithm = getKeyType(certificate);
        if (!algorithm.contains("RSA")){
            return "";
        }
        RSAPublicKey rsaPublicKey = (RSAPublicKey) certificate.getPublicKey();
        int pubKeySize = rsaPublicKey.getModulus().bitLength();
        return String.format("%d bits", pubKeySize);
    }

    public static ParsedAttestationRecord getAttestationExtension(Certificate certificate){
        ParsedAttestationRecord parsedAttestationRecord = null;
        try {
            parsedAttestationRecord = createParsedAttestationRecord((X509Certificate) certificate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  parsedAttestationRecord;
    }

    public static AuthorizationList getTeeAuthorizationList(Certificate certificate){
        ParsedAttestationRecord parsedAttestationRecord = null;
        try {
            parsedAttestationRecord = createParsedAttestationRecord((X509Certificate) certificate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parsedAttestationRecord.teeEnforced;
    }

    public static boolean requiresAuthentication(Certificate certificate){
        ParsedAttestationRecord parsedAttestationRecord = null;
        try {
            parsedAttestationRecord = createParsedAttestationRecord((X509Certificate) certificate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        AuthorizationList teeEnforced = parsedAttestationRecord.teeEnforced;
        return !teeEnforced.noAuthRequired;
    }

    public static String getKeyUsageString(X509Certificate certificate){
        // Extract the key usage
        /*
         * KeyUsage::= BIT STRING { digitalSignature (0), nonRepudiation (1),
         * keyEncipherment (2), dataEncipherment (3), keyAgreement (4),
         * keyCertSign (5), cRLSign (6), encipherOnly (7), decipherOnly (8) }
         */
        boolean[] keyUsageFlags = certificate.getKeyUsage();
        ArrayList<String> keyUsage = new ArrayList<>();
        if (keyUsageFlags != null) {
            if (keyUsageFlags[0]) {
                keyUsage.add("Digital Signature");
            }
            if (keyUsageFlags[1]) {
                keyUsage.add("Non Repudiation");
            }
            if (keyUsageFlags[2]) {
                keyUsage.add("Key Encipherment");
            }
            if (keyUsageFlags[3]) {
                keyUsage.add("Data Encipherment");
            }
            if (keyUsageFlags[4]) {
                keyUsage.add("Key Agreement");
            }
            if (keyUsageFlags[5]) {
                keyUsage.add("Key Cert Sign");
            }
            if (keyUsageFlags[6]) {
                keyUsage.add("CRL Sign");
            }
            if (keyUsageFlags[7]) {
                keyUsage.add("Encipher Only");
            }
            if (keyUsageFlags[8]) {
                keyUsage.add("Decipher Only");
            }
        }
        return join(keyUsage);
    }

    public static String join(ArrayList<String> arrayList){
        StringBuilder out=new StringBuilder();
        for (String str: arrayList) {
            out.append(str).append(", ");
        }
        if (out.length()>1){
            return out.substring(0,out.length()-2);
        }
        return "";
    }
}
