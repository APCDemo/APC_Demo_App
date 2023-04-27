package ch.bfh.securevote.utils;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;


public class CertificateParserTest {
    // Our APC test certificate with attestation extension
    private static String pemCert = "MIIDRTCCAuygAwIBAgIBATAKBggqhkjOPQQDAjA/MRIwEAYDVQQMDAlTdHJvbmdC" +
            "b3gxKTAnBgNVBAUTIDA2ODQyZjg0YmNiYWRiZDE5NjQwNWJmZDZhNjM0OWViMB4X" +
            "DTIzMDMyOTA4MzgzNloXDTI0MDMyOTA5MzgzNlowSzELMAkGA1UEBhMCQ0gxDDAK" +
            "BgNVBAsTA0JGSDETMBEGA1UECxMKU2VjdXJlVm90ZTEZMBcGA1UEAxMQQkZIIEFQ" +
            "QyBEZW1vIEFwcDBZMBMGByqGSM49AgEGCCqGSM49AwEHA0IABFejuuikx8MU+Q8J" +
            "4LjJgfuCis1gTDzWSS/8NEtnSp5oKmo4v3L8dV9pvi0+qga/SQcvtkWZKkKuZc8A" +
            "0t8IbmejggHLMIIBxzAOBgNVHQ8BAf8EBAMCB4AwggGzBgorBgEEAdZ5AgERBIIB" +
            "ozCCAZ8CAWQKAQICAWQKAQIEJDM0NjVlZjIzLTMyOGItNGJlYi05M2RhLTQzMjQw" +
            "MzcwMzQ2ZgQAMHa/gxAIAgYBhyyE2oC/gxEIAgYBjomTUQC/gxIIAgYBjomTUQC/" +
            "hT0IAgYBhyyE2oy/hUVCBEAwPjEYMBYEEWNoLmJmaC5zZWN1cmV2b3RlAgERMSIE" +
            "IFYx474QeKzzVsCZ+B2voFpQ4h6VXlOp47FXcc0LnJA2MIHwoQUxAwIBAqIDAgED" +
            "owQCAgEApQsxCQIBBAIBBQIBBqoDAgEBv4N3AgUAv4N8AgUAv4N9AgUAv4U+AwIB" +
            "AL+FQEwwSgQgD251yAGDtd7AdLAFTUJx6ZOJ6+SxNrCBneHxULoP+dcBAf8KAQAE" +
            "IDYnS2BR96N8t7nyRg9VMwfDNGcxqcQ5e0a71CNEiUsIv4VBBQIDAfvQv4VCBQID" +
            "AxY/v4VGCAQGZ29vZ2xlv4VHCAQGb3Jpb2xlv4VICAQGb3Jpb2xlv4VMCAQGR29v" +
            "Z2xlv4VNCQQHUGl4ZWwgNr+FTgYCBAE0sKG/hU8GAgQBNLChMAoGCCqGSM49BAMC" +
            "A0cAMEQCIFJGWP8TcvC6eKK2bXEkpm5lKbJj957GdHvHSh50S+4BAiAo46oHHJ2V" +
            "cv6WCdcDOjRsCJOVq2RoSwEK2qaA2uiZ3w==";

    // get X509Certificate from pem string
    private static Certificate getCertificate() throws CertificateException {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return cf.generateCertificate(new ByteArrayInputStream(Base64.getDecoder().decode(pemCert.getBytes())));
    }

    // Test for getKeyType()
    @Test
    public void getKeyType() throws CertificateException {
        Certificate cert = getCertificate();
        String keyType = CertificateParser.getKeyType(cert);
        assert keyType.equals("EC");
    }

    // Test for getEcKeyLength()
    @Test
    public void getEcKeyLength() throws CertificateException {
        Certificate cert = getCertificate();
        String keyLength = CertificateParser.getEcKeyLength(cert);
        assert keyLength.equals("256 bits");
    }

    // Test for getKeyUsageString()
    @Test
    public void getKeyUsageString() throws CertificateException {
        X509Certificate cert = (X509Certificate) getCertificate();
        String keyUsage = CertificateParser.getKeyUsageString(cert);
        assert keyUsage.equals("Digital Signature");
    }

    //Test for requiresAuthentication()
    @Test
    public void requiresAuthentication() throws CertificateException {
        X509Certificate cert = (X509Certificate) getCertificate();
        boolean requiresAuthentication = CertificateParser.requiresAuthentication(cert);
        // the given certificate does not require authentication
        assert !requiresAuthentication;
    }

    //Test for requiresProtectedConfirmation()
    @Test
    public void requiresProtectedConfirmation() throws CertificateException {
        X509Certificate cert = (X509Certificate) getCertificate();
        boolean requiresProtectedConfirmation = CertificateParser.requiresProtectedConfirmation(cert);
        assert requiresProtectedConfirmation;
    }
}
