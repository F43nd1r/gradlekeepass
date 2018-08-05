package com.faendir.gradlekeepass

import groovy.json.JsonOutput
/**
 * @author lukas
 * @since 05.08.18
 */
class KeepassRequest {
    String RequestType
    String Nonce
    String Verifier
    String Key
    String Url
    String Id

    String toJson() {
        return JsonOutput.toJson(this)
        /*Map<?, ?> properties = DefaultGroovyMethods.getProperties(this)
        properties.remove("class")
        properties.remove("declaringClass")
        properties.remove("metaClass")
        while (properties.values().remove(null)){
        }
        return JsonOutput.toJson(properties)*/
    }

    static KeepassRequest create(String requestType) {
        KeepassRequest request = new KeepassRequest()
        request.RequestType = requestType
        return request
    }
}
