package com.ithersta.tgbotapi.core

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext

public class BehaviourContextRunner internal constructor(
    public val block: BehaviourContext.() -> Unit
)

public fun runInBehaviourContext(
    block: BehaviourContext.() -> Unit
): BehaviourContextRunner = BehaviourContextRunner(block)
