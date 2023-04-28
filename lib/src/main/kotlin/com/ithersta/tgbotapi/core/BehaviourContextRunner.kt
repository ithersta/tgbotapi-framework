package com.ithersta.tgbotapi.core

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext

/**
 * Used to run jobs inside [BehaviourContext].
 */
public class BehaviourContextRunner internal constructor(
    public val block: BehaviourContext.() -> Unit
)

/**
 * Constructs [BehaviourContextRunner].
 *
 * @param block job that will run inside the bot's [BehaviourContext].
 */
public fun runInBehaviourContext(
    block: BehaviourContext.() -> Unit
): BehaviourContextRunner = BehaviourContextRunner(block)
