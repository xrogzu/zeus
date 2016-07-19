package com.ctrip.zeus.service.query.command;

import com.ctrip.zeus.tag.entity.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhoumy on 2016/7/19.
 */
public class PropQueryCommand implements QueryCommand {
    public final int union_prop = 0;
    public final int join_prop = 1;

    private final String type;

    private String[] values = new String[2];

    public PropQueryCommand() {
        this.type = "prop";
    }

    @Override
    public boolean add(String queryName, String queryValue) {
        int idx;
        switch (queryName) {
            case "anyProp":
            case "unionProp":
                idx = union_prop;
                break;
            case "props":
            case "joinPro":
                idx = join_prop;
                break;
            default:
                return false;
        }
        return addAtIndex(idx, queryValue);
    }

    @Override
    public boolean addAtIndex(int idx, String queryValue) {
        if (idx < values.length) {
            values[idx] = queryValue;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean hasValue(int idx) {
        return values[idx] != null;
    }

    @Override
    public String[] getValue(int idx) {
        String value = values[idx];
        return value == null ? null : value.split(",");
    }

    public List<Property> getProperties(int idx) {
        List<Property> properties = new ArrayList<>();
        for (String s : getValue(idx)) {
            int ps = s.trim().indexOf(':');
            if (ps == -1) continue;
            properties.add(new Property().setName(s.substring(0, ps)).setValue(s.substring(ps + 1)));
        }
        return properties;
    }

    @Override
    public String getType() {
        return type;
    }
}