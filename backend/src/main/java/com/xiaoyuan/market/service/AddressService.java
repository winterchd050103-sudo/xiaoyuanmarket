package com.xiaoyuan.market.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiaoyuan.market.common.BusinessException;
import com.xiaoyuan.market.dto.AddressReq;
import com.xiaoyuan.market.entity.Address;
import com.xiaoyuan.market.mapper.AddressMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务
 */
@Service
public class AddressService extends ServiceImpl<AddressMapper, Address> {

    public List<Address> listByUser(Long userId) {
        return list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault)
                .orderByDesc(Address::getId));
    }

    public Address add(Long userId, AddressReq req) {
        Address address = new Address();
        copyReq(address, userId, req);
        save(address);
        if (address.getIsDefault() == 1) {
            clearDefaultExcept(userId, address.getId());
        }
        return address;
    }

    public Address update(Long userId, Long id, AddressReq req) {
        Address address = getOwned(userId, id);
        copyReq(address, userId, req);
        updateById(address);
        if (address.getIsDefault() == 1) {
            clearDefaultExcept(userId, id);
        }
        return address;
    }

    public void remove(Long userId, Long id) {
        getOwned(userId, id);
        removeById(id);
    }

    public void setDefault(Long userId, Long id) {
        getOwned(userId, id);
        clearDefaultExcept(userId, id);
        Address update = new Address();
        update.setId(id);
        update.setIsDefault(1);
        updateById(update);
    }

    private Address getOwned(Long userId, Long id) {
        Address address = getById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        return address;
    }

    private void copyReq(Address address, Long userId, AddressReq req) {
        address.setUserId(userId);
        address.setReceiver(req.getReceiver());
        address.setPhone(req.getPhone());
        address.setProvince(req.getProvince());
        address.setCity(req.getCity());
        address.setDistrict(req.getDistrict());
        address.setDetail(req.getDetail());
        address.setIsDefault(Boolean.TRUE.equals(req.getIsDefault()) ? 1 : 0);
    }

    @Transactional
    protected void clearDefaultExcept(Long userId, Long exceptId) {
        List<Address> defaults = list(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .eq(Address::getIsDefault, 1)
                .ne(Address::getId, exceptId));
        for (Address a : defaults) {
            Address update = new Address();
            update.setId(a.getId());
            update.setIsDefault(0);
            updateById(update);
        }
    }
}
