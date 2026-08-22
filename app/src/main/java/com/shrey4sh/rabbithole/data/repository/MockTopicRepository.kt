package com.shrey4sh.rabbithole.data.repository

import com.shrey4sh.rabbithole.data.mock.MockData
import com.shrey4sh.rabbithole.domain.model.RabbitHole
import com.shrey4sh.rabbithole.domain.repository.TopicRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

/** Phase 2: mock-backed repository with realistic discovery delays. */
class MockTopicRepository @Inject constructor() : TopicRepository {

    override fun searchTopic(query: String): Flow<RabbitHole?> = flow {
        delay(600) // "Finding the main topic..."
        val hole = MockData.search(query)
        delay(500) // "Discovering related concepts..."
        emit(hole)
    }

    override suspend fun randomTopic(): RabbitHole {
        delay(400)
        return MockData.random()
    }
}
