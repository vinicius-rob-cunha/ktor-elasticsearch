rootProject.name = "ktor-elasticsearch"

pluginManagement {

    val artifactory_repository: String by settings
    val artifactory_user: String by settings
    val artifactory_password: String by settings

    repositories {
        mavenLocal()
        maven {
            url = java.net.URI("$artifactory_repository")
            credentials {
                username = "$artifactory_user"
                password = "$artifactory_password"
            }
        }
        maven("https://repo.spring.io/milestone")
        maven("https://repo.spring.io/snapshot")
    }
}