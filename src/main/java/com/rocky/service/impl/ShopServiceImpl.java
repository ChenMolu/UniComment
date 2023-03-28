package com.rocky.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rocky.dto.Result;
import com.rocky.entity.Shop;
import com.rocky.mapper.ShopMapper;
import com.rocky.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocky.utils.RedisConstants;
import io.lettuce.core.RedisURI;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryById(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 从Redis中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //如果存在
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson , Shop.class);
            return Result.ok(shop);
        }
        // 如果不存在
        Shop shop = getById(id);
        if(shop == null){
            return Result.fail("店铺不存在");
        }
        //存在将商铺信息写入Redis中
        stringRedisTemplate.opsForValue().set(key , JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }
}
