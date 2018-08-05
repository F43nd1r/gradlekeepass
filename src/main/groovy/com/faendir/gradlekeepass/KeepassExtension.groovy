package com.faendir.gradlekeepass

import org.gradle.api.Project
/**
 * @author lukas
 * @since 05.08.18
 */
class KeepassExtension {
    int port = 19455
    String host = "http://localhost"
    String configFilePath
    private Keepass keepass
    private final Project project

    KeepassExtension(Project project) {
        this.project = project
    }

    Keepass getKeypass() {
        if (keepass == null) {
            keepass = new Keepass("$host:$port", getConfigFile())
        }
        return keepass
    }

    KeepassEntry getLogin(String name) {
        Keepass keepass = getKeypass()
        if (!keepass.isAssociated()) {
            keepass.associate()
        }
        List<KeepassEntry> logins = keepass.getLogins(name)
        if (logins.size() > 0) {
            if (logins.size() > 1) {
                Keepass.logger.info("Got multiple keepass entries for $name. Using ${logins[0].Name}")
            }
            return logins[0]
        }
        Keepass.logger.warn("Got no keepass entries for $name")
        return new KeepassEntry()
    }

    private File getConfigFile() {
        return new File(configFilePath != null ? configFilePath : "${project.getGradle().getGradleUserHomeDir()}/keepass.gradle")
    }
}
