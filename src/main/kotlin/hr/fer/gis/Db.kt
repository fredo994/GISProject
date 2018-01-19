package hr.fer.gis

import com.mongodb.MongoClient
import com.mongodb.MongoClientURI
import org.litote.kmongo.* //NEEDED! import KMongo extensions
import java.util.logging.Logger

object Db {
    private val log =  Logger.getLogger("DB")!!
    private val client: MongoClient
    private val db: String

    init {
        val connectionString = System.getenv(MONGO_DB_ENV_VAR)
        if (connectionString != null) {
            log.info("Using heroku mongo with connection string $connectionString")
            client = KMongo.createClient(MongoClientURI(connectionString))
            db = HEROKU_DB
        } else {
            log.info("Using local mongo.")
            client = KMongo.createClient()
            db = DEFAULT_DB
        }
    }

    fun getDb() = client.getDatabase(db)
}