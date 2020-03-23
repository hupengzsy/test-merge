//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.hzero.gateway.helper.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.hzero.core.base.BaseConstants;
import org.hzero.core.redis.RedisHelper;
import org.hzero.gateway.helper.domain.vo.PermissionCacheVO;
import org.hzero.gateway.helper.entity.PermissionDO;
import org.hzero.gateway.helper.infra.mapper.PermissionPlusMapper;
import org.hzero.gateway.helper.service.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

@Service
public class PermissionServiceImpl implements PermissionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionService.class);
    private final AntPathMatcher matcher = new AntPathMatcher();
    private PermissionPlusMapper permissionPlusMapper;
    private RedisHelper redisHelper;
    private ObjectMapper mapper;

    public PermissionServiceImpl(PermissionPlusMapper permissionPlusMapper, RedisHelper redisHelper) {
        this.mapper = BaseConstants.MAPPER;
        this.permissionPlusMapper = permissionPlusMapper;
        this.redisHelper = redisHelper;
    }

    @Cacheable(
            value = {"permission"},
            key = "'choerodon:permission:'+#requestKey",
            unless = "#result == null"
    )
    public PermissionDO selectPermissionByRequest(String requestKey) {
        String[] request = requestKey.split(":::");
        String uri = request[0];
        String method = request[1];
        String serviceName = request[2];
        List<PermissionDO> permissionDOS = this.selectPermissions(serviceName, method);
        LOGGER.debug("serviceName:【" + serviceName + "】; method: 【" + method + "】");
        LOGGER.debug("permissionDOS.size() : "+ permissionDOS.size());
        List<PermissionDO> matchPermissions = (List) permissionDOS.stream().filter((t) -> {
            return this.matcher.match(t.getPath(), uri);
        }).sorted((o1, o2) -> {
            Comparator<String> patternComparator = this.matcher.getPatternComparator(uri);
            return patternComparator.compare(o1.getPath(), o2.getPath());
        }).collect(Collectors.toList());
        int matchSize = matchPermissions.size();
        if (matchSize < 1) {
            return null;
        } else {
            PermissionDO bestMatchPermission = (PermissionDO) matchPermissions.get(0);
            if (matchSize > 1) {
                LOGGER.info("Request: {} match multiply permission: {}, the best match is: {}", new Object[]{uri, matchPermissions, bestMatchPermission.getPath()});
            }

            return bestMatchPermission;
        }
    }

    private List<PermissionDO> selectPermissions(String serviceName, String method) {
        serviceName = StringUtils.lowerCase(serviceName);
        method = StringUtils.lowerCase(method);
        String permissionKey = PermissionDO.generateKey(serviceName, method);
        Map<String, String> map = this.redisHelper.hshGetAll(permissionKey);
        Object permissions;
        if (MapUtils.isNotEmpty(map)) {
            permissions = new ArrayList(map.size());

            try {
                Iterator var6 = map.values().iterator();

                while (var6.hasNext()) {
                    String permission = (String) var6.next();
                    ((List) permissions).add(this.mapper.readValue(permission, PermissionDO.class));
                }
            } catch (IOException var10) {
                LOGGER.error("deserialize json error.");
            }
        } else {
            permissions = this.permissionPlusMapper.selectPermissionByMethodAndService(method, serviceName);
            LOGGER.info("cache service permissions, serviceName={}, method={}, permissionsSize={}", new Object[]{serviceName, method, ((List) permissions).size()});
            String key = PermissionDO.generateKey(serviceName, method);

            try {
                Iterator var12 = ((List) permissions).iterator();

                while (var12.hasNext()) {
                    PermissionDO permission = (PermissionDO) var12.next();
                    this.redisHelper.hshPut(key, permission.getId().toString(), this.mapper.writeValueAsString(new PermissionCacheVO(permission)));
                }
            } catch (JsonProcessingException var9) {
                LOGGER.error("serialize json error.");
            }
        }

        return (List) permissions;
    }
}
