package com.rdnutz.providers

import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class PlumperDPlugin : Plugin() {
    override fun load() {
        registerMainAPI(PlumperDProvider())
    }
}