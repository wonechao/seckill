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

import io.servicecomb.poc.demo.seckill.entities.PromotionEntity;
import io.servicecomb.poc.demo.seckill.event.CouponGrabbedEvent;
import io.servicecomb.poc.demo.seckill.event.PromotionFinishEvent;
import io.servicecomb.poc.demo.seckill.event.PromotionStartEvent;
import io.servicecomb.poc.demo.seckill.json.ToJsonFormat;
import io.servicecomb.poc.demo.seckill.repositories.SecKillEventRepository;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecKillPersistentRunner<T> {

  private static final Logger logger = LoggerFactory.getLogger(SecKillPersistentRunner.class);

  private final BlockingQueue<T> coupons;
  private final SecKillEventRepository repository;
  private final AtomicInteger claimedCoupons;
  private final PromotionEntity promotion;
  private ToJsonFormat toJsonFormat;
  private final SecKillRecoveryCheckResult<T> recoveryInfo;

  public SecKillPersistentRunner(PromotionEntity promotion,
      BlockingQueue<T> couponQueue,
      AtomicInteger claimedCoupons,
      SecKillEventRepository repository,
      ToJsonFormat toJsonFormat,
      SecKillRecoveryCheckResult<T> recoveryInfo) {

    this.promotion = promotion;
    this.coupons = couponQueue;
    this.repository = repository;
    this.claimedCoupons = claimedCoupons;
    this.toJsonFormat = toJsonFormat;
    this.recoveryInfo = recoveryInfo;
  }

  public void run() {
    if (!recoveryInfo.isStarted()) {
      logger.info("PromotionEntity {} is not started", promotion);
      repository.save(new PromotionStartEvent(promotion).toEntity(toJsonFormat));
    }

    logger.info("Starting to consume user requests for promotion {}", promotion);
    CompletableFuture.runAsync(() -> {
      boolean promotionEnded = false;
      while (!Thread.currentThread().isInterrupted() && !hasConsumedAllCoupons() && !promotionEnded) {
        try {
          promotionEnded = consumeCoupon();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }

      logger.info("Consumed all user requests for promotion {}", promotion);
      repository.save(new PromotionFinishEvent(promotion).toEntity(toJsonFormat));
    }, Executors.newFixedThreadPool(4));
  }

  private boolean consumeCoupon() throws InterruptedException {
    T customerId = coupons
        .poll(promotion.getFinishTime().getTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    if (customerId != null) {
      repository.save(new CouponGrabbedEvent<>(promotion, customerId).toEntity(toJsonFormat));
      logger.info("Assigned promotion coupon {} to customer {}", promotion, customerId);
    }
    return customerId == null;
  }

  private boolean hasConsumedAllCoupons() {
    return claimedCoupons.get() >= recoveryInfo.remainingCoupons() && coupons.isEmpty();
  }
}
