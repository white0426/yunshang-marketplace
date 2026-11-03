package com.yunshang.market.service;

import com.yunshang.market.common.ApiException;
import com.yunshang.market.dto.ApiDtos.AddressRequest;
import com.yunshang.market.dto.ApiDtos.AddressView;
import com.yunshang.market.entity.UserAddress;
import com.yunshang.market.mapper.UserAddressMapper;
import com.yunshang.market.security.AuthPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AddressService {
    private static final int MAX_ADDRESSES = 10;

    private final UserAddressMapper addressMapper;

    public AddressService(UserAddressMapper addressMapper) {
        this.addressMapper = addressMapper;
    }

    public List<AddressView> list(AuthPrincipal principal) {
        return addressMapper.selectByUser(principal.userId())
                .stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public AddressView create(AuthPrincipal principal, AddressRequest request) {
        long count = addressMapper.countByUser(principal.userId());
        if (count >= MAX_ADDRESSES) {
            throw ApiException.badRequest("最多保存 10 个收货地址");
        }
        boolean firstAddress = count == 0;
        boolean makeDefault = firstAddress || Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault) clearDefaults(principal.userId());

        UserAddress address = new UserAddress();
        address.setUserId(principal.userId());
        apply(address, request);
        address.setIsDefault(makeDefault);
        addressMapper.insert(address);
        return toView(address);
    }

    @Transactional
    public AddressView update(AuthPrincipal principal, Long addressId, AddressRequest request) {
        UserAddress address = requireOwned(principal, addressId);
        boolean makeDefault = Boolean.TRUE.equals(request.defaultAddress());
        if (makeDefault) clearDefaults(principal.userId());
        apply(address, request);
        address.setIsDefault(makeDefault || Boolean.TRUE.equals(address.getIsDefault()));
        addressMapper.updateById(address);
        return toView(address);
    }

    @Transactional
    public void setDefault(AuthPrincipal principal, Long addressId) {
        UserAddress address = requireOwned(principal, addressId);
        clearDefaults(principal.userId());
        address.setIsDefault(true);
        addressMapper.updateById(address);
    }

    @Transactional
    public void delete(AuthPrincipal principal, Long addressId) {
        UserAddress address = requireOwned(principal, addressId);
        addressMapper.deleteById(addressId);
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            UserAddress next = addressMapper.selectMostRecent(principal.userId());
            if (next != null) {
                next.setIsDefault(true);
                addressMapper.updateById(next);
            }
        }
    }

    private UserAddress requireOwned(AuthPrincipal principal, Long addressId) {
        UserAddress address = addressMapper.selectById(addressId);
        if (address == null) {
            throw ApiException.notFound("收货地址不存在");
        }
        if (!principal.userId().equals(address.getUserId())) {
            throw ApiException.forbidden("不能操作其他用户的收货地址");
        }
        return address;
    }

    private void clearDefaults(String userId) {
        addressMapper.clearDefault(userId);
    }

    private void apply(UserAddress address, AddressRequest request) {
        address.setReceiverName(request.receiverName().trim());
        address.setReceiverPhone(request.receiverPhone().trim());
        address.setRegion(request.region().trim());
        address.setDetail(request.detail().trim());
    }

    private AddressView toView(UserAddress address) {
        return new AddressView(
                address.getId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getRegion(),
                address.getDetail(),
                address.getRegion() + " " + address.getDetail(),
                Boolean.TRUE.equals(address.getIsDefault())
        );
    }
}
