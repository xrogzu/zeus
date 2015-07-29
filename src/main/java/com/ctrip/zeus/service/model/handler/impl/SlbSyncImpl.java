package com.ctrip.zeus.service.model.handler.impl;

import com.ctrip.zeus.dal.core.*;
import com.ctrip.zeus.exceptions.ValidationException;
import com.ctrip.zeus.model.entity.*;
import com.ctrip.zeus.service.model.VirtualServerRepository;
import com.ctrip.zeus.service.model.handler.SlbSync;
import com.ctrip.zeus.service.model.handler.SlbValidator;
import com.ctrip.zeus.support.C;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.unidal.dal.jdbc.DalException;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author:xingchaowang
 * @date: 3/7/2015.
 */
@Component("slbSync")
public class SlbSyncImpl implements SlbSync {
    @Resource
    private SlbDao slbDao;
    @Resource
    private SlbServerDao slbServerDao;
    @Resource
    private SlbVipDao slbVipDao;
    @Resource
    private VirtualServerRepository virtualServerRepository;
    @Resource
    private SlbValidator slbModelValidator;
    
    Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void add(Slb slb) throws Exception {
        slbModelValidator.validate(slb);
        SlbDo d = C.toSlbDo(0L, slb);
        d.setCreatedTime(new Date());
        d.setVersion(1);

        slbDao.insert(d);
        Long id = d.getId();
        slb.setId(id);
        syncSlbVips(id, slb.getVips());
        syncSlbServers(id, slb.getSlbServers());
        for (VirtualServer virtualServer : slb.getVirtualServers()) {
            virtualServerRepository.addVirtualServer(id, virtualServer);
        }
    }

    @Override
    public void update(Slb slb) throws Exception {
        slbModelValidator.validate(slb);
        SlbDo check = slbDao.findById(slb.getId(), SlbEntity.READSET_FULL);
        if (check == null)
            throw new ValidationException("Slb does not exist.");
        if (check.getVersion() > slb.getVersion())
            throw new ValidationException("Newer Slb version is detected.");
        if (slbModelValidator.modifiable(slb)) {
            SlbDo d = C.toSlbDo(slb.getId(), slb);
            slbDao.updateById(d, SlbEntity.UPDATESET_FULL);
            Long id = d.getId();
            syncSlbVips(id, slb.getVips());
            syncSlbServers(id, slb.getSlbServers());
        } else {
            throw new ValidationException(check.getName() + " cannot be updated. Dependency exists.");
        }
    }

    @Override
    public int delete(Long slbId) throws Exception {
        SlbDo d = slbDao.findById(slbId, SlbEntity.READSET_FULL);
        if (d == null)
            return 0;
        if (slbModelValidator.removable(C.toSlb(d))) {
            slbVipDao.deleteBySlb(new SlbVipDo().setSlbId(slbId));
            slbServerDao.deleteBySlb(new SlbServerDo().setSlbId(slbId));
            virtualServerRepository.batchDeleteVirtualServers(slbId);
            return slbDao.deleteByPK(d);
        }
        throw new ValidationException(d.getName() + " cannot be deleted. Dependency exists.");
    }

    private void syncSlbVips(Long slbId, List<Vip> vips) throws DalException {
        Map<String, SlbVipDo> originVips = Maps.uniqueIndex(
                slbVipDao.findAllBySlb(slbId, SlbVipEntity.READSET_FULL), new Function<SlbVipDo, String>() {
                    @Override
                    public String apply(SlbVipDo input) {
                        return input.getIp();
                    }
                });
        for (Vip e : vips) {
            SlbVipDo originVip = originVips.get(e.getIp());
            if (originVip != null) {
                originVips.remove(originVip.getIp());
            }
            slbVipDao.insert(C.toSlbVipDo(slbId, e).setCreatedTime(new Date()));
        }
        for (SlbVipDo d : originVips.values()) {
            slbVipDao.deleteByPK(d);
        }
    }

    private void syncSlbServers(Long slbId, List<SlbServer> slbServers) throws DalException {
        Map<String, SlbServerDo> originServers = Maps.uniqueIndex(
                slbServerDao.findAllBySlb(slbId, SlbServerEntity.READSET_FULL), new Function<SlbServerDo, String>() {
                    @Override
                    public String apply(SlbServerDo input) {
                        return input.getIp();
                    }
                });
        for (SlbServer e : slbServers) {
            SlbServerDo originServer = originServers.get(e.getIp());
            if (originServer != null) {
                originServers.remove(originServer);
            }
            slbServerDao.insert(C.toSlbServerDo(slbId, e).setCreatedTime(new Date()));
        }
        for (SlbServerDo d : originServers.values()) {
            slbServerDao.deleteByPK(d);
        }
    }
}
