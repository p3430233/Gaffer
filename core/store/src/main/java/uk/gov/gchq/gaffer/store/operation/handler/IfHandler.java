/*
 * Copyright 2017 Crown Copyright
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
package uk.gov.gchq.gaffer.store.operation.handler;

import uk.gov.gchq.gaffer.operation.Operation;
import uk.gov.gchq.gaffer.operation.OperationChain;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.impl.If;
import uk.gov.gchq.gaffer.operation.io.Input;
import uk.gov.gchq.gaffer.operation.io.Output;
import uk.gov.gchq.gaffer.store.Context;
import uk.gov.gchq.gaffer.store.Store;

/**
 * An operation handler for {@link If} operations.
 * If {@link If#getThen()} or {@link If#getOtherwise()} returns a null operation, then the input object will simply be returned.
 */
public class IfHandler implements OutputOperationHandler<If<Object, Object>, Object> {
    @Override
    public Object doOperation(final If operation, final Context context, final Store store) throws OperationException {
        final Object input = operation.getInput();

        boolean computedCondition;

        if (null == operation.getCondition()) {
            final Object intermediate;

            if (null != operation.getConditional() && null == operation.getConditional().getTransform()) {
                intermediate = input;
            } else {
                final Operation transform = operation.getConditional().getTransform();
                updateOperationInput(transform, input);
                intermediate = getResultsOrNull(transform, context, store);

                if (null == intermediate) {
                    throw new OperationException("Conditional transform produced a null output");
                }
            }

            try {
                computedCondition = null != operation.getConditional().getPredicate()
                        && operation.getConditional().getPredicate().test(intermediate);
            } catch (final ClassCastException e) {
                throw new OperationException("The predicate '" + operation.getConditional().getPredicate().getClass().getSimpleName()
                        + "' cannot accept an input of type '" + intermediate.getClass().getSimpleName());
            }
        } else {
            computedCondition = operation.getCondition();
        }

        final Operation nextOp = computedCondition ? operation.getThen() : operation.getOtherwise();
        if (null == nextOp) {
            return input;
        } else {
            updateOperationInput(nextOp, input);
            return getResultsOrNull(nextOp, context, store);
        }
    }

    private void updateOperationInput(final Operation operation, final Object input) {
        if (operation instanceof OperationChain) {
            if (!((OperationChain) operation).getOperations().isEmpty()) {
                final Operation firstOp = (Operation) ((OperationChain) operation).getOperations().get(0);
                if (firstOp instanceof Input) {
                    setOperationInput(firstOp, input);
                }
            }
        } else if (operation instanceof Input) {
            setOperationInput(operation, input);
        }
    }

    private void setOperationInput(final Operation operation, final Object input) {
        if (null == ((Input) operation).getInput()) {
            ((Input) operation).setInput(input);
        }
    }

    private Object getResultsOrNull(final Operation op, final Context context, final Store store) throws OperationException {
        if (op instanceof Output) {
            return store.execute((Output) op, context);
        } else {
            store.execute(op, context);
            return null;
        }
    }
}
