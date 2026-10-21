# KeyBridge: Digital Signature, PKI and Cryptographic Token Management Platform

> **How to use this file in Antigravity:** Put this file in the project root as `SPEC.md`. Open the workspace, then give the agent the prompts from **Section 14** one phase at a time. Tell the agent: *"Read SPEC.md fully before writing code. Follow it exactly."*

---

## 1. Project Overview

KeyBridge is a web platform to manage digital certificates and crypto tokens, sign documents, verify signatures, track certificate expiry, handle service requests, and keep audit logs.

**Scope rules (important):**
- Use **test certificates only**, issued by an internal Test Root CA the app generates.
- No real PKCS#11 hardware tokens, no government portals, no real Certifying Authorities. Mention as future scope.
- Private keys are **never stored in the database**. They live in password-protected `.p12` files on disk.

**Roles**
| Role | Can do |
|---|---|
| ADMIN | Everything: users, tokens, certificates, issue test certs, revoke, approve requests, view audit |
| USER | View own certs/tokens, sign documents, verify documents, raise service requests |
| AUDITOR | Read-only access to audit logs, verification logs and certificate list |

---

## 2. Tech Stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, Validation, Spring AOP |
| JWT | jjwt 0.12.x |
| Crypto | `java.security`, `KeyStore` (PKCS12), Bouncy Castle (`bcprov-jdk18on`, `bcpkix-jdk18on`) |
| DB | MySQL 8 (PostgreSQL also works) |
| Frontend | React 18 + Vite, React Router, Axios, Tailwind CSS, **Framer Motion**, Recharts, Lucide, Sonner |
| Build | Maven, npm |

---

## 3. Repository Structure

```
keybridge/
├── SPEC.md
├── backend/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/keybridge/
│       │   ├── KeyBridgeApplication.java
│       │   ├── config/        (SecurityConfig, CorsConfig, AppProperties)
│       │   ├── security/      (JwtService, JwtAuthFilter, UserDetailsServiceImpl)
│       │   ├── entity/        (User, Token, Certificate, SignedDocument,
│       │   │                   VerificationLog, ServiceRequest, AuditLog)
│       │   ├── enums/         (Role, TokenStatus, CertStatus, RequestStatus,
│       │   │                   RequestType, VerifyResult)
│       │   ├── repository/    (one JpaRepository per entity)
│       │   ├── dto/           (request/response records)
│       │   ├── service/       (AuthService, UserService, TokenService, CertificateService,
│       │   │                   CryptoService, CertificateParserService, TestCaService,
│       │   │                   SigningService, VerificationService, ExpiryMonitorService,
│       │   │                   ServiceRequestService, AuditService)
│       │   ├── controller/    (AuthController, UserController, TokenController,
│       │   │                   CertificateController, SignController, VerifyController,
│       │   │                   RequestController, AuditController, DashboardController)
│       │   ├── audit/         (@Audited, AuditAspect)
│       │   └── exception/     (GlobalExceptionHandler, custom exceptions)
│       └── resources/application.yml
├── keystore-data/             (git-ignored: root-ca.p12, user .p12 files, signed files)
└── frontend/            (interactive dashboard UI, see Section 8.2 for the full tree)
    ├── package.json
    └── src/ (api, lib/motion.js, context, layout, components/{ui,charts,crypto}, pages)
```

---

## 4. Database Schema (MySQL)

```sql
CREATE DATABASE keybridge;
USE keybridge;

CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(150) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role ENUM('ADMIN','USER','AUDITOR') NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tokens (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  serial_no VARCHAR(60) NOT NULL UNIQUE,
  label VARCHAR(100),
  owner_id BIGINT NULL,
  status ENUM('AVAILABLE','ASSIGNED','LOST','RETIRED') NOT NULL DEFAULT 'AVAILABLE',
  assigned_date DATE NULL,
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE certificates (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_id BIGINT NULL,
  owner_id BIGINT NOT NULL,
  subject VARCHAR(300) NOT NULL,
  issuer VARCHAR(300) NOT NULL,
  serial_hex VARCHAR(100) NOT NULL,
  valid_from DATETIME NOT NULL,
  valid_to DATETIME NOT NULL,
  sha256_fingerprint CHAR(64) NOT NULL UNIQUE,
  pem LONGTEXT NOT NULL,                 -- public certificate only
  keystore_file VARCHAR(200) NULL,       -- name of .p12 file on disk (test certs only)
  status ENUM('VALID','EXPIRING','EXPIRED','REVOKED') NOT NULL DEFAULT 'VALID',
  revoked_at DATETIME NULL,
  revoke_reason VARCHAR(200) NULL,
  FOREIGN KEY (token_id) REFERENCES tokens(id),
  FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE TABLE signed_documents (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  cert_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  doc_hash CHAR(64) NOT NULL,
  signature_b64 LONGTEXT NOT NULL,
  algorithm VARCHAR(30) NOT NULL DEFAULT 'SHA256withRSA',
  signed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (cert_id) REFERENCES certificates(id)
);

CREATE TABLE verification_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  verified_by BIGINT NOT NULL,
  file_name VARCHAR(255),
  cert_id BIGINT NULL,
  result ENUM('VALID','TAMPERED','EXPIRED','REVOKED','UNTRUSTED','NOT_YET_VALID','ERROR') NOT NULL,
  reason VARCHAR(300),
  verified_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (verified_by) REFERENCES users(id)
);

CREATE TABLE service_requests (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  cert_id BIGINT NULL,
  type ENUM('NEW_TOKEN','RENEW_CERT','REVOKE_CERT','LOST_TOKEN') NOT NULL,
  status ENUM('PENDING','APPROVED','REJECTED','COMPLETED') NOT NULL DEFAULT 'PENDING',
  remarks VARCHAR(500),
  admin_note VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NULL,
  action VARCHAR(80) NOT NULL,
  details VARCHAR(500),
  ip VARCHAR(60),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_cert_status ON certificates(status);
CREATE INDEX idx_cert_valid_to ON certificates(valid_to);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
```

Use `spring.jpa.hibernate.ddl-auto=validate` once the schema exists (or `update` during early dev).

---

## 5. Backend Setup

### 5.1 `pom.xml` dependencies
```xml
<dependencies>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
  <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-aop</artifactId></dependency>
  <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
  <dependency><groupId>org.bouncycastle</groupId><artifactId>bcprov-jdk18on</artifactId><version>1.78.1</version></dependency>
  <dependency><groupId>org.bouncycastle</groupId><artifactId>bcpkix-jdk18on</artifactId><version>1.78.1</version></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.12.6</version></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
  <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.12.6</version><scope>runtime</scope></dependency>
  <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><optional>true</optional></dependency>
</dependencies>
```

### 5.2 `application.yml`
```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/keybridge
    username: root
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

keybridge:
  jwt:
    secret: ${JWT_SECRET:change-this-to-a-long-random-string-at-least-32-chars}
    expiry-minutes: 120
  keystore:
    dir: ./keystore-data
    password: ${KS_PASSWORD:changeit}      # test only
    root-alias: keybridge-root
  expiry:
    warn-days: 30
    cron: "0 0 1 * * *"                    # daily 1 AM
```

### 5.3 Enums
```java
public enum Role { ADMIN, USER, AUDITOR }
public enum TokenStatus { AVAILABLE, ASSIGNED, LOST, RETIRED }
public enum CertStatus { VALID, EXPIRING, EXPIRED, REVOKED }
public enum RequestType { NEW_TOKEN, RENEW_CERT, REVOKE_CERT, LOST_TOKEN }
public enum RequestStatus { PENDING, APPROVED, REJECTED, COMPLETED }
public enum VerifyResult { VALID, TAMPERED, EXPIRED, REVOKED, UNTRUSTED, NOT_YET_VALID, ERROR }
```

### 5.4 Entities
Create JPA entities mirroring the tables in Section 4 (`@Entity`, `@Table`, Lombok `@Getter @Setter @NoArgsConstructor`). Use `@Enumerated(EnumType.STRING)` for all enums, `@ManyToOne(fetch = LAZY)` for foreign keys, `LocalDateTime` for dates. Never expose entities directly from controllers; map to DTO records.

---

## 6. Core Logic (the heart of the project)

### 6.1 CryptoService
```java
@Service
public class CryptoService {

    public String sha256Hex(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(data);
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    public byte[] sign(byte[] data, PrivateKey key) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initSign(key);
            s.update(data);
            return s.sign();
        } catch (GeneralSecurityException e) { throw new CryptoException("Signing failed", e); }
    }

    public boolean verify(byte[] data, byte[] sig, PublicKey key) {
        try {
            Signature s = Signature.getInstance("SHA256withRSA");
            s.initVerify(key);
            s.update(data);
            return s.verify(sig);
        } catch (GeneralSecurityException e) { return false; }
    }
}
```

### 6.2 CertificateParserService
```java
@Service
public class CertificateParserService {
    private final CryptoService crypto;

    public X509Certificate parse(byte[] bytes) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(bytes));
        } catch (CertificateException e) { throw new BadRequestException("Invalid certificate file"); }
    }

    public CertMeta extract(X509Certificate c) throws CertificateEncodingException {
        return new CertMeta(
            c.getSubjectX500Principal().getName(),
            c.getIssuerX500Principal().getName(),
            c.getSerialNumber().toString(16),
            LocalDateTime.ofInstant(c.getNotBefore().toInstant(), ZoneId.systemDefault()),
            LocalDateTime.ofInstant(c.getNotAfter().toInstant(), ZoneId.systemDefault()),
            crypto.sha256Hex(c.getEncoded()),
            toPem(c));
    }

    public String toPem(X509Certificate c) throws CertificateEncodingException {
        String b64 = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(c.getEncoded());
        return "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
    }
}
public record CertMeta(String subject, String issuer, String serialHex,
        LocalDateTime validFrom, LocalDateTime validTo, String fingerprint, String pem) {}
```

### 6.3 TestCaService (generates root CA and issues user certs)
```java
@Service
public class TestCaService {
    static { Security.addProvider(new BouncyCastleProvider()); }

    @Value("${keybridge.keystore.dir}") private String dir;
    @Value("${keybridge.keystore.password}") private String ksPass;
    @Value("${keybridge.keystore.root-alias}") private String rootAlias;

    private Path rootPath() { return Path.of(dir, "root-ca.p12"); }

    /** Called once at startup via @PostConstruct. Creates root CA if missing. */
    @PostConstruct
    public void ensureRoot() throws Exception {
        Files.createDirectories(Path.of(dir));
        if (Files.exists(rootPath())) return;
        KeyPair kp = newKeyPair();
        X500Name name = new X500Name("CN=KeyBridge Test Root CA, O=KeyBridge, C=IN");
        X509Certificate cert = build(name, name, kp.getPublic(), kp.getPrivate(), 3650, true);
        saveP12(rootPath(), rootAlias, kp.getPrivate(), new Certificate[]{cert});
    }

    public IssuedCert issue(String commonName, String email, int validDays) throws Exception {
        KeyStore rootKs = load(rootPath());
        PrivateKey rootKey = (PrivateKey) rootKs.getKey(rootAlias, ksPass.toCharArray());
        X509Certificate root = (X509Certificate) rootKs.getCertificate(rootAlias);

        KeyPair kp = newKeyPair();
        X500Name subject = new X500Name("CN=" + commonName + ", E=" + email + ", O=KeyBridge Users");
        X500Name issuer = new X500Name(root.getSubjectX500Principal().getName());
        X509Certificate cert = build(issuer, subject, kp.getPublic(), rootKey, validDays, false);

        String file = "user-" + UUID.randomUUID() + ".p12";
        saveP12(Path.of(dir, file), "user", kp.getPrivate(), new Certificate[]{cert, root});
        return new IssuedCert(cert, file);
    }

    public X509Certificate rootCert() throws Exception {
        return (X509Certificate) load(rootPath()).getCertificate(rootAlias);
    }

    private KeyPair newKeyPair() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
        g.initialize(2048);
        return g.generateKeyPair();
    }

    private X509Certificate build(X500Name issuer, X500Name subject, PublicKey pub,
                                  PrivateKey signKey, int days, boolean ca) throws Exception {
        Date from = new Date(System.currentTimeMillis() - 60_000);
        Date to = new Date(System.currentTimeMillis() + days * 86_400_000L);
        JcaX509v3CertificateBuilder b = new JcaX509v3CertificateBuilder(
            issuer, new BigInteger(64, new SecureRandom()), from, to, subject, pub);
        b.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        b.addExtension(Extension.keyUsage, true, ca
            ? new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign)
            : new KeyUsage(KeyUsage.digitalSignature | KeyUsage.nonRepudiation));
        ContentSigner cs = new JcaContentSignerBuilder("SHA256withRSA").build(signKey);
        return new JcaX509CertificateConverter().getCertificate(b.build(cs));
    }

    private void saveP12(Path p, String alias, PrivateKey k, Certificate[] chain) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry(alias, k, ksPass.toCharArray(), chain);
        try (OutputStream o = Files.newOutputStream(p)) { ks.store(o, ksPass.toCharArray()); }
    }

    public KeyStore load(Path p) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream i = Files.newInputStream(p)) { ks.load(i, ksPass.toCharArray()); }
        return ks;
    }

    public record IssuedCert(X509Certificate cert, String keystoreFile) {}
}
```

### 6.4 SigningService
Rules: only the certificate **owner** (or ADMIN) may sign with it; cert must be `VALID` or `EXPIRING`.
```java
@Service @RequiredArgsConstructor
public class SigningService {
    private final CertificateRepository certRepo;
    private final SignedDocumentRepository signedRepo;
    private final CryptoService crypto;
    private final TestCaService ca;
    @Value("${keybridge.keystore.dir}") private String dir;
    @Value("${keybridge.keystore.password}") private String ksPass;

    @Transactional
    public SignResponse sign(Long certId, MultipartFile file, User current) throws Exception {
        Certificate cert = certRepo.findById(certId).orElseThrow(() -> new NotFoundException("Certificate not found"));
        if (!cert.getOwner().getId().equals(current.getId()) && current.getRole() != Role.ADMIN)
            throw new ForbiddenException("Not your certificate");
        if (cert.getStatus() == CertStatus.REVOKED || cert.getStatus() == CertStatus.EXPIRED)
            throw new BadRequestException("Certificate is " + cert.getStatus());
        if (cert.getKeystoreFile() == null)
            throw new BadRequestException("No private key available for this certificate");

        byte[] data = file.getBytes();
        KeyStore ks = ca.load(Path.of(dir, cert.getKeystoreFile()));
        PrivateKey key = (PrivateKey) ks.getKey("user", ksPass.toCharArray());

        byte[] sig = crypto.sign(data, key);
        SignedDocument d = new SignedDocument();
        d.setUser(current); d.setCert(cert);
        d.setFileName(file.getOriginalFilename());
        d.setDocHash(crypto.sha256Hex(data));
        d.setSignatureB64(Base64.getEncoder().encodeToString(sig));
        signedRepo.save(d);

        return new SignResponse(d.getId(), d.getDocHash(), d.getSignatureB64(), cert.getSha256Fingerprint());
    }
}
```
The frontend downloads the `.sig` file (Base64 text of the signature) and the user keeps the original file.

### 6.5 VerificationService (most important logic)
Inputs: original file, signature (`.sig` text or Base64), and certificate (selected from DB by fingerprint, or uploaded `.cer`).

Verification pipeline, in this exact order:
1. Parse certificate.
2. **Chain check:** `cert.verify(rootPublicKey)`. If it fails → `UNTRUSTED`.
3. **Revocation check:** look up fingerprint in DB; if status `REVOKED` → `REVOKED`.
4. **Validity check:** `cert.checkValidity(signingTime)`. Not yet valid → `NOT_YET_VALID`, expired → `EXPIRED`.
5. **Signature check:** verify with the cert's public key. If false → `TAMPERED`.
6. Otherwise `VALID`.
7. Always write a `verification_logs` row and an audit entry.

```java
@Service @RequiredArgsConstructor
public class VerificationService {
    private final CryptoService crypto;
    private final CertificateParserService parser;
    private final TestCaService ca;
    private final CertificateRepository certRepo;
    private final VerificationLogRepository logRepo;

    public VerifyResponse verify(MultipartFile file, String signatureB64,
                                 byte[] certBytes, User current) {
        VerifyResult result; String reason; Long certId = null;
        try {
            X509Certificate cert = parser.parse(certBytes);
            String fp = crypto.sha256Hex(cert.getEncoded());
            Optional<Certificate> dbCert = certRepo.findBySha256Fingerprint(fp);
            certId = dbCert.map(Certificate::getId).orElse(null);

            try { cert.verify(ca.rootCert().getPublicKey()); }
            catch (GeneralSecurityException e) {
                return log(current, file, certId, VerifyResult.UNTRUSTED, "Not issued by the trusted root");
            }
            if (dbCert.isPresent() && dbCert.get().getStatus() == CertStatus.REVOKED)
                return log(current, file, certId, VerifyResult.REVOKED, "Certificate has been revoked");
            try { cert.checkValidity(); }
            catch (CertificateExpiredException e) {
                return log(current, file, certId, VerifyResult.EXPIRED, "Certificate expired");
            } catch (CertificateNotYetValidException e) {
                return log(current, file, certId, VerifyResult.NOT_YET_VALID, "Certificate not yet valid");
            }
            byte[] sig = Base64.getDecoder().decode(signatureB64.trim());
            boolean ok = crypto.verify(file.getBytes(), sig, cert.getPublicKey());
            result = ok ? VerifyResult.VALID : VerifyResult.TAMPERED;
            reason = ok ? "Signature is valid and document is intact"
                        : "Document was modified or signature does not match";
        } catch (Exception e) {
            result = VerifyResult.ERROR; reason = "Could not process input: " + e.getMessage();
        }
        return log(current, file, certId, result, reason);
    }

    private VerifyResponse log(User u, MultipartFile f, Long certId, VerifyResult r, String reason) {
        VerificationLog l = new VerificationLog();
        l.setVerifiedBy(u); l.setFileName(f.getOriginalFilename());
        l.setCertId(certId); l.setResult(r); l.setReason(reason);
        logRepo.save(l);
        return new VerifyResponse(r, reason);
    }
}
```

### 6.6 ExpiryMonitorService
```java
@Service @RequiredArgsConstructor
public class ExpiryMonitorService {
    private final CertificateRepository repo;
    @Value("${keybridge.expiry.warn-days}") private int warnDays;

    @Scheduled(cron = "${keybridge.expiry.cron}")
    @Transactional
    public void refreshStatuses() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime warn = now.plusDays(warnDays);
        for (Certificate c : repo.findByStatusNot(CertStatus.REVOKED)) {
            CertStatus s = c.getValidTo().isBefore(now) ? CertStatus.EXPIRED
                         : c.getValidTo().isBefore(warn) ? CertStatus.EXPIRING
                         : CertStatus.VALID;
            c.setStatus(s);
        }
    }
}
```
Also expose `POST /api/certs/refresh-status` (ADMIN) to trigger it manually for demos. Add `@EnableScheduling` on the main class.

### 6.7 Service Request state machine
```
PENDING ──approve──▶ APPROVED ──complete──▶ COMPLETED
   └─────reject────▶ REJECTED
```
On `COMPLETE`, perform the action:
- `RENEW_CERT`: issue a new test cert for the user, mark the old one `EXPIRED`
- `REVOKE_CERT`: set cert `REVOKED`, `revoked_at = now`
- `NEW_TOKEN`: assign an `AVAILABLE` token to the user
- `LOST_TOKEN`: mark token `LOST` and revoke linked certs

Invalid transitions must return HTTP 409.

### 6.8 Audit logging
```java
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
public @interface Audited { String value(); }

@Aspect @Component @RequiredArgsConstructor
public class AuditAspect {
    private final AuditService audit;
    @AfterReturning("@annotation(a)")
    public void log(JoinPoint jp, Audited a) { audit.record(a.value(), jp.getArgs()); }
}
```
`AuditService.record` reads the current user from `SecurityContextHolder` and IP from `HttpServletRequest`. Annotate: login, cert upload/issue/revoke, sign, verify, token assign, request status change, user create.

### 6.9 Security (JWT)
```java
@Service
public class JwtService {
    private final SecretKey key;
    private final long expiryMs;
    public JwtService(@Value("${keybridge.jwt.secret}") String secret,
                      @Value("${keybridge.jwt.expiry-minutes}") long mins) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = mins * 60_000;
    }
    public String generate(User u) {
        return Jwts.builder().subject(u.getEmail()).claim("role", u.getRole().name())
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiryMs))
            .signWith(key).compact();
    }
    public String subject(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
    }
}
```
`JwtAuthFilter extends OncePerRequestFilter`: read `Authorization: Bearer ...`, validate, load user, set `SecurityContext`.

`SecurityConfig`:
```java
@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean SecurityFilterChain chain(HttpSecurity http, JwtAuthFilter f) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(f, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
}
```
Use `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints. Seed one admin on startup via `CommandLineRunner` (`admin@keybridge.com` / `Admin@123`, change after first login).

### 6.10 Global error format
All errors return `{ "timestamp", "status", "error", "message" }`. Map: `NotFoundException→404`, `BadRequestException→400`, `ForbiddenException→403`, invalid state transition `→409`, anything else `→500`.

---

## 7. REST API Reference

| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/auth/login` | public | `{email,password}` → `{token,user}` |
| GET | `/api/auth/me` | any | Current user |
| GET/POST | `/api/users` | ADMIN | List / create users |
| PUT | `/api/users/{id}` | ADMIN | Update role / active |
| GET | `/api/tokens` | ADMIN, AUDITOR | List tokens (USER sees own) |
| POST | `/api/tokens` | ADMIN | Create token |
| PUT | `/api/tokens/{id}/assign` | ADMIN | `{userId}` assign token |
| PUT | `/api/tokens/{id}/status` | ADMIN | Mark LOST / RETIRED |
| GET | `/api/certs` | any | List (USER sees own); filters `status`, `q` |
| GET | `/api/certs/{id}` | any | Detail |
| GET | `/api/certs/expiring` | any | Certs in EXPIRING state |
| POST | `/api/certs/upload` | ADMIN | Upload `.cer` / `.pem` (metadata only) |
| POST | `/api/certs/issue` | ADMIN | `{userId,commonName,validDays,tokenId?}` issue test cert |
| PUT | `/api/certs/{id}/revoke` | ADMIN | `{reason}` |
| GET | `/api/certs/{id}/download` | any | Download public cert PEM |
| POST | `/api/certs/refresh-status` | ADMIN | Run expiry job now |
| POST | `/api/sign` | USER, ADMIN | multipart: `file`, `certId` → signature |
| POST | `/api/verify` | any | multipart: `file`, `signature`, `certId` or `certFile` |
| GET | `/api/signed-documents` | any | History (USER sees own) |
| GET | `/api/verifications` | ADMIN, AUDITOR | Verification log |
| POST | `/api/requests` | USER | Raise request |
| GET | `/api/requests` | any | USER sees own, ADMIN sees all |
| PUT | `/api/requests/{id}/status` | ADMIN | `{status, adminNote}` |
| GET | `/api/audit` | ADMIN, AUDITOR | Paginated; filters `userId`, `action`, `from`, `to` |
| GET | `/api/dashboard` | any | Counts and recent activity |
| GET | `/api/dashboard/activity` | any | Signs and verifications per day (`days` param) |
| GET | `/api/dashboard/expiry-timeline` | any | Certs expiring per month (`months` param) |
| GET | `/api/dashboard/status-distribution` | any | Count per certificate status |
| GET | `/api/search` | any | Global search for the command palette (`q` param) |

**Dashboard response example**
```json
{
  "totalCerts": 12, "valid": 8, "expiring": 2, "expired": 1, "revoked": 1,
  "totalTokens": 10, "assignedTokens": 6, "pendingRequests": 3,
  "recentVerifications": [ { "file": "a.pdf", "result": "VALID", "at": "..." } ]
}
```

---

## 8. Frontend (Interactive Dashboard + Framer Motion)

**Design goal:** a modern, dark, glassmorphism SaaS-style dashboard that feels alive: animated counters, live charts, smooth page transitions, spring-based modals and drawers, a command palette, and a dramatic signature-verification reveal. Every interaction should give visual feedback.

### 8.1 Setup
```bash
npm create vite@latest frontend -- --template react
cd frontend
npm i axios react-router-dom framer-motion recharts lucide-react sonner clsx
npm i -D tailwindcss @tailwindcss/vite
```
Dev server on `5173`. Backend CORS must allow `http://localhost:5173`.

| Library | Used for |
|---|---|
| `framer-motion` | page transitions, stagger, layout animations, springs, gestures, SVG path drawing, counters |
| `recharts` | animated area, donut and bar charts |
| `lucide-react` | icons |
| `sonner` | animated toast notifications |
| `clsx` | conditional class names |

### 8.2 Folder structure
```
frontend/src/
├── main.jsx                  (BrowserRouter, AuthProvider, ThemeProvider, MotionConfig, Toaster)
├── App.jsx                   (AnimatePresence + routes)
├── api/axios.js
├── lib/motion.js             (shared variants and springs)
├── context/                  (AuthContext.jsx, ThemeContext.jsx)
├── layout/                   (AppShell, Sidebar, Topbar, CommandPalette, NotificationDrawer)
├── components/
│   ├── ui/                   (GlassCard, StatCard, AnimatedCounter, StatusBadge, DataTable,
│   │                          Modal, Drawer, FileDrop, Stepper, Skeleton, EmptyState, Button, ProtectedRoute)
│   ├── charts/               (ActivityAreaChart, StatusDonut, ExpiryBarChart)
│   └── crypto/               (CertFlipCard, VerifyResultCard, PipelineSteps, SigningProgress)
└── pages/                    (Login, Dashboard, Certificates, CertificateDetail, Tokens,
                               SignDocument, VerifyDocument, Requests, AuditLogs, Users)
```

### 8.3 Design system
- **Theme:** dark by default (slate-950 background), light mode toggle in the topbar (persisted in localStorage).
- **Accent:** indigo to cyan gradient. Status colours: VALID emerald, EXPIRING amber, EXPIRED slate, REVOKED red.
- **Glass cards:** `bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl shadow-lg`.
- **Typography:** Inter, tabular numbers for stats.
- **Background:** subtle animated gradient blobs behind the app shell (two large blurred circles that drift slowly with Framer Motion).
- **Reduced motion:** wrap the app in `<MotionConfig reducedMotion="user">` so users with that OS setting get minimal animation.

```css
/* index.css */
@import "tailwindcss";
.glass { @apply bg-white/5 backdrop-blur-xl border border-white/10 rounded-2xl shadow-lg; }
.shimmer { background: linear-gradient(90deg,#ffffff08 25%,#ffffff18 50%,#ffffff08 75%);
           background-size: 200% 100%; animation: shimmer 1.4s infinite; }
@keyframes shimmer { to { background-position: -200% 0; } }
```

### 8.4 Shared motion library `src/lib/motion.js`
```js
export const ease = [0.22, 1, 0.36, 1];
export const spring = { type: "spring", stiffness: 300, damping: 26 };

export const pageVariants = {
  initial: { opacity: 0, y: 16 },
  animate: { opacity: 1, y: 0, transition: { duration: 0.4, ease } },
  exit:    { opacity: 0, y: -12, transition: { duration: 0.2 } },
};
export const container = {
  hidden: {},
  show: { transition: { staggerChildren: 0.07, delayChildren: 0.1 } },
};
export const item = {
  hidden: { opacity: 0, y: 20 },
  show:   { opacity: 1, y: 0, transition: { duration: 0.45, ease } },
};
export const fadeScale = {
  initial: { opacity: 0, scale: 0.92 },
  animate: { opacity: 1, scale: 1, transition: spring },
  exit:    { opacity: 0, scale: 0.95, transition: { duration: 0.15 } },
};
```

### 8.5 App shell, routing and page transitions
```jsx
// main.jsx
<BrowserRouter>
  <MotionConfig reducedMotion="user">
    <ThemeProvider><AuthProvider>
      <App /><Toaster richColors position="top-right" />
    </AuthProvider></ThemeProvider>
  </MotionConfig>
</BrowserRouter>

// App.jsx
export default function App() {
  const location = useLocation();
  return (
    <AnimatePresence mode="wait">
      <Routes location={location} key={location.pathname}>
        <Route path="/login" element={<Login />} />
        <Route element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/certificates" element={<Certificates />} />
          <Route path="/certificates/:id" element={<CertificateDetail />} />
          <Route path="/tokens" element={<Tokens />} />
          <Route path="/sign" element={<SignDocument />} />
          <Route path="/verify" element={<VerifyDocument />} />
          <Route path="/requests" element={<Requests />} />
          <Route path="/audit" element={<ProtectedRoute roles={["ADMIN","AUDITOR"]}><AuditLogs /></ProtectedRoute>} />
          <Route path="/users" element={<ProtectedRoute roles={["ADMIN"]}><Users /></ProtectedRoute>} />
        </Route>
      </Routes>
    </AnimatePresence>
  );
}

// every page is wrapped like this
export default function PageTransition({ children }) {
  return <motion.div variants={pageVariants} initial="initial" animate="animate" exit="exit">{children}</motion.div>;
}
```
`AppShell` = animated gradient background + `Sidebar` + `Topbar` + `<Outlet />` inside a `PageTransition`.

### 8.6 Core components

**Sidebar (collapsible, sliding active pill)**
```jsx
const [open, setOpen] = useState(true);
<motion.aside animate={{ width: open ? 240 : 76 }} transition={spring} className="glass h-screen sticky top-0 p-3">
  {links.filter(l => l.roles.includes(user.role)).map(l => (
    <NavLink key={l.to} to={l.to} className="relative flex items-center gap-3 px-3 py-2.5 rounded-xl text-slate-300">
      {({ isActive }) => (<>
        {isActive && <motion.span layoutId="nav-pill" transition={spring}
          className="absolute inset-0 rounded-xl bg-indigo-500/20 border border-indigo-400/30" />}
        <l.icon size={20} className="relative z-10" />
        <AnimatePresence>
          {open && <motion.span initial={{opacity:0,x:-8}} animate={{opacity:1,x:0}} exit={{opacity:0}}
            className="relative z-10">{l.label}</motion.span>}
        </AnimatePresence>
      </>)}
    </NavLink>
  ))}
</motion.aside>
```

**Topbar:** search trigger (opens command palette, shortcut `Ctrl/Cmd + K`), notification bell with a pulsing badge showing the expiring-cert count (opens the NotificationDrawer), theme toggle (icon rotates on switch), user menu with avatar and role chip.

**AnimatedCounter**
```jsx
import { animate, useMotionValue, useTransform, motion } from "framer-motion";
export default function AnimatedCounter({ value }) {
  const mv = useMotionValue(0);
  const rounded = useTransform(mv, v => Math.round(v));
  useEffect(() => { const c = animate(mv, value, { duration: 1.2, ease: "easeOut" }); return c.stop; }, [value]);
  return <motion.span className="tabular-nums">{rounded}</motion.span>;
}
```

**StatCard (stagger child, lifts on hover, click filters the certificate list)**
```jsx
export default function StatCard({ icon: Icon, label, value, color, onClick }) {
  return (
    <motion.button variants={item} whileHover={{ y: -6, scale: 1.02 }} whileTap={{ scale: 0.97 }}
      onClick={onClick} className="glass p-5 text-left relative overflow-hidden group">
      <motion.div className="absolute -right-6 -top-6 w-24 h-24 rounded-full blur-2xl opacity-30"
        style={{ background: color }} animate={{ scale: [1, 1.25, 1] }} transition={{ repeat: Infinity, duration: 4 }} />
      <Icon className="mb-3" style={{ color }} />
      <div className="text-3xl font-semibold"><AnimatedCounter value={value} /></div>
      <div className="text-sm text-slate-400">{label}</div>
    </motion.button>
  );
}
```

**Modal and Drawer (spring in, blurred backdrop, click outside or Esc to close)**
```jsx
export default function Modal({ open, onClose, title, children }) {
  useEffect(() => { const h = e => e.key === "Escape" && onClose(); window.addEventListener("keydown", h); return () => window.removeEventListener("keydown", h); }, [onClose]);
  return (
    <AnimatePresence>
      {open && (
        <motion.div className="fixed inset-0 z-50 grid place-items-center bg-black/60 backdrop-blur-sm"
          initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} onClick={onClose}>
          <motion.div {...fadeScale} onClick={e => e.stopPropagation()} className="glass w-full max-w-lg p-6">
            <h3 className="text-lg font-semibold mb-4">{title}</h3>{children}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
```
`Drawer` is the same pattern but slides from the right: `initial={{ x: "100%" }} animate={{ x: 0 }} exit={{ x: "100%" }} transition={spring}`.

**DataTable:** sticky header, sortable columns (arrow rotates), animated row entry (`staggerChildren`), row hover highlight, **expandable rows** (`AnimatePresence` with `height: "auto"`), pagination with a sliding page indicator, skeleton rows while loading, `EmptyState` with an animated illustration when no data. Use `<motion.tr layout>` so sorting and filtering re-order rows smoothly.

**FileDrop:** dashed border that scales up and glows on drag-over (`animate={{ scale: dragging ? 1.03 : 1 }}`), file chip that springs in with name, size and an animated remove button.

**StatusBadge:** pill with a coloured dot; EXPIRING gets a pulsing dot (`animate={{ opacity: [1, .3, 1] }}`, infinite).

**Skeleton:** `shimmer` class blocks sized like the real content, shown while data loads.

**Toasts:** `sonner` for success and error (sign complete, cert revoked, request approved, API errors).

### 8.7 Charts (Recharts inside animated glass cards)
- **ActivityAreaChart:** signatures and verifications per day for the last 7 days (two gradient-filled areas, animated draw-in, custom glass tooltip).
- **StatusDonut:** certificate status distribution; centre label shows the total with `AnimatedCounter`; clicking a slice navigates to `/certificates?status=...`; hovered slice expands (`activeShape`).
- **ExpiryBarChart:** certificates expiring per month for the next 6 months; bars coloured amber when within 30 days.

Each chart card enters with the `item` stagger variant and has a time-range toggle (7d / 30d) with a sliding highlight using `layoutId`.

### 8.8 Crypto-specific interactive components

**CertFlipCard:** a credit-card-style certificate card. Front: subject, owner, status badge, validity bar. Back (click or hover to flip): issuer, serial, SHA-256 fingerprint with copy button, download and revoke buttons.
```jsx
export default function CertFlipCard({ cert }) {
  const [flipped, setFlipped] = useState(false);
  return (
    <div style={{ perspective: 1000 }} onClick={() => setFlipped(f => !f)} className="h-52 cursor-pointer">
      <motion.div animate={{ rotateY: flipped ? 180 : 0 }} transition={{ duration: 0.6, ease }}
        style={{ transformStyle: "preserve-3d" }} className="relative w-full h-full">
        <div className="glass absolute inset-0 p-5" style={{ backfaceVisibility: "hidden" }}>{/* front */}</div>
        <div className="glass absolute inset-0 p-5" style={{ backfaceVisibility: "hidden", transform: "rotateY(180deg)" }}>{/* back */}</div>
      </motion.div>
    </div>
  );
}
```
The front also shows a **validity progress bar** that animates from 0 to the elapsed percentage of the certificate lifetime (green, amber under 30 days, red when expired).

**SigningProgress (Sign page):** after clicking **Sign**, show a 4-step animated stepper: *Hashing file → Loading private key → Signing → Saved*. Each step shows a spinner, then an animated check. The final state reveals the SHA-256 hash with a typewriter effect and a **Download .sig** button that pops in with a spring.

**PipelineSteps (Verify page):** the verification pipeline as a vertical timeline. Steps light up one by one with a 350 ms delay each: *Chain of trust → Revocation → Validity period → Signature match*. A passed step shows a green check, a failed step shows a red cross and the remaining steps stay greyed out.

**VerifyResultCard:** large result reveal with a drawing shield.
```jsx
const CFG = {
  VALID:         { color: "#22c55e", title: "Signature Valid" },
  TAMPERED:      { color: "#ef4444", title: "Document Tampered" },
  REVOKED:       { color: "#ef4444", title: "Certificate Revoked" },
  EXPIRED:       { color: "#f59e0b", title: "Certificate Expired" },
  UNTRUSTED:     { color: "#f59e0b", title: "Untrusted Issuer" },
  NOT_YET_VALID: { color: "#f59e0b", title: "Not Yet Valid" },
  ERROR:         { color: "#94a3b8", title: "Could Not Verify" },
};
export default function VerifyResultCard({ result, reason }) {
  const { color, title } = CFG[result]; const ok = result === "VALID";
  return (
    <motion.div initial={{ opacity: 0, scale: .9 }}
      animate={ok ? { opacity: 1, scale: 1 } : { opacity: 1, scale: 1, x: [0, -10, 10, -8, 8, 0] }}
      transition={{ duration: .6 }} className="glass p-8 text-center"
      style={{ boxShadow: `0 0 60px ${color}33` }}>
      <motion.svg viewBox="0 0 100 100" width="130" className="mx-auto" fill="none" stroke={color}
        strokeWidth="4" strokeLinecap="round" strokeLinejoin="round">
        <motion.path d="M50 8 L88 22 V50 C88 74 70 90 50 94 C30 90 12 74 12 50 V22 Z"
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ duration: .9 }} />
        <motion.path d={ok ? "M32 52 L45 65 L70 36" : "M36 36 L64 64 M64 36 L36 64"}
          initial={{ pathLength: 0 }} animate={{ pathLength: 1 }} transition={{ delay: .8, duration: .5 }} />
      </motion.svg>
      <motion.h2 initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 1.2 }}
        className="text-2xl font-semibold mt-4" style={{ color }}>{title}</motion.h2>
      <motion.p initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.4 }}
        className="text-slate-400 mt-2">{reason}</motion.p>
    </motion.div>
  );
}
```
For VALID, also fire a short confetti-style burst: 16 small `motion.span` dots with random `x`, `y`, `opacity` animated outward from the shield centre.

### 8.9 Command palette (`Ctrl/Cmd + K`)
Modal with a search input and grouped results (Pages, Certificates, Tokens, Users). Arrow keys move a highlighted row (`layoutId="cmd-hl"` for a sliding highlight), Enter navigates, Esc closes. Results come from `GET /api/search?q=` (debounced 250 ms); pages are filtered locally by role.

### 8.10 Notification drawer
Bell icon opens a right-side `Drawer` listing expiring and revoked certificates and pending requests (ADMIN). Items stagger in, each has a quick action (View, Renew request). The bell badge pulses while the count is above zero.

### 8.11 Page specs

**Login:** full-screen animated gradient background with two drifting blobs; card springs in; inputs have animated focus rings; button shows a spinner while loading; wrong password shakes the card. Demo-credentials chips below the form fill the inputs on click.

**Dashboard (the showpiece):**
1. Greeting header with the user name and role chip, fading in.
2. Stat card row (staggered): Total, Valid, Expiring, Expired, Revoked, Pending requests. Cards are clickable filters.
3. Charts row: ActivityAreaChart (wide) + StatusDonut.
4. ExpiryBarChart + "Expiring soon" list with a days-left progress ring for each cert.
5. Recent verifications feed: results slide in from the right, newest first, coloured by result, auto-refresh every 30 s with an animated insert of new items (`AnimatePresence` + `layout`).
6. Quick actions (floating action button, bottom right): *Sign a document*, *Verify a document*, *New request*. It expands into a fan of buttons with staggered springs.

**Certificates:** toggle between **Card view** (grid of `CertFlipCard`) and **Table view** (DataTable) with an animated sliding toggle. Filter chips (All / Valid / Expiring / Expired / Revoked) with a sliding underline. Search box with debounce. ADMIN actions: *Issue Test Certificate* (Modal with a 3-step form: user, details, review), *Upload Certificate*, *Revoke* (confirm Modal with a reason field).

**Certificate detail:** hero card with a large validity ring, tabbed sections (Overview, Signatures made, Audit trail) with a sliding tab indicator, fingerprint copy button that morphs into a check mark on copy.

**Sign Document:** left column: certificate selector (cards, select one with a highlight ring), `FileDrop`. Right column: live preview panel showing file name, size and SHA-256 once the file is chosen. **Sign** button triggers `SigningProgress`. History table below.

**Verify Document:** three inputs (file, `.sig`, certificate) shown as a 3-step wizard with a progress bar. After **Verify**, show `PipelineSteps` followed by `VerifyResultCard`. A **Try tamper demo** button appends one byte to the uploaded file in the browser and re-runs verification to show TAMPERED.

**Tokens:** card grid of tokens with a status ring, drag a token card onto a user avatar in a side panel to assign it (Framer Motion `drag` with `dragSnapToOrigin` and a drop-zone highlight; also provide a dropdown for accessibility).

**Requests:** Kanban board with columns *Pending → Approved → Completed / Rejected*. ADMIN can drag cards between columns (call `PUT /requests/{id}/status` on drop; illegal moves snap back with a shake and an error toast). USER sees own requests in the same board, read-only, plus a **New request** modal.

**Audit Logs:** timeline-style table; rows stagger in, action icons by type, filter bar with action dropdown and date range, expandable row for details, "Live" toggle that polls every 10 s and animates new rows in at the top.

**Users:** DataTable with role chips, active toggle switch (animated thumb), create-user modal.

### 8.12 API layer, auth and route guard
```js
// api/axios.js
import axios from "axios";
const api = axios.create({ baseURL: "http://localhost:8080/api" });
api.interceptors.request.use(cfg => {
  const t = localStorage.getItem("kb_token");
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});
api.interceptors.response.use(r => r, err => {
  if (err.response?.status === 401) { localStorage.clear(); window.location = "/login"; }
  return Promise.reject(err);
});
export default api;
```
```jsx
// context/AuthContext.jsx
export const AuthContext = createContext();
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem("kb_user") || "null"));
  const login = async (email, password) => {
    const { data } = await api.post("/auth/login", { email, password });
    localStorage.setItem("kb_token", data.token);
    localStorage.setItem("kb_user", JSON.stringify(data.user));
    setUser(data.user);
  };
  const logout = () => { localStorage.clear(); setUser(null); };
  return <AuthContext.Provider value={{ user, login, logout }}>{children}</AuthContext.Provider>;
}

// components/ui/ProtectedRoute.jsx
export default function ProtectedRoute({ roles, children }) {
  const { user } = useContext(AuthContext);
  if (!user) return <Navigate to="/login" />;
  if (roles && !roles.includes(user.role)) return <Navigate to="/" />;
  return children;
}
```

### 8.13 File upload and download snippets
```js
// sign
const fd = new FormData();
fd.append("file", file); fd.append("certId", certId);
const { data } = await api.post("/sign", fd);

// download .sig
const blob = new Blob([data.signatureB64], { type: "text/plain" });
const a = document.createElement("a");
a.href = URL.createObjectURL(blob);
a.download = file.name + ".sig";
a.click();
```

### 8.14 Backend additions required by the interactive UI
Add these to the backend (small, read-only endpoints):
- `GET /api/dashboard/activity?days=7` returns `[{ "date": "2026-10-10", "signs": 4, "verifications": 7 }]`
- `GET /api/dashboard/expiry-timeline?months=6` returns `[{ "month": "2026-11", "count": 2 }]`
- `GET /api/dashboard/status-distribution` returns `{ "VALID": 8, "EXPIRING": 2, "EXPIRED": 1, "REVOKED": 1 }`
- `GET /api/search?q=` returns `{ "certificates": [...], "tokens": [...], "users": [...] }` (users only for ADMIN; limit 5 per group; USER role sees only own items)
- `POST /api/verify` response must also include `steps`: `[{ "name": "CHAIN", "status": "PASSED|FAILED|SKIPPED" }, { "name": "REVOCATION", ... }, { "name": "VALIDITY", ... }, { "name": "SIGNATURE", ... }]`. In `VerificationService`, record each step as the pipeline runs; steps after the first failure are `SKIPPED`.

### 8.15 Performance and quality rules
- Animate only `transform` and `opacity` where possible; avoid animating `width`/`height` on large lists (the sidebar width and `height: "auto"` expansions are the exceptions).
- Lazy-load pages with `React.lazy` and a `Suspense` fallback of skeletons.
- Cap stagger delays so a 50-row table never takes more than about 0.5 s to appear (stagger only the first 10 rows).
- All interactive elements need hover, focus-visible and pressed states, and keyboard access.
- Everything must work on mobile widths: the sidebar becomes a bottom sheet / hamburger drawer under 768 px.

---

## 9. End-to-End Flows

**Flow 1: Issue and use a certificate**
1. Admin logs in → Certificates → Issue Test Certificate for a user.
2. Backend generates a key pair, the root CA signs the cert, a `.p12` is saved, metadata is stored.
3. User logs in → Sign → picks the cert, uploads a PDF → gets `.sig`.
4. Anyone opens Verify, uploads the PDF, `.sig` and cert → **VALID**.

**Flow 2: Tamper detection**
Edit the PDF and verify again → **TAMPERED**.

**Flow 3: Expiry**
Issue a cert with `validDays=20` → run `/certs/refresh-status` → status becomes **EXPIRING** and shows on the dashboard.

**Flow 4: Revocation**
Admin revokes a cert → signing is blocked; verifying an older signature returns **REVOKED**.

**Flow 5: Service request**
User raises RENEW_CERT → Admin approves → Admin completes → new cert issued, old cert marked EXPIRED.

---

## 10. Validation and Security Checklist

- [ ] BCrypt for passwords, minimum 8 characters
- [ ] JWT secret from environment variable, not hard-coded in production
- [ ] Private keys only in `.p12` files, never in DB or API responses
- [ ] `keystore-data/` in `.gitignore`
- [ ] File size limit 10 MB, validate that the uploaded cert parses
- [ ] USER cannot access another user's certs, tokens or requests (check owner on every read)
- [ ] All admin endpoints use `@PreAuthorize`
- [ ] Never log passwords, keystore passwords or private key bytes
- [ ] CORS limited to the frontend origin

---

## 11. Testing Plan

**Unit tests (JUnit 5)**
- `CryptoServiceTest`: sign then verify returns true; modified data returns false
- `CertificateParserServiceTest`: metadata extracted correctly from a generated cert
- `TestCaServiceTest`: issued cert verifies against the root; expired cert detected
- `VerificationServiceTest`: one test per result type (VALID, TAMPERED, EXPIRED, REVOKED, UNTRUSTED)
- `ExpiryMonitorServiceTest`: statuses set correctly around the 30-day boundary
- Request state machine: all valid and invalid transitions

**Integration tests:** login, issue cert, sign, verify (MockMvc + H2 or Testcontainers).

**Manual demo script:** Flows 1 to 5 in Section 9.

---

## 12. Seed Data (CommandLineRunner)

- Admin: `admin@keybridge.com` / `Admin@123`
- User: `user@keybridge.com` / `User@123`
- Auditor: `auditor@keybridge.com` / `Audit@123`
- 5 tokens (`TKN-001` to `TKN-005`)
- 3 test certs for the user with validity of 200, 20 and 400 days

---

## 13. Run Instructions

```bash
# DB
mysql -u root -p -e "CREATE DATABASE keybridge;"

# Backend
cd backend
mvn spring-boot:run          # http://localhost:8080

# Frontend
cd frontend
npm install
npm run dev                  # http://localhost:5173
```

---

## 14. Build Phases and Prompts for the Antigravity Agent

Give these to the agent **one at a time**. Review and run after each phase before moving on.

**Phase 1: Backend foundation**
> Read SPEC.md. Create the Spring Boot project in `/backend` with the dependencies in 5.1, `application.yml` from 5.2, enums, entities and repositories for all tables in Section 4. Add the global exception handler (6.10). Make sure it starts and connects to MySQL.

**Phase 2: Auth and security**
> Implement JwtService, JwtAuthFilter, SecurityConfig, UserDetailsServiceImpl, AuthController (`/api/auth/login`, `/api/auth/me`), the admin/user/auditor seed runner, and User management endpoints per Sections 6.9, 7 and 12. Add tests for login.

**Phase 3: Certificates and test CA**
> Implement CryptoService, CertificateParserService, TestCaService (6.1 to 6.3), CertificateService and CertificateController: upload, issue, list, detail, download, revoke. Write unit tests for the crypto and parser classes.

**Phase 4: Tokens and expiry**
> Implement Token CRUD and assignment, ExpiryMonitorService with `@EnableScheduling` and the manual refresh endpoint (6.6), and the Dashboard endpoint.

**Phase 5: Sign and verify**
> Implement SigningService and VerificationService exactly as in 6.4 and 6.5, with controllers `/api/sign`, `/api/verify`, `/api/signed-documents`, `/api/verifications`. Write one unit test per VerifyResult value.

**Phase 6: Requests and audit**
> Implement ServiceRequestService with the state machine and completion actions (6.7), the `@Audited` annotation and AuditAspect (6.8), AuditController with pagination and filters.

**Phase 7: Frontend foundation**
> Create the Vite React app in `/frontend` per Sections 8.1 to 8.6 and 8.12. Install framer-motion, recharts, lucide-react, sonner. Build `lib/motion.js`, the glass design system, ThemeContext (dark/light), AuthContext, ProtectedRoute, AppShell with the collapsible Sidebar (layoutId active pill), Topbar, animated page transitions with AnimatePresence, and the animated Login page. Add the backend endpoints from 8.14 first.

**Phase 8: Frontend pages**
> Build the Dashboard (stat cards with AnimatedCounter, ActivityAreaChart, StatusDonut, ExpiryBarChart, live verifications feed, floating action button), Certificates (card/table toggle with CertFlipCard), CertificateDetail, Tokens (drag to assign), Users, CommandPalette and NotificationDrawer per 8.7 to 8.11, using the real APIs. Include Skeleton loading states and the reusable ui components.

**Phase 9: Sign, Verify, Requests, Audit UI**
> Build SignDocument (SigningProgress stepper, hash typewriter, .sig download), VerifyDocument (3-step wizard, PipelineSteps, VerifyResultCard with the drawing shield, tamper demo button), Requests (drag-and-drop Kanban board), and AuditLogs (live timeline table) per 8.8 and 8.11.

**Phase 10: Polish and testing**
> Run the full test suite, fix failures, add loading and error states to every page, check animation performance and reduced-motion behaviour (8.15), test mobile widths, and walk through the five flows in Section 9. Write a README with setup steps and screenshot placeholders.

---

## 15. Future Scope (mention in report, do not build)

- PKCS#11 hardware token integration
- Certificate Revocation Lists (CRL) and OCSP
- PDF-embedded signatures (PAdES) and timestamping (RFC 3161)
- Integration with licensed Certifying Authorities and government portals
- Email notifications before expiry
- Multi-factor authentication

---

## 16. Rules for the AI Agent

1. Follow this spec; if something is ambiguous, choose the simplest option and leave a `// NOTE:` comment.
2. Do not store private keys in the database or return them in any API response.
3. Use DTOs for all API input and output, never raw entities.
4. Keep each service focused on one responsibility; controllers stay thin.
5. Write or update tests with every phase and run them before finishing the phase.
6. Do not add features outside this spec.
7. Every page must use `PageTransition`, staggered entry for lists and cards, and skeleton loaders; use only the shared variants from `lib/motion.js` so motion stays consistent.
8. Respect `prefers-reduced-motion` through `MotionConfig reducedMotion="user"`.
