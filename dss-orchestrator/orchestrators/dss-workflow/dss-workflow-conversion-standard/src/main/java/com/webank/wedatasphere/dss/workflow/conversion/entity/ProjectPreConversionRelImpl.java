/*
 * Copyright 2019 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.webank.wedatasphere.dss.workflow.conversion.entity;

import com.webank.wedatasphere.dss.orchestrator.converter.standard.ref.DSSToRelConversionRequestRef;
import com.webank.wedatasphere.dss.workflow.core.entity.Workflow;
import java.util.List;


public class ProjectPreConversionRelImpl implements ProjectPreConversionRel {

    private List<Workflow> workflows;
    private DSSToRelConversionRequestRef dssToRelConversionRequestRef;

    public void setWorkflows(List<Workflow> workflows) {
        this.workflows = workflows;
    }

    public void setDSSToRelConversionRequestRef(
        DSSToRelConversionRequestRef dssToRelConversionRequestRef) {
        this.dssToRelConversionRequestRef = dssToRelConversionRequestRef;
    }

    @Override
    public List<Workflow> getWorkflows() {
        return workflows;
    }

    @Override
    public DSSToRelConversionRequestRef getDSSToRelConversionRequestRef() {
        return dssToRelConversionRequestRef;
    }
}
