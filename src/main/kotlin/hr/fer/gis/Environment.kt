package hr.fer.gis

object Environment {

    fun getPort(): Int {
        val port = System.getenv("PORT")
        return port?.toInt() ?: 8080
    }

    fun getMongoConnoection(): String {
        return System.getenv(MONGO_DB_ENV_VAR) ?: "0.0.0.0:27197"
    }
}