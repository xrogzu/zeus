package com.ctrip.zeus.service.query;

import java.util.Set;

/**
 * Created by zhoumy on 2015/8/7.
 */
public interface GroupCriteriaQuery {

    Set<Long> queryAll() throws Exception;

    Set<Long> queryByDomain(String domain) throws Exception;

    Set<Long> queryBySlbId(Long slbId) throws Exception;
}
