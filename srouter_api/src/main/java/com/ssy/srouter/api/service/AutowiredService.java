package com.ssy.srouter.api.service;

import com.ssy.srouter.api.template.IProvider;

public interface AutowiredService extends IProvider {

    void autowrite(Object instance);
}
