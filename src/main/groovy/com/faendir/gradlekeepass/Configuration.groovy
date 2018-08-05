package com.faendir.gradlekeepass

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import javax.crypto.spec.SecretKeySpec

/**
 * @author lukas
 * @since 05.08.18
 */
class Configuration {
    String id
    String key

    SecretKeySpec loadKey() {
        return key == null ? null : new SecretKeySpec(Base64.decoder.decode(key), 'AES')
    }

    void writeToFile(File file) {
        file.write(JsonOutput.toJson(this))
    }

    static Configuration readFromFile(File file) {
        return new Configuration((Map)new JsonSlurper().parse(file))
    }
}
