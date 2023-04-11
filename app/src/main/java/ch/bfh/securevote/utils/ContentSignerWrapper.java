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

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.ContentSigner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class ContentSignerWrapper implements ContentSigner {
    protected AlgorithmIdentifier myAlgorithmIdentifier = null;
    protected byte[] signature = null;

    public static Map<String, AlgorithmIdentifier> ALGOS = new HashMap<>();
    static {
        ALGOS.put("SHA256withECDSA".toLowerCase(), new AlgorithmIdentifier(
                new ASN1ObjectIdentifier("1.2.840.10045.4.3.2")));
        ALGOS.put("SHA256withRSA".toLowerCase(), new AlgorithmIdentifier(
                new ASN1ObjectIdentifier("1.2.840.113549.1.1.11")));
        ALGOS.put("SHA1withRSA".toLowerCase(), new AlgorithmIdentifier(
                new ASN1ObjectIdentifier("1.2.840.113549.1.1.5")));

    }

    public ContentSignerWrapper(String algorithmIdentifier, byte[] signature) throws Exception {
        if (ALGOS.containsKey(algorithmIdentifier.toLowerCase())) {
            this.myAlgorithmIdentifier = ALGOS.get(algorithmIdentifier.toLowerCase());
            this.signature = signature;
        } else {
            throw new Exception(String.format("AlgorithmIdentifier %s not yet defined! Please define it!", algorithmIdentifier));
        }
    }

    @Override
    public AlgorithmIdentifier getAlgorithmIdentifier() {
        return myAlgorithmIdentifier;
    }

    @Override
    public OutputStream getOutputStream() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(signature.length);
        try {
            byteArrayOutputStream.write(signature);
            return byteArrayOutputStream;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public byte[] getSignature() {
        return signature;
    }
}
