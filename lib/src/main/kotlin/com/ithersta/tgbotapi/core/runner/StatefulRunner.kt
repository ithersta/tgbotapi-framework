package com.ithersta.tgbotapi.core.runner

import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext

/**
 * Used to run jobs inside [BehaviourContext].
 */
public class StatefulRunner internal constructor(
    public val block: StatefulRunnerContext.() -> Unit,
)

/**
 * Constructs [StatefulRunner].
 *
 * @param block job that will run inside the bot's [BehaviourContext].
 */
public fun statefulRunner(
    block: StatefulRunnerContext.() -> Unit,
): StatefulRunner = StatefulRunner(block)
