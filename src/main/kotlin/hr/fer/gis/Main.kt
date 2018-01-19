@file:JvmName("Main")
package hr.fer.gis

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.Iterables
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Projections
import com.mongodb.client.model.geojson.Point
import com.mongodb.client.model.geojson.Position
import org.litote.kmongo.createIndex
import org.litote.kmongo.getCollection
import org.litote.kmongo.toList
import spark.kotlin.RouteHandler
import spark.kotlin.ignite
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import java.util.stream.Stream

private val log = Logger.getLogger("API")!!
private val dslamColl = Db.getDb().getCollection<DSLAM>(DSLAM_COLLECTION)
private val slapColl = Db.getDb().getCollection<SLAP>(SLAP_COLLECTION)
private val latRange = (-90.0).rangeTo(90.0)
private val longRange = (-180.0).rangeTo(180.0)

fun bootstrapNeeded(): Boolean {
    return Db.getDb().getCollection(DSLAM_COLLECTION).count() == 0L || Db.getDb().getCollection(SLAP_COLLECTION).count() == 0L
}

fun <T> toIterable(stream: Stream<T>): Iterable<T> {
    return object : Iterable<T> {
        override fun iterator(): Iterator<T> = stream.iterator()

    }
}

fun bootstrap() {
    dslamColl.drop()
    dslamColl.createIndex(keys = """{location:"2dsphere"}""")
    log.info("Reading DSLAM data")

    log.info("Inserting DSLAM data into mongo")
    Iterables.partition(toIterable(CsvReader.readDSLAM("Projekt 5 - DSLAM lokacije.txt")
            .filter { it.location.latitude() in latRange }
            .filter { it.location.longitude() in longRange }), 100
    ).forEach { dslamColl.insertMany(it) }


    slapColl.drop()
    slapColl.createIndex(keys = """{timestamp:1}""")
    slapColl.createIndex(keys = """{location:"2dsphere"}""")
    slapColl.createIndex(keys = """{timestamp:1, location:"2dsphere"}""")
    log.info("Reading SLAP data")
    Iterables.partition(toIterable(CsvReader.readSLAP("SLAP-export-2017.07.24.txt")
            .filter { it.location.latitude() in latRange }
            .filter { it.location.longitude() in longRange }), 100)
            .forEach { slapColl.insertMany(it) }
}

fun now() = System.currentTimeMillis()

data class LightningCheckReq(val longitude: Double, val latitude: Double, val radius: Double)
data class LightningCheckResp(val hit: Boolean)

fun checkIfLightningExists(checkReq: LightningCheckReq): LightningCheckResp {
    val exists = slapColl.find(
            and(
                    gte("timestamp", now() - THREE_MINUTES),
                    near(
                            "location",
                            Point(Position(checkReq.longitude, checkReq.latitude)),
                            checkReq.radius,
                            0.0
                    )
            )
    ).projection(Projections.include("_id"))
            .limit(1)
            .any()
    return LightningCheckResp(exists)
}

data class LightningStrikesReq(val longitude: Double, val latitude: Double, val radius: Double, val timestamp: Long)
data class LightningStrikeRespEntry(val longitude: Double, val latitude: Double, val timestamp: Long, val type: LightningStrike)

fun getLightningStrikes(req: LightningStrikesReq): List<LightningStrikeRespEntry> {
    return slapColl.find(
            and(
                    gte("timestamp", req.timestamp),
                    lt("timestamp", req.timestamp + TimeUnit.MINUTES.toMillis(10)),
                    near(
                            "location",
                            Point(Position(req.longitude, req.latitude)),
                            req.radius,
                            0.0
                    )
            ))
            .map { LightningStrikeRespEntry(it.location.longitude(), it.location.latitude(), it.timestamp, it.type) }
            .toList()
}

data class DSLAMStationsReq(val longitude: Double, val latitude: Double, val radius: Double)
data class DSLAMStationRespEntry(val name: String, val longitude: Double, val latitude: Double, val numberOfUsers: Int)

fun getDSLAMStations(req: DSLAMStationsReq): List<DSLAMStationRespEntry> {
    return dslamColl.find(
            near(
                    "location",
                    Point(Position(req.longitude, req.latitude)),
                    req.radius,
                    0.0
            )
    ).map {
        DSLAMStationRespEntry(it.name, it.location.longitude(), it.location.latitude(), it.userCount)
    }.toList()
}

inline fun <reified T : Any> RouteHandler.bodyRequest(mapper: ObjectMapper): T = mapper.readValue(request.body())
fun RouteHandler.json(mapper: ObjectMapper, value: Any): String {
    response.header("Content-Type", "application/json")
    return mapper.writeValueAsString(value)
}

fun <O> time(name: String = "", func: () -> O): O {
    val s = now()
    val result = func()
    val e = now()
    log.info("Duration of $name: ${e - s} ms")
    return result
}

fun getPort() = System.getenv(HTTP_PORT)?.toInt() ?: 8080

fun main(args: Array<String>) {
    if (bootstrapNeeded()) {
        with(Thread { bootstrap() }) {
            start()
        }
    }
    val port = getPort()
    log.info("Got port $port")

    val server = ignite()
    server.port(port)
    val mapper = jacksonObjectMapper()
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    with(server) {
        post(path = "/check", accepts = "application/json") { json(mapper, time("check") { checkIfLightningExists(bodyRequest(mapper)) })  }
        post(path = "/hits", accepts = "application/json")  { json(mapper, time("hits") { getLightningStrikes(bodyRequest(mapper)) })  }
        post(path = "/dslam", accepts = "application/json") { json(mapper, time("dslam") { getDSLAMStations(bodyRequest(mapper)) })  }
        get(path = "/") { "Hello World" }
    }
}