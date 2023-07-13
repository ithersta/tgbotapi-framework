package com.ithersta.tgbotapi.init.plugins

import com.ithersta.tgbotapi.init.BotConfigurer

public fun interface Plugin {
    public fun BotConfigurer.install()
}