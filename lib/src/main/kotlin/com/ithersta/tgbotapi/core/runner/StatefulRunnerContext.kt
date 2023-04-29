package com.ithersta.tgbotapi.core.runner

import com.ithersta.tgbotapi.core.UnboundStateAccessor
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext

public class StatefulRunnerContext internal constructor(
    behaviourContext: BehaviourContext,
    public val state: UnboundStateAccessor,
) : BehaviourContext by behaviourContext
