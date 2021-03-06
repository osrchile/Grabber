package cl.tesis.tls;

import cl.tesis.output.JsonWritable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.DatatypeConverter;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Certificate implements JsonWritable {
    private static String BEGIN_CERT ="-----BEGIN CERTIFICATE-----";
    private static String END_CERT ="-----END CERTIFICATE-----";

    private String certificateAuthority;
    private String signatureAlgorithm;
    private Date expiredTime;
    private String organizationName;
    private String organizationURL;
    private String keyBits;
    private String PemCert;

    public Certificate(X509Certificate x509Certificate) {
        Map<String, String> issuerMap = parserX500Principal(x509Certificate.getIssuerX500Principal());
        Map<String, String> subjectMap = parserX500Principal(x509Certificate.getSubjectX500Principal());

        this.certificateAuthority = issuerMap.get("CN");
        this.signatureAlgorithm = x509Certificate.getSigAlgName();
        this.expiredTime = x509Certificate.getNotAfter();
        this.organizationName = subjectMap.get("O");
        this.organizationURL = subjectMap.get("CN");
        this.keyBits = parserPublicKeyBits(x509Certificate);
        this.PemCert = toPemFormat(x509Certificate);
    }

    protected Map<String, String> parserX500Principal(X500Principal x500Principal) {
        String[] values = x500Principal.getName().split(",");

        Map<String, String> principalMap = new HashMap<>();

        for (String value : values) {
            String[] elements = value.split("=");

            if (elements.length >= 2) {
                principalMap.put(elements[0], elements[1]);
            }
        }

        return principalMap;
    }

    protected String parserPublicKeyBits(X509Certificate x509Certificate) {
        String[] publicKey = x509Certificate.getPublicKey().toString().split("[\r\n]+");
        Pattern pattern =  Pattern.compile("(.*), (.*) bits");
        Matcher matcher = pattern.matcher(publicKey[0]);
        boolean success = matcher.find();

        if (success) {
            return matcher.group(2);
        }

        return null;
    }

    protected String toPemFormat(X509Certificate x509Certificate) {
        String pemFormat = "";

        try {
            pemFormat += BEGIN_CERT + "\n";
            pemFormat += DatatypeConverter.printBase64Binary(x509Certificate.getEncoded());
            pemFormat += "\n" + END_CERT;
        }catch (CertificateEncodingException e) {
            pemFormat = "";
        }

        return pemFormat;
    }

    public static Certificate[] parseCertificateChain(X509Certificate[] certs) {
        Certificate[] certificates =  new Certificate[certs.length];

        for (int i = 0; i < certs.length ; i++) {
            certificates[i] = new Certificate(certs[i]);
        }

        return certificates;
    }

    @Override
    public String toString() {
        return "Certificate{" +
                "signatureAlgorithm='" + signatureAlgorithm + '\'' +
                ", expiredTime=" + expiredTime +
                ", organizationName='" + organizationName + '\'' +
                ", organizationURL='" + organizationURL + '\'' +
                ", keyBits='" + keyBits + '\'' +
                ", PemCert='" + PemCert + '\'' +
                '}';
    }

    @Override
    public String toJson() {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        return gson.toJson(this);
    }

    public String getCertificateAuthority() {
        return certificateAuthority;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public Date getExpiredTime() {
        return expiredTime;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getOrganizationURL() {
        return organizationURL;
    }

    public String getKeyBits() {
        return keyBits;
    }

    public String getPemCert() {
        return PemCert;
    }
}
