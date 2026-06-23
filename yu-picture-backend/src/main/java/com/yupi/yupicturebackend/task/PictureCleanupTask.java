package com.yupi.yupicturebackend.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.qcloud.cos.model.StorageClass;
import com.yupi.yupicturebackend.config.CleanupProperties;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PictureCleanupTask {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CleanupProperties cleanupProperties;

    private static final String LOCK_ORPHAN = "yupicture:lock:cleanup:orphan";
    private static final String LOCK_REJECTED = "yupicture:lock:cleanup:rejected";
    private static final String LOCK_ARCHIVE = "yupicture:lock:cleanup:archive";

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOrphanPictures() {
        if (!tryLock(LOCK_ORPHAN)) {
            return;
        }
        try {
            Date threshold = DateUtil.offsetDay(new Date(), -cleanupProperties.getDeletedRetentionDays());
            int totalCleaned = 0;
            for (int i = 0; i < 100; i++) {
                List<Picture> batch = pictureMapper.selectDeletedBefore(threshold, cleanupProperties.getBatchSize());
                if (CollUtil.isEmpty(batch)) {
                    break;
                }
                for (Picture pic : batch) {
                    try {
                        deleteCosFile(pic.getUrl());
                        deleteCosFile(pic.getThumbnailUrl());
                        pictureMapper.physicalDeleteById(pic.getId());
                        totalCleaned++;
                    } catch (Exception e) {
                        log.error("[OrphanCleanup] id={}, url={}", pic.getId(), pic.getUrl(), e);
                    }
                }
            }
            log.info("[OrphanCleanup] Done. cleaned={}, threshold={}", totalCleaned, threshold);
        } finally {
            unlock(LOCK_ORPHAN);
        }
    }

    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanRejectedPictures() {
        if (!tryLock(LOCK_REJECTED)) {
            return;
        }
        try {
            Date threshold = DateUtil.offsetDay(new Date(), -cleanupProperties.getRejectedRetentionDays());
            int totalCleaned = 0;
            for (int i = 0; i < 100; i++) {
                List<Picture> batch = pictureMapper.selectRejectedBefore(
                        PictureReviewStatusEnum.REJECT.getValue(), threshold,
                        cleanupProperties.getBatchSize());
                if (CollUtil.isEmpty(batch)) {
                    break;
                }
                for (Picture pic : batch) {
                    try {
                        deleteCosFile(pic.getUrl());
                        deleteCosFile(pic.getThumbnailUrl());
                        pictureMapper.deleteById(pic.getId());
                        totalCleaned++;
                    } catch (Exception e) {
                        log.error("[RejectedCleanup] id={}, url={}", pic.getId(), pic.getUrl(), e);
                    }
                }
            }
            log.info("[RejectedCleanup] Done. cleaned={}, threshold={}", totalCleaned, threshold);
        } finally {
            unlock(LOCK_REJECTED);
        }
    }

    @Scheduled(cron = "0 0 4 * * ?")
    public void archiveColdPictures() {
        if (!tryLock(LOCK_ARCHIVE)) {
            return;
        }
        try {
            Date threshold = DateUtil.offsetDay(new Date(), -cleanupProperties.getArchiveUnmodifiedDays());
            int totalArchived = 0;
            for (int i = 0; i < 100; i++) {
                List<Picture> batch = pictureMapper.selectUnmodifiedBefore(threshold, cleanupProperties.getBatchSize());
                if (CollUtil.isEmpty(batch)) {
                    break;
                }
                for (Picture pic : batch) {
                    try {
                        archiveCosFile(pic.getUrl());
                        archiveCosFile(pic.getThumbnailUrl());
                        totalArchived++;
                    } catch (Exception e) {
                        log.error("[Archive] id={}, url={}", pic.getId(), pic.getUrl(), e);
                    }
                }
            }
            log.info("[Archive] Done. archived={}, threshold={}", totalArchived, threshold);
        } finally {
            unlock(LOCK_ARCHIVE);
        }
    }

    private boolean tryLock(String lockKey) {
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", cleanupProperties.getLockTimeoutSeconds(), TimeUnit.SECONDS);
        return Boolean.TRUE.equals(ok);
    }

    private void unlock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    private void deleteCosFile(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        String key = urlToKey(url);
        if (StrUtil.isBlank(key)) {
            return;
        }
        cosManager.deleteObject(key);
    }

    private void archiveCosFile(String url) {
        if (StrUtil.isBlank(url)) {
            return;
        }
        String key = urlToKey(url);
        if (StrUtil.isBlank(key)) {
            return;
        }
        cosManager.changeStorageClass(key, StorageClass.Standard_IA);
    }

    private String urlToKey(String fullUrl) {
        String prefix = cosClientConfig.getHost() + "/";
        if (fullUrl.startsWith(prefix)) {
            return fullUrl.substring(prefix.length());
        }
        return fullUrl;
    }
}
