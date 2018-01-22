# GISProject API 

You need to have MongoDb (version > 3.0) on your local matching.

Also you need to have maven, java & kotlin installed.

Api endpoints:

1. 
    - path: /hits
    - method: POST
    - description: Gets list of Lightning strikes.
    - request:
        ```      
        {
            "longitude" : Double,
            "latitude"  : Double,
            "radius"    : Double|Int (meters)
            "from" : Long (milliseconds),
            "to" : Long (milliseconds) 
        } 
        ```
    - response:
        ```
        {
            "longitude" : Double,
            "latitude"  : Double,
            "timestamp" : Long (milliseconds),
            "type"      : String (CLOUD_CLOUD|CLOUD_EARTH)
        }
        ```
2.
    - path: /check
    - method: POST
    - description: Checks if lightning strike occurred in last two hours
    - request:
        ```
        {
            "longitude" : Double,
            "latitude"  : Double,
            "radius"    : Double|Int (meters)
        }
        ```
    - response:
        ```
        {
            "hit": Boolean
        }
        ```
3. 
    - path: /dslam
    - method: POST
    - description: Gets info about DSLAM stations
    - request: 
        ```
        {
            "longitude" : Double,
            "latitude"  : Double, 
            "radius"    : Double|Int (meters)
        }
        ```
    - response:
        ```
        {
            "name"          : String,
            "longitude"     : Double,
            "latitude"      : Double,
            "numberOfUsers" : Int 
        }
        ```
4.
    - path: /strike
    - method: POST
    - description: Records lightning strike at current time.
    - request:
        ```
        {
            "longitude" : Double,
            "latitude" : Double,
            "type" : "CLOUD_EARTH"|"CLOUD_CLOUD",
            "amperage" : Double,
            "height" : Int,
            "locationError" : Int
        }
        ```
    - response: 
        ```
        {
            {
                "timestamp": Long,
                "type": "CLOUD_EARTH"|"CLOUD_CLOUD",
                "amperage": Double,
                "height": Int,
                "locationError": Int,
                "location": {
                    "type": "Point",
                    "coordinates": [Double, Double] # long, lat
                }
            }
        }
        ```