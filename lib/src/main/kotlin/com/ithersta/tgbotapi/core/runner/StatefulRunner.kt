package com.ithersta.tgbotapi.core.runner

/**
 * Used to run jobs inside [StatefulRunnerContext].
 */
public class StatefulRunner internal constructor(
    public val block: StatefulRunnerContext.() -> Unit,
)

/**
 * Constructs [StatefulRunner].
 *
 * @param block job that will run inside the bot's [StatefulRunnerContext].
 */
public fun statefulRunner(
    block: StatefulRunnerContext.() -> Unit,
): StatefulRunner = StatefulRunner(block)
