package com.keybridge;

import com.keybridge.dto.verify.VerifyResponse;
import com.keybridge.entity.Certificate;
import com.keybridge.entity.User;
import com.keybridge.enums.CertStatus;
import com.keybridge.enums.Role;
import com.keybridge.enums.VerifyResult;
import com.keybridge.repository.CertificateRepository;
import com.keybridge.repository.VerificationLogRepository;
import com.keybridge.service.CertificateParserService;
import com.keybridge.service.CryptoService;
import com.keybridge.service.TestCaService;
import com.keybridge.service.VerificationService;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerificationServiceTest {

    private CryptoService cryptoService;
    private CertificateParserService parserService;
    private TestCaService testCaService;
    private CertificateRepository certificateRepository;
    private VerificationLogRepository logRepository;
    private VerificationService verificationService;

    private User testUser;
    private MockMultipartFile docFile;
    private byte[] docBytes;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        cryptoService = new CryptoService();
        parserService = new CertificateParserService(cryptoService);
        testCaService = new TestCaService(tempDir.toString(), "changeit", "keybridge-root");
        testCaService.ensureRoot();

        certificateRepository = mock(CertificateRepository.class);
        logRepository = mock(VerificationLogRepository.class);

        verificationService = new VerificationService(
                cryptoService,
                parserService,
                testCaService,
                certificateRepository,
                logRepository
        );

        testUser = new User("Alice", "alice@example.com", "pass", Role.USER);
        testUser.setId(1L);

        docBytes = "Official contract document payload".getBytes(StandardCharsets.UTF_8);
        docFile = new MockMultipartFile("file", "contract.pdf", "application/pdf", docBytes);
    }

    @Test
    void testVerifyValidSignature() throws Exception {
        TestCaService.IssuedCert issued = testCaService.issue("Alice", "alice@example.com", 30);
        Path p12Path = Path.of(tempDir.toString(), issued.keystoreFile());
        var ks = testCaService.load(p12Path);
        var privKey = (java.security.PrivateKey) ks.getKey("user", "changeit".toCharArray());

        byte[] sig = cryptoService.sign(docBytes, privKey);
        String sigB64 = Base64.getEncoder().encodeToString(sig);

        when(certificateRepository.findBySha256Fingerprint(any())).thenReturn(Optional.empty());

        VerifyResponse response = verificationService.verify(
                docFile,
                sigB64,
                issued.cert().getEncoded(),
                null,
                testUser
        );

        assertEquals(VerifyResult.VALID, response.result());
    }

    @Test
    void testVerifyTamperedDocument() throws Exception {
        TestCaService.IssuedCert issued = testCaService.issue("Alice", "alice@example.com", 30);
        Path p12Path = Path.of(tempDir.toString(), issued.keystoreFile());
        var ks = testCaService.load(p12Path);
        var privKey = (java.security.PrivateKey) ks.getKey("user", "changeit".toCharArray());

        byte[] sig = cryptoService.sign(docBytes, privKey);
        String sigB64 = Base64.getEncoder().encodeToString(sig);

        MockMultipartFile tamperedFile = new MockMultipartFile("file", "contract.pdf", "application/pdf",
                "Modified contract document payload".getBytes(StandardCharsets.UTF_8));

        when(certificateRepository.findBySha256Fingerprint(any())).thenReturn(Optional.empty());

        VerifyResponse response = verificationService.verify(
                tamperedFile,
                sigB64,
                issued.cert().getEncoded(),
                null,
                testUser
        );

        assertEquals(VerifyResult.TAMPERED, response.result());
    }

    @Test
    void testVerifyUntrustedRoot() throws Exception {
        // Generate a separate, untrusted rogue Root CA and cert
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        KeyPair rogueKp = g.generateKeyPair();
        X500Name name = new X500Name("CN=Rogue Untrusted CA, O=Untrusted, C=US");

        Date from = new Date(System.currentTimeMillis() - 60_000);
        Date to = new Date(System.currentTimeMillis() + 86_400_000L);
        JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(
                name, new BigInteger(64, new SecureRandom()), from, to, name, rogueKp.getPublic()
        );
        ContentSigner cs = new JcaContentSignerBuilder("SHA256withRSA").build(rogueKp.getPrivate());
        X509Certificate rogueCert = new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().getCertificate(b.build(cs));

        byte[] sig = cryptoService.sign(docBytes, rogueKp.getPrivate());
        String sigB64 = Base64.getEncoder().encodeToString(sig);

        VerifyResponse response = verificationService.verify(
                docFile,
                sigB64,
                rogueCert.getEncoded(),
                null,
                testUser
        );

        assertEquals(VerifyResult.UNTRUSTED, response.result());
    }

    @Test
    void testVerifyRevokedCertificate() throws Exception {
        TestCaService.IssuedCert issued = testCaService.issue("Alice", "alice@example.com", 30);
        Path p12Path = Path.of(tempDir.toString(), issued.keystoreFile());
        var ks = testCaService.load(p12Path);
        var privKey = (java.security.PrivateKey) ks.getKey("user", "changeit".toCharArray());

        byte[] sig = cryptoService.sign(docBytes, privKey);
        String sigB64 = Base64.getEncoder().encodeToString(sig);

        Certificate dbCert = new Certificate();
        dbCert.setId(10L);
        dbCert.setStatus(CertStatus.REVOKED);
        dbCert.setRevokeReason("Private key compromised");

        when(certificateRepository.findBySha256Fingerprint(any())).thenReturn(Optional.of(dbCert));

        VerifyResponse response = verificationService.verify(
                docFile,
                sigB64,
                issued.cert().getEncoded(),
                null,
                testUser
        );

        assertEquals(VerifyResult.REVOKED, response.result());
    }

    @Test
    void testVerifyErrorOnInvalidCert() {
        byte[] invalidCertBytes = "NOT A VALID X509 CERT".getBytes(StandardCharsets.UTF_8);

        VerifyResponse response = verificationService.verify(
                docFile,
                "some-sig",
                invalidCertBytes,
                null,
                testUser
        );

        assertEquals(VerifyResult.ERROR, response.result());
    }
}
