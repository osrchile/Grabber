package cl.tesis.mail;


import cl.tesis.input.FileReader;
import cl.tesis.mail.exception.ConnectionException;
import cl.tesis.output.FileWriter;
import cl.tesis.tls.Certificate;
import cl.tesis.tls.ScanCipherSuites;
import cl.tesis.tls.ScanTLSProtocols;
import cl.tesis.tls.TLSHandshake;
import cl.tesis.tls.exception.*;

import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IMAPThread extends Thread{
    private static final Logger logger = Logger.getLogger(IMAPThread.class.getName());
    private static final int IP = 0;

    private FileReader reader;
    private FileWriter writer;
    private int port;
    private boolean needStartTLS;
    private StartTLS startTLS;
    private boolean allProtocols;
    private boolean allCiphersSuites;
    private boolean heartbleed;


    public IMAPThread(FileReader reader, FileWriter writer, int port, boolean startTLS, boolean allProtocols, boolean allCiphersSuites, boolean heartbleed) {
        this.reader = reader;
        this.writer = writer;
        this.port = port;
        this.needStartTLS = startTLS;
        this.allProtocols = allProtocols;
        this.allCiphersSuites = allCiphersSuites;
        this.heartbleed = heartbleed;

        if (needStartTLS) {
            this.startTLS = StartTLS.IMAP;
        }
    }

    @Override
    public void run() {
        String[] columns;

        while((columns = this.reader.nextLine()) != null) {
            IMAPData data = new IMAPData(columns[IP]);
            try {
                if (needStartTLS) { // STARTTLS
                    IMAP imap = new IMAP(columns[IP], this.port);
                    data.setBanner(imap.readBanner());

                    TLSHandshake tlsHandshake = new TLSHandshake(imap.getSocket(), this.startTLS);
                    tlsHandshake.connect();
                    X509Certificate[] certs = tlsHandshake.getChainCertificate();
                    data.setChain(Certificate.parseCertificateChain(certs));

                    /* Check all SSL/TLS Protocols*/
                    if (allProtocols) {
                        ScanTLSProtocols protocols = new ScanTLSProtocols(columns[IP], port);
                        data.setProtocols(protocols.scanAllProtocols(this.startTLS));
                    }

                    /* Check all Cipher Suites */
                    if (allCiphersSuites) {
                        ScanCipherSuites cipherSuites = new ScanCipherSuites(columns[IP], port);
                        data.setCiphersSuites(cipherSuites.scanAllCipherSuites(this.startTLS));
                    }

                    /* Heartbleed test*/
//                    if (heartbleed)
//                        data.setHeartbleed(tls.heartbleedTest(this.startTLS, TLSVersion.TLS_12));

                } else { // Secure Port
                    TLSHandshake tlsHandshake = new TLSHandshake(columns[IP], port);
                    tlsHandshake.connect();
                    X509Certificate[] certs = tlsHandshake.getChainCertificate();
                    data.setChain(Certificate.parseCertificateChain(certs));

                    /* Check all SSL/TLS Protocols*/
                    if (allProtocols) {
                        ScanTLSProtocols protocols = new ScanTLSProtocols(columns[IP], port);
                        data.setProtocols(protocols.scanAllProtocols());
                    }

                    /* Check all Cipher Suites */
                    if (allCiphersSuites) {
                        ScanCipherSuites cipherSuites = new ScanCipherSuites(columns[IP], port);
                        data.setCiphersSuites(cipherSuites.scanAllCipherSuites());
                    }

                    /* Heartbleed test*/
//                    if (heartbleed)
//                        data.setHeartbleed(tls.heartbleedTest(this.startTLS, TLSVersion.TLS_12));

                }
            } catch (ConnectionException e) {
                data.setError(e.getMessage());
                logger.log(Level.INFO, "Connection Exception {0},  {1}", new String[]{columns[IP], e.getMessage()});
            } catch (SocketTLSHandshakeException | TLSConnectionException e) {
                data.setError("Connection error");
                logger.log(Level.INFO, "Connection error {0}", columns[IP]);
            } catch (StartTLSException e) {
                data.setError("Start Protocol error");
                logger.log(Level.INFO, "Start Protocol error {0}", columns[IP]);
            }  catch (TLSHandshakeException e) {
                data.setError("Handshake error");
                logger.log(Level.INFO, "Handshake error {0}", columns[IP]);
            } catch (TLSGetCertificateException e) {
                data.setError("Certificate get error");
                logger.log(Level.INFO, "Certificate get error {0}", columns[IP]);
            }

            this.writer.writeLine(data);
        }
    }
}
