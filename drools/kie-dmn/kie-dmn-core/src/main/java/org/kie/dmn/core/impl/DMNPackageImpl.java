/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.dmn.core.impl;

import org.kie.api.io.ResourceType;
import org.kie.dmn.core.api.DMNModel;
import org.kie.dmn.core.api.DMNPackage;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DMNPackageImpl implements DMNPackage {

    private String namespace;

    private Map<String, DMNModel> models = new HashMap<>(  );

    public DMNPackageImpl() {
        this("");
    }

    public DMNPackageImpl(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    public void setNamespace( String namespace ) {
        this.namespace = namespace;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.DMN;
    }

    public DMNModel addModel( String name, DMNModel model ) {
        return models.put( name, model );
    }

    @Override
    public DMNModel getModel(String name){
        return models.get( name );
    }

    @Override
    public Map<String, DMNModel> getAllModels() {
        return Collections.unmodifiableMap( models );
    }
}
