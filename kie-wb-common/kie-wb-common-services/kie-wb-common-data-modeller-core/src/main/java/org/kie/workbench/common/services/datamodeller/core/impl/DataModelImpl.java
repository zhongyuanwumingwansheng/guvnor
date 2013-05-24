/**
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.datamodeller.core.impl;

import org.kie.workbench.common.services.datamodeller.core.ObjectSource;
import org.kie.workbench.common.services.datamodeller.util.NamingUtils;
import org.kie.workbench.common.services.datamodeller.core.DataModel;
import org.kie.workbench.common.services.datamodeller.core.DataObject;

import java.util.*;

public class DataModelImpl implements DataModel {

    Map<String, DataObject> dataObjects = new HashMap<String, DataObject>();

    Map<String, DataObject> dependencyDataObjects = new HashMap<String, DataObject>();

    public DataModelImpl() {
    }

    @Override
    public Set<DataObject> getDataObjects() {
        return getDataObjects(ObjectSource.INTERNAL);
    }

    @Override
    public Set<DataObject> getDataObjects(ObjectSource source) {
        switch (source) {
            case INTERNAL:
                return getDataObjects(dataObjects);
            case DEPENDENCY:
                return getDataObjects(dependencyDataObjects);
        }
        return null;
    }

    @Override
    public DataObject getDataObject(String className) {
        return getDataObject(className, ObjectSource.INTERNAL);
    }

    @Override
    public DataObject getDataObject(String className, ObjectSource source) {
        switch (source) {
            case INTERNAL:
                return dataObjects.get(className);
            case DEPENDENCY:
                return dependencyDataObjects.get(className);
        }
        return null;
    }

    @Override
    public DataObject removeDataObject(String className) {
        return removeDataObject(className, ObjectSource.INTERNAL);
    }

    @Override
    public DataObject removeDataObject(String className, ObjectSource source) {
        switch (source) {
            case INTERNAL:
                return dataObjects.remove(className);
            case DEPENDENCY:
                return dependencyDataObjects.remove(className);
        }
        return null;
    }

    @Override
    public DataObject addDataObject(String packageName, String name) {
        return addDataObject(packageName, name, ObjectSource.INTERNAL);
    }

    @Override
    public DataObject addDataObject(String packageName, String name, ObjectSource source) {
        switch (source) {
            case INTERNAL:
                return addDataObject(packageName, name, dataObjects);
            case DEPENDENCY:
                return addDataObject(packageName, name, dependencyDataObjects);
        }
        return null;
    }

    @Override
    public DataObject addDataObject(String className) {
        return addDataObject(className, ObjectSource.INTERNAL);
    }

    @Override
    public DataObject addDataObject(String className, ObjectSource source) {
        String name = NamingUtils.getInstance().extractClassName(className);
        String packageName = NamingUtils.getInstance().extractPackageName(className);
        return addDataObject(packageName, name, source);
    }

    private Set<DataObject> getDataObjects(Map<String, DataObject> objectsMap) {
        HashSet<DataObject> set = new HashSet<DataObject>();
        set.addAll(objectsMap.values());
        return set;
    }

    private DataObject addDataObject(String packageName, String name, Map<String, DataObject> objectsMap) {
        DataObject dataObject = new DataObjectImpl(packageName, name);
        objectsMap.put(dataObject.getClassName(), dataObject);
        return dataObject;
    }

}