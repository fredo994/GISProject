package hr.fer.gis

import org.joda.time.format.DateTimeFormat


//2017.07.23 00:20:58.998
private val DTF = DateTimeFormat.forPattern("yyy.MM.dd HH:mm:ss.SSS")

data class Location(val type: String = "Point", val coordinates: DoubleArray) {
    constructor(long: Double, lat: Double) : this(coordinates = doubleArrayOf(long, lat))
    fun longitude() = coordinates[0]
    fun latitude() = coordinates[1]
}

enum class LightningStrike {
    CLOUD_EARTH,
    CLOUD_CLOUD
}

data class SLAP(
        val timestamp: Long,
        val type: LightningStrike,
        val amperage: Double,
        val height: Int,
        val locationError: Int,
        val location: Location
)

data class DSLAM(
        val name: String,
        val userCount: Int,
        val location: Location
)

fun String.toDateTime() = DTF.parseDateTime(this)

fun String.safeToDouble() = this.replace(',', '.', true).toDouble()

object CsvReader {

    fun readCSV(fileName: String) = CsvReader.javaClass.classLoader.getResource(fileName)
            .readText()
            .lines()
            .filter { !it.isEmpty() }
            .filter { !it.startsWith("--") }

    fun readSLAP(fileName: String) = readCSV(fileName).drop(1)
            .filter { !it.startsWith("vrijeme") }
            .map { it.split("|") }
            .map {
                SLAP(
                        it[0].toDateTime().millis,
                        when (it[1].toInt()) {
                            1 -> LightningStrike.CLOUD_EARTH
                            2 -> LightningStrike.CLOUD_CLOUD
                            else -> throw RuntimeException()
                        },
                        it[2].safeToDouble(),
                        it[3].toInt(),
                        it[4].toInt(),
                        Location(
                                it[5].safeToDouble(),
                                it[6].safeToDouble()
                        )
                )
            }


    fun readDSLAM(fileName: String) = readCSV(fileName).drop(1)
            .map { it.split(Regex("\\s+")) }
            .map {
                DSLAM(
                        it[0],
                        it[1].toInt(),
                        Location(
                                it[2].safeToDouble(),
                                it[3].safeToDouble()
                        )
                )
            }.toList()


}