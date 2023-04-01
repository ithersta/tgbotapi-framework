package com.ithersta.tgbotapi.basetypes

import kotlinx.serialization.Serializable

public interface MessageState {
    @Serializable
    public object Empty : MessageState
}
