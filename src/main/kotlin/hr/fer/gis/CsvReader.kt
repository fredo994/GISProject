package hr.fer.gis

import hr.fer.gis.CsvReader.readCSV
import org.joda.time.format.DateTimeFormat
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


//2017.07.23 00:20:58.998
private val DTF = DateTimeFormat.forPattern("yyy.MM.dd HH:mm:ss.SSS")

data class Location(val type: String = "Point", val coordinates: DoubleArray) {
    constructor(long: Double, lat: Double) : this(coordinates = doubleArrayOf(long, lat))
}

data class SLAP(
        val vrijeme_utc: Date,
        val tip_id: Int,
        val struja: Double,
        val visina: Int,
        val greska: Int,
        val location: Location
)

data class DSLAM(
        val naziv: String,
        val brojKorisnika: Int,
        val location: Location
)

fun String.toDate() = DTF.parseDateTime(this).toDate()

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
                        it[0].toDate(),
                        it[1].toInt(),
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
                println(it)
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