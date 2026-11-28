package me.jfenn.bingo.api.services

import me.jfenn.bingo.api.annotations.BingoInternalApi
import net.minecraft.server.MinecraftServer
import net.minecraft.network.chat.Component
import java.util.concurrent.CopyOnWriteArrayList

interface TextPlaceholderService {
    fun parseText(message: Component, server: MinecraftServer): Component
}

object TextPlaceholders {
    @BingoInternalApi
    @JvmStatic
    val services: MutableList<TextPlaceholderService> = CopyOnWriteArrayList()

    @JvmStatic
    @OptIn(BingoInternalApi::class)
    fun register(service: TextPlaceholderService) {
        services.add(service)
    }

    @JvmStatic
    @OptIn(BingoInternalApi::class)
    fun unregister(service: TextPlaceholderService) {
        services.remove(service)
    }
}
