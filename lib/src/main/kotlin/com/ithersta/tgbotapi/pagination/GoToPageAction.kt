package com.ithersta.tgbotapi.pagination

import com.ithersta.tgbotapi.basetypes.Action
import kotlinx.serialization.Serializable

@Serializable
public class GoToPageAction(public val page: Int) : Action
