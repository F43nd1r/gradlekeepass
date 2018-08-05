package com.faendir.gradlekeepass

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
/**
 * @author lukas
 * @since 05.08.18
 */
class Keepass {
    static final Logger logger = LoggerFactory.getLogger(Keepass)
    private final SecureRandom random
    private final Cipher cipher
    private final KeyGenerator keyGenerator
    private final OkHttpClient client
    private final String url
    private final File configFile
    private boolean associated

    Keepass(String url, File configFile) {
        this.url = url
        this.configFile = configFile
        this.client = new OkHttpClient.Builder().readTimeout(0, TimeUnit.SECONDS).build()
        this.keyGenerator = KeyGenerator.getInstance('AES')
        this.keyGenerator.init(256)
        this.cipher = Cipher.getInstance('AES/CBC/PKCS5Padding')
        this.random = new SecureRandom()
    }


    private Response post(KeepassRequest request) {
        return client.newCall(new Request.Builder().url(url).post(RequestBody.create(null, request.toJson())).build()).execute()
    }

    boolean isAssociated() {
        if (!associated) {
            KeepassRequest request = KeepassRequest.create('test-associate')
            Configuration configuration = setVerifier(request)
            if (configuration != null) {
                Response response = post(request)
                if (response.successful) {
                    verifyResponse(configuration, KeepassResponse.fromJson(response.body().string()))
                    logger.debug('Confirmed association with keepass')
                } else {
                    logger.warn('Failed to test association with keepass')
                }
            }
        } else {
            logger.debug('Already confirmed association with keepass, not checking again')
        }
        return associated
    }

    boolean associate() {
        SecretKey key = keyGenerator.generateKey()
        KeepassRequest request = KeepassRequest.create('associate')
        request.Key = Base64.encoder.encodeToString(key.getEncoded())
        Configuration configuration = setVerifier(request, key)
        Response response = post(request)
        if (response.successful) {
            KeepassResponse json = KeepassResponse.fromJson(response.body().string())
            if (verifyResponse(configuration, json)) {
                configuration.id = json.Id
                configuration.writeToFile(configFile)
                associated = true
                logger.debug('Associated with keepass')
            }
        } else {
            logger.warn('Failed to associate with keepass')
        }
        return associated
    }

    List<KeepassEntry> getLogins(String name) {
        if (associated) {
            KeepassRequest request = KeepassRequest.create('get-logins')
            Configuration configuration = setVerifier(request)
            cipher.init(Cipher.ENCRYPT_MODE, configuration.loadKey(), new IvParameterSpec(Base64.decoder.decode(request.Nonce)))
            request.Url = Base64.encoder.encodeToString(cipher.doFinal(name.getBytes()))
            Response response = post(request)
            if (response.successful) {
                KeepassResponse json = KeepassResponse.fromJson(response.body().string())
                if (verifyResponse(configuration, json)) {
                    List<KeepassEntry> entries = new ArrayList<>();
                    for(KeepassEntry entry : json.Entries) {
                        cipher.init(Cipher.DECRYPT_MODE, configuration.loadKey(), new IvParameterSpec(Base64.decoder.decode((String) json.Nonce)))
                        KeepassEntry decryptedEntry = new KeepassEntry()
                        decryptedEntry.Login = new String(cipher.doFinal(Base64.decoder.decode(entry.Login)))
                        decryptedEntry.Password = new String(cipher.doFinal(Base64.decoder.decode(entry.Password)))
                        decryptedEntry.Uuid = new String(cipher.doFinal(Base64.decoder.decode(entry.Uuid)))
                        decryptedEntry.Name = new String(cipher.doFinal(Base64.decoder.decode(entry.Name)))
                        entries.add(decryptedEntry)
                    }
                    return entries
                }
            } else {
                logger.warn("Failed to get logins for $name")
            }
        } else {
            logger.warn("No keepass association, cannot get logins for $name")
        }
        return Collections.emptyList()
    }

    private Configuration setVerifier(KeepassRequest request) {
        return setVerifier(request, null)
    }

    private Configuration setVerifier(KeepassRequest request, SecretKey inKey) {
        Configuration config
        if (inKey != null) {
            config = new Configuration()
            config.key = Base64.encoder.encodeToString(inKey.getEncoded())
        } else {
            if (configFile.exists()) {
                config = Configuration.readFromFile(configFile)
            } else {
                logger.debug("No keepass connection configured")
                return null
            }
        }
        if (config.id != null) {
            request.Id = config.id
        }
        byte[] iv = new byte[cipher.getBlockSize()]
        this.random.nextBytes(iv)
        request.Nonce = Base64.encoder.encodeToString(iv)
        cipher.init(Cipher.ENCRYPT_MODE, config.loadKey(), new IvParameterSpec(iv))
        request.Verifier = Base64.encoder.encodeToString(cipher.doFinal(request.Nonce.getBytes()))
        return config
    }

    private boolean verifyResponse(Configuration config, KeepassResponse response) {
        if (response.Success) {
            byte[] iv = Base64.decoder.decode((String) response.Nonce)
            byte[] crypted = Base64.decoder.decode((String) response.Verifier)
            cipher.init(Cipher.DECRYPT_MODE, config.loadKey(), new IvParameterSpec(iv))
            this.associated = new String(cipher.doFinal(crypted)) == response.Nonce
            if (config.id != null) {
                this.associated &= config.id == response.Id
            }
            return associated
        }
        logger.warn("keepasshttp was unable to complete a request")
        return false
    }
}
