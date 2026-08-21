package com.keeply.app

import android.app.Application
import com.keeply.app.data.ThingRepository
import com.keeply.app.data.local.KeeplyDatabase

class KeeplyApplication : Application() {
    val database: KeeplyDatabase by lazy { KeeplyDatabase.create(this) }
    val thingRepository: ThingRepository by lazy { ThingRepository(database.thingDao()) }
}
