/*
 * Copyright 2017-2019 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.federatedstore.operation.handler.impl;

import uk.gov.gchq.gaffer.federatedstore.FederatedStore;
import uk.gov.gchq.gaffer.federatedstore.operation.RemoveGraph;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.operation.handler.OutputOperationHandler;

import static uk.gov.gchq.gaffer.federatedstore.util.FederatedStoreUtil.isUserRequestingAdminUsage;

/**
 * A handler for RemoveGraph operation for the FederatedStore.
 * <p>
 * Does not delete the graph, just removes it from the scope of the FederatedStore.
 *
 * @see FederatedStore
 * @see RemoveGraph
 */
public class FederatedRemoveGraphHandler implements OutputOperationHandler<RemoveGraph, Boolean> {
    @Override
    public Boolean doOperation(final RemoveGraph operation, final Context context, final Store store) throws OperationException {
        try {
            final boolean userRequestingAdminUsage = isUserRequestingAdminUsage(operation);
            return ((FederatedStore) store).remove(operation.getGraphId(), context.getUser(), userRequestingAdminUsage);
        } catch (final Exception e) {
            throw new OperationException("Error removing graph: " + operation.getGraphId(), e);
        }
    }
}
