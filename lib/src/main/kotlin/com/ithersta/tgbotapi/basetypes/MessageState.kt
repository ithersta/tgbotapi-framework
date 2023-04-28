package com.ithersta.tgbotapi.basetypes

import kotlinx.serialization.Serializable

/**
 * Base interface for message states.
 */
public interface MessageState {
    /**
     * Empty message state.
     *
     * It's used by default when no other state can be found.
     */
    @Serializable
    public object Empty : MessageState
}
