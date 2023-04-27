package com.ithersta.tgbotapi.persistence

import com.ithersta.tgbotapi.basetypes.Action
import java.security.SecureRandom
import kotlin.streams.asSequence

public class PersistedAction(
    public val key: String,
    public val action: Action
) {
    internal companion object {
        private val secureRandom = SecureRandom.getInstanceStrong()
        private val allowedChars = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toList().toCharArray()

        fun from(action: Action) = PersistedAction(
            key = secureRandom.ints(64, 0, allowedChars.size).asSequence()
                .joinToString(separator = "") { allowedChars[it].toString() },
            action = action
        )
    }
}
