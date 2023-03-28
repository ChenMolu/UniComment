package com.rocky.service;

import com.rocky.dto.Result;
import com.rocky.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {

    public Result queryById(Long id);

}
