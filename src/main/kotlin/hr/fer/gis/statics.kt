@file:JvmName("Constants")
package hr.fer.gis

import java.util.concurrent.TimeUnit

const val HEROKU_DB = "heroku_50j17k4m"
const val DEFAULT_DB = "gis-project-db"
const val MONGO_DB_ENV_VAR = "MONGODB_URI"
const val SLAP_COLLECTION = "slap"
const val DSLAM_COLLECTION = "DSLAM"
const val HTTP_PORT = "PORT"
val THREE_MINUTES = TimeUnit.MINUTES.toMillis(3)