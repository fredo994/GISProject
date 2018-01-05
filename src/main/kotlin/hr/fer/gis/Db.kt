package hr.fer.gis

import com.mongodb.MongoClient
import org.litote.kmongo.* //NEEDED! import KMongo extensions

object Db {
    private val client: MongoClient = KMongo.createClient()

    fun getDb() = client.getDatabase("gis-project-db")
}