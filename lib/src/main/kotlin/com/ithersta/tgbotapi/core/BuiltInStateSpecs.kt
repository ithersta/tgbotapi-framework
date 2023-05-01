package com.ithersta.tgbotapi.core

import com.ithersta.tgbotapi.pagination.withPaginationSpec

internal val builtInStateSpecs: List<StateSpec<*, *>> = listOf(withPaginationSpec)
