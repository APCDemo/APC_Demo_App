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

import android.util.Log;

import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.SignerInfoGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.DigestCalculatorProvider;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.Store;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

public class PKCS7Builder {
    static public final String TAG = PKCS7Builder.class.getName();

    /**
     * Generates a PKCS7 signature including the certificate chain
     * @param dataToSign
     * @param signature
     * @param signatureAlgorithm
     * @return
     */
    public static byte[] generatePkcs7(byte[] dataToSign, byte[] signature, String signatureAlgorithm){

        try {
            final CMSSignedDataGenerator cmsSignedDataGenerator = new CMSSignedDataGenerator();
            CMSTypedData msg = new CMSProcessableByteArray(dataToSign);
            List<Certificate> certList = Arrays.asList(HpcUtility.getCertChain(Constants.KEY_NAME));
            Store certs = new JcaCertStore(certList);
            cmsSignedDataGenerator.addCertificates(certs);

            JcaDigestCalculatorProviderBuilder jcaDigestCalculatorProviderBuilder = new JcaDigestCalculatorProviderBuilder();
            DigestCalculatorProvider digestCalculatorProvider = jcaDigestCalculatorProviderBuilder.build();
            ContentSignerWrapper signer = new ContentSignerWrapper(signatureAlgorithm, signature);
            JcaSignerInfoGeneratorBuilder jcaSignerInfoGeneratorBuilder = new JcaSignerInfoGeneratorBuilder(digestCalculatorProvider);
            jcaSignerInfoGeneratorBuilder.setDirectSignature(true);
            Certificate cert = HpcUtility.getCert(Constants.KEY_NAME);
            X509Certificate x509Certificate = (X509Certificate) cert;
            SignerInfoGenerator signerInfoGenerator = jcaSignerInfoGeneratorBuilder.build(signer, x509Certificate);

            cmsSignedDataGenerator.addSignerInfoGenerator(signerInfoGenerator);

            CMSSignedData sigData = cmsSignedDataGenerator.generate(msg, true);
            return sigData.toASN1Structure().getEncoded("DER");

        } catch (Exception ex){
            Log.e(TAG, String.format("Generating PKCS7 failed with: %s", ex));
        }
        return new byte[] {};
    }

    /**
     * Generates a PEM encoded PKCS7 signature
     * @param pkcs7
     * @return
     */
    public static String generatePkcs7Pem(byte[] pkcs7){
        StringBuffer sb = new StringBuffer();
        try{
            sb.append("-----BEGIN PKCS7-----\n");
            sb.append(android.util.Base64.encodeToString(pkcs7, android.util.Base64.DEFAULT));
            sb.append("-----END PKCS7----- \n");
        }catch (Exception ex){
            Log.e(TAG, String.format("Failed to get PEM encoded certificate: %s", ex));
        }
        return sb.toString();
    }
}
