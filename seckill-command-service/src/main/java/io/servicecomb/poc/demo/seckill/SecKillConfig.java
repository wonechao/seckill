/*
 *   Copyright 2017 Huawei Technologies Co., Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.servicecomb.poc.demo.seckill;

import io.servicecomb.poc.demo.seckill.event.PromotionEvent;
import io.servicecomb.poc.demo.seckill.repositories.PromotionEventRepository;
import io.servicecomb.poc.demo.seckill.repositories.PromotionEventRepositoryImpl;
import io.servicecomb.poc.demo.seckill.repositories.PromotionRepository;
import io.servicecomb.poc.demo.seckill.repositories.SpringBasedPromotionEventRepository;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.PagingAndSortingRepository;

@Configuration
class SecKillConfig {

  @Bean
  Map<String, SecKillCommandService<String>> commandServices() {
    return new HashMap<>();
  }

  @Bean
  List<SecKillPersistentRunner<String>> persistentRunners() {
    return new LinkedList<>();
  }

  @Bean
  PromotionEventRepository<String> promotionEventRepository(
      PagingAndSortingRepository<PromotionEvent<String>, Integer> repository) {

    return new PromotionEventRepositoryImpl<>(repository);
  }

  @Bean
  SecKillPromotionBootstrap secKillPromotionBootstrap(PromotionRepository promotionRepository,
      PromotionEventRepository<String> eventRepository,
      Map<String, SecKillCommandService<String>> commandServices,
      List<SecKillPersistentRunner<String>> persistentRunners,
      SecKillRecoveryService<String> recoveryService) {
    SecKillPromotionBootstrap<String> promotionBootstrap = new SecKillPromotionBootstrap<>(promotionRepository,
        eventRepository,
        commandServices,
        persistentRunners, recoveryService);
    promotionBootstrap.run();
    return promotionBootstrap;
  }

  @Bean
  SecKillRecoveryService<String> secKillRecoveryService(SpringBasedPromotionEventRepository<String> promotionEventRepository) {
    return new SecKillRecoveryService<String>(promotionEventRepository);
  }
}
