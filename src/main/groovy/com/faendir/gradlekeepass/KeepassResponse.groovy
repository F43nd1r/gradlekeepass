package com.faendir.gradlekeepass

import groovy.json.JsonSlurper

/**
 * @author lukas
 * @since 05.08.18
 */
class KeepassResponse {
    String RequestType
    String Error
    boolean Success
    String Id
    int Count
    String Version
    String Hash
    List<KeepassEntry> Entries
    String Nonce
    String Verifier

    static KeepassResponse fromJson(String json) {
        return new KeepassResponse((Map) new JsonSlurper().parseText(json))
    }
}
