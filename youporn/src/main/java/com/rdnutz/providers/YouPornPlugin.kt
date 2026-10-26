package com.rdnutz.providers

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class YouPornPlugin : Plugin() {
    override fun load() {
        registerMainAPI(YouPornProvider())
    }
}