package de.mroedel.maildozer

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

@Configuration
class Config {

    @Bean(name = ["backgroundTaskExecutor"])
    fun backgroundTaskExecutor(): ThreadPoolTaskExecutor? {
        val threadPoolExecutor = ThreadPoolTaskExecutor()
        threadPoolExecutor.corePoolSize = 2
        threadPoolExecutor.maxPoolSize = 2
        threadPoolExecutor.setQueueCapacity(1000)
        threadPoolExecutor.initialize()

        return threadPoolExecutor
    }

    @Bean(name = ["mailFetchTaskExecutor"])
    fun mailFetchTaskExecutor(): ThreadPoolTaskExecutor? {
        val threadPoolExecutor = ThreadPoolTaskExecutor()
        threadPoolExecutor.corePoolSize = 10
        threadPoolExecutor.maxPoolSize = 10
        threadPoolExecutor.setQueueCapacity(1000)
        threadPoolExecutor.initialize()

        return threadPoolExecutor
    }

}