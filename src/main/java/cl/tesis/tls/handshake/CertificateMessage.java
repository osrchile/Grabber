package cl.tesis.tls.handshake;

import cl.tesis.tls.exception.HandshakeHeaderException;
import cl.tesis.tls.exception.TLSHeaderException;
import cl.tesis.tls.util.TLSUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CertificateMessage {

    public static final int TLS_HEADER_LENGTH = 5;
    public static final int HANDSHAKE_HEADER_LENGTH = 4;

    private byte[] tlsHeader;
    private byte[] handshakeHeader;
    private byte[] handshakeBody;
    private byte[] certificateList;
    private List<byte[]> certificates;

    public CertificateMessage(byte[] array, int startMessage) throws TLSHeaderException, HandshakeHeaderException {
        /* TLS */
            this.tlsHeader = Arrays.copyOfRange(array, startMessage, startMessage + TLS_HEADER_LENGTH);
            int tlsBodyLength = (this.tlsHeader[3] & 0xFF) << 8 | (this.tlsHeader[4] & 0xFF);
            if (!checkTLSHeader())
                throw new TLSHeaderException("Error in Certificate header");

        /* Handshake */
            this.handshakeHeader = Arrays.copyOfRange(array, startMessage + TLS_HEADER_LENGTH, startMessage + TLS_HEADER_LENGTH + HANDSHAKE_HEADER_LENGTH);
            if (!checkHandshakeHeader())
                throw new HandshakeHeaderException("Error in Certificate header");
            this.handshakeBody = Arrays.copyOfRange(array, startMessage + TLS_HEADER_LENGTH + HANDSHAKE_HEADER_LENGTH, startMessage + tlsBodyLength + TLS_HEADER_LENGTH);

        /* Certificate List */
            int certificateLengthList = (this.handshakeBody[0] & 0xFF) << 16 | (this.handshakeBody[1] & 0xFF) << 8 | (this.handshakeBody[2] & 0xFF);
            this.certificateList = Arrays.copyOfRange(this.handshakeBody, 3, certificateLengthList + 3);

            this.certificates = new ArrayList<>();
            for (int i = 0; i < certificateLengthList; ) {
                int certLength = (this.certificateList[i] & 0xFF) << 16 | (this.certificateList[i + 1] & 0xFF) << 8 | (this.certificateList[i + 2] & 0xFF);
                if (certLength == 0)
                    break;
                byte[] certificate = Arrays.copyOfRange(this.certificateList, i + 3, i + certLength + 3);
                certificates.add(certificate);
                i += certLength + 3;
            }
    }

    private boolean checkTLSHeader() {
        return this.tlsHeader[0] == 0x16;
    }

    private boolean checkHandshakeHeader() {
        return this.handshakeHeader[0] == 0x0B;
    }

    private String certificatesToString() {
        String certs = "[";
        for (byte[] cert : this.certificates) {
            certs += TLSUtil.bytesToHex(cert) + " , ";
        }
        return certs + "]";
    }

    public List<byte[]> getCertificates() {
        return certificates;
    }

    @Override
    public String toString() {
        return "CertificateMessage{" +
                "tlsHeader=" + TLSUtil.bytesToHex(tlsHeader) +
                ", handshakeHeader=" + TLSUtil.bytesToHex(handshakeHeader) +
                ", certificates=" + certificatesToString() +
                '}';
    }
}
