# gradlekeepass
Gradle Plugin which uses [KeepassHttp](https://github.com/pfn/keepasshttp/) to get logins from [Keepass](https://keepass.info/) 

# Usage example
```groovy
signingConfigs {
    release {
        def login = keepass.getLogin('intellij.android.key')
        storePassword login.Password
        keyAlias login.Login
        keyPassword login.Password
    }
}
```

# Configuration
```groovy
keepass {
    host = 'http://localhost'
    port = 19455
    configFilePath = null //internally defaults to ${project.getGradle().getGradleUserHomeDir()}/keepass.gradle
}
