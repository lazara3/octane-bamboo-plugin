/*
 *     Copyright 2017 EntIT Software LLC, a Micro Focus company, L.P.
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.hp.octane.plugins.bamboo.listener;

import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanManager;
import com.atlassian.bamboo.plan.cache.CachedPlanManager;
import com.atlassian.bamboo.plan.cache.ImmutableChain;
import com.atlassian.bamboo.plan.cache.ImmutableChainBranch;
import com.atlassian.sal.api.component.ComponentLocator;
import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.hp.octane.integrations.dto.events.MultiBranchType;
import com.hp.octane.integrations.dto.pipelines.PipelineNode;
import com.hp.octane.plugins.bamboo.octane.SDKBasedLoggerProvider;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import static com.hp.octane.plugins.bamboo.listener.BaseListener.CONVERTER;

public class MultibranchHelper {

    private static CachedPlanManager cachedPlanManager;
    private static PlanManager planManager;
    protected static final Logger log = SDKBasedLoggerProvider.getLogger(OctanePostChainAction.class);


    private static CachedPlanManager getCachedPlanManager() {
        if (cachedPlanManager == null) {
            cachedPlanManager = ComponentLocator.getComponent(CachedPlanManager.class);
        }
        return cachedPlanManager;
    }

    private static PlanManager getPlanManager() {
        if (planManager == null) {
            planManager = ComponentLocator.getComponent(PlanManager.class);
        }
        return planManager;
    }

    public static boolean isMultiBranchParent(ImmutableChain chain) {
        try {
            Set<PlanKey> branchKeys = getCachedPlanManager().getBranchKeysOfChain(chain.getPlanKey());
            return !branchKeys.isEmpty();
        } catch (Exception e) {
            log.error("Failed to check isMultiBranchParent : " + e.getMessage());
            return false;
        }
    }

    public static void enrichMultiBranchParentPipeline(ImmutableChain chain, PipelineNode pipelineNode) {
        if (isMultiBranchParent(chain)) {
            pipelineNode.setMultiBranchType(MultiBranchType.MULTI_BRANCH_PARENT);
        }
    }

    public static void enrichMultiBranchEvent(ImmutableChain chain, CIEvent ciEvent) {
        if (chain instanceof ImmutableChainBranch) {
            ciEvent.setParentCiId(chain.getMaster().getPlanKey().toString()).setMultiBranchType(MultiBranchType.MULTI_BRANCH_CHILD);
        }
    }

    public static void onChainDeleted(PlanKey planKey) {
        Plan plan = getPlanManager().getPlanByKey(planKey);
        if (plan instanceof ImmutableChainBranch) {
            CIEvent cievent = CONVERTER.getEventWithDetails(planKey.toString(), CIEventType.DELETED);
            OctaneSDK.getClients().forEach(client -> client.getEventsService().publishEvent(cievent));
        }
    }


}
