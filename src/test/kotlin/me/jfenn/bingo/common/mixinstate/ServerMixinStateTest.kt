package me.jfenn.bingo.common.mixinstate

import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertEquals

class ServerMixinStateTest {

    private class Holder(val value: Int = 0)

    @Test
    fun `get returns the same instance for the same key`() {
        val key = Any()
        val first = ServerMixinState.storeFor(key).get(Holder::class.java) { Holder(1) }
        val second = ServerMixinState.storeFor(key).get(Holder::class.java) { Holder(2) }
        assertSame(first, second)
        assertEquals(1, second.value)
    }

    @Test
    fun `different keys are isolated`() {
        val a = Any()
        val b = Any()
        val holderA = ServerMixinState.storeFor(a).get(Holder::class.java) { Holder(1) }
        val holderB = ServerMixinState.storeFor(b).get(Holder::class.java) { Holder(2) }
        assertEquals(1, holderA.value)
        assertEquals(2, holderB.value)
    }

    @Test
    fun `peek does not create state`() {
        val key = Any()
        assertNull(ServerMixinState.storeFor(key).peek(Holder::class.java))
        assertNotNull(ServerMixinState.storeFor(key).get(Holder::class.java) { Holder() })
        assertNotNull(ServerMixinState.storeFor(key).peek(Holder::class.java))
    }

    @Test
    fun `clearAll removes every store`() {
        ServerMixinState.storeFor(Any()).get(Holder::class.java) { Holder() }
        ServerMixinState.storeFor(Any()).get(Holder::class.java) { Holder() }
        ServerMixinState.clearAll()
        assertEquals(0, ServerMixinState.storeCount())
    }
}
