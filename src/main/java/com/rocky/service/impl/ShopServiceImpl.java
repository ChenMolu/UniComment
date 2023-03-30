package com.rocky.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.rocky.dto.Result;
import com.rocky.entity.Shop;
import com.rocky.mapper.ShopMapper;
import com.rocky.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rocky.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

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
        //缓存击穿
        Shop shop = queryWithMutex(id);

        return Result.ok(shop);
    }

    /**
     * 解决缓存穿透
     * @param id
     * @return
     */
    public Result queryWithPassThrough(Long id) {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 从Redis中获取商铺信息
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //如果存在
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson , Shop.class);
            return Result.ok(shop);
        }
        //命中的是否是空值(解决缓存穿透问题)
        if(shopJson != null){
            return Result.fail("店铺不存在");
        }
        // 如果不存在
        Shop shop = getById(id);
        if(shop == null){
            //不存在时写入空值
            stringRedisTemplate.opsForValue().set(key , "",RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            return Result.fail("店铺不存在");
        }
        //存在将商铺信息写入Redis中
        stringRedisTemplate.opsForValue().set(key , JSONUtil.toJsonStr(shop),RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    /**
     * 解决缓存击穿与穿透
     * @param id
     * @return
     */
    public Shop queryWithMutex(Long id)  {
        String key = RedisConstants.CACHE_SHOP_KEY + id;
        // 1、从redis中查询商铺缓存
        String shopJson = stringRedisTemplate.opsForValue().get("key");
        // 2、判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 存在,直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断命中的值是否是空值
        if (shopJson != null) {
            //返回一个错误信息
            return null;
        }
        // 4.实现缓存重构
        //4.1 获取互斥锁
        String lockKey = "lock:shop:" + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2 判断否获取成功
            if(!isLock){
                // TODO
                //4.3 失败，则休眠重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //4.4 成功，根据id查询数据库
            // TODO 这里应该重新获取缓存 如果缓存存在则不需要重建缓存
            shop = getById(id);
            // 5.不存在，返回错误
            if(shop == null){
                //将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);
                //返回错误信息
                return null;
            }
            //6.写入redis
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),RedisConstants.CACHE_NULL_TTL,TimeUnit.MINUTES);

        }catch (Exception e){
            throw new RuntimeException(e);
        }
        finally {
            //7.释放互斥锁
            unlock(lockKey);
        }
        return shop;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if(id == null){
            return Result.fail("店铺Id不能为空！");
        }
        //更新数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(RedisConstants.CACHE_SHOP_KEY+id);
        return Result.ok();
    }

}
