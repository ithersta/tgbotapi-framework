package com.ithersta.tgbotapi.message

import com.ithersta.tgbotapi.basetypes.MessageState
import com.ithersta.tgbotapi.basetypes.Role
import com.ithersta.tgbotapi.builders.FrameworkDslMarker
import com.ithersta.tgbotapi.core.HandlerContext
import com.ithersta.tgbotapi.core.StateAccessor
import dev.inmo.tgbotapi.types.MessageId

@FrameworkDslMarker
public class MessageContext<S : MessageState, R : Role, M : MessageId?>(
    context: HandlerContext<S, StateAccessor.Changing<S>, R, M>,
) : HandlerContext<S, StateAccessor.Changing<S>, R, M> by context,
    ActionButtonContext by ListActionButtonContext()
