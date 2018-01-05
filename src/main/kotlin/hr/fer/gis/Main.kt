package hr.fer.gis

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.mongodb.client.model.Filters
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import org.litote.kmongo.* //NEEDED! import KMongo extensions
import spark.kotlin.ignite
import java.util.logging.Logger

val log = Logger.getLogger("API")
val BOOTSTRAP = false
val dslamColl = Db.getDb().getCollection<DSLAM>(DSLAM_COLLECTION)
val slapColl = Db.getDb().getCollection<SLAP>(SLAP_COLLECTION)

fun bootstrapNeeded():Boolean {
    return BOOTSTRAP &&
            Db.getDb().getCollection(DSLAM_COLLECTION).count() != 0L && Db.getDb().getCollection(SLAP_COLLECTION).count() != 0L
 }

fun bootstrap() {
    dslamColl.drop()
    dslamColl.createIndex(keys = """{location:"2dsphere"}""")
    log.info("Reading DSLAM data")

    val dslams = CsvReader.readDSLAM(
            CsvReader.javaClass.getResource("Projekt 5 - DSLAM lokacije.txt").toExternalForm().toString()
    );
    log.info("Inserting DSLAM data into mongo")
    dslamColl.insertMany(dslams)

    slapColl.drop()
    slapColl.createIndex(keys = """{location:"2dsphere"}""")
    log.info("Reading SLAP data")
    val slaps = CsvReader.readSLAP(
            CsvReader.javaClass.getResource("SLAP-export-2017.07.24.txt").toExternalForm().toString()
    )
    log.info("Storing SLAP data in mongo")
    slapColl.insertMany(slaps)
}

fun find_slap_radius(radius:Double, location: Location): String {
    val query = Filters.near(
            "location",
            Point(Position(location.coordinates[0], location.coordinates[1])),
            radius,
            0.0
    )
    log.info(query.toString())
    val nearResult= slapColl.find(query).toList()
    return jacksonObjectMapper().writeValueAsString(nearResult)
}

fun main(args: Array<String>) {
    if (bootstrapNeeded()){
        bootstrap()
    }
    val server = ignite()
    server.port(8080)
    server.get("hello-world") {
        jacksonObjectMapper().writeValueAsString(dslamColl.find().take(100).toList())
    }
    server.get("slap/:long/:lat/:radius") {
        val long = request.params("long").toDouble()
        val lat = request.params("lat").toDouble()
        val radius = request.params("radius").toDouble()
        find_slap_radius(radius, Location(long, lat))
    }
    server.get("slap/:id") {
        request.params(":id")
    }
}