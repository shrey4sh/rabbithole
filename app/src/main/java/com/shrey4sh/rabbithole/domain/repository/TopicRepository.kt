package com.shrey4sh.rabbithole.domain.repository

import com.shrey4sh.rabbithole.domain.model.RabbitHole
import kotlinx.coroutines.flow.Flow

interface TopicRepository {
    fun searchTopic(query: String): Flow<RabbitHole?>
    suspend fun randomTopic(): RabbitHole
}
