/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.kie.kogito.codegen.decision;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.kie.api.builder.Message.Level;
import org.kie.dmn.api.core.DMNMessage;
import org.kie.dmn.model.api.DMNModelInstrumentedBase;
import org.kie.dmn.model.api.DecisionTable;
import org.kie.dmn.model.api.Definitions;
import org.kie.dmn.model.api.FunctionDefinition;
import org.kie.dmn.model.api.NamedElement;
import org.kie.dmn.validation.dtanalysis.model.DTAnalysis;
import org.kie.kogito.codegen.api.context.KogitoBuildContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to support DecisionCodegen for DMN model validations
 */
public class DecisionValidation {

    public static final Logger LOG = LoggerFactory.getLogger(DecisionValidation.class);

    private DecisionValidation() {

    }

    public enum ValidationOption {
        /**
         * Perform DMN Validation and blocks if any Errors is found. This is the default.
         */
        ENABLED,
        /**
         * Disable DMN Validation
         */
        DISABLED,
        /**
         * Perform DMN Validation, but Errors are ignored: they are logged but they don't result in any blocking
         */
        IGNORE;
    }

    static void logAndProcessValidationMessages(ValidationOption validateOption, List<DMNMessage> schemaModelValidations) {
        logValidationMessages(schemaModelValidations, DecisionValidation::extractMsgPrefix, DMNMessage::getText);
        processMessagesHandleErrors(validateOption, schemaModelValidations);
    }

    private static String extractMsgPrefix(DMNMessage msg) {
        if (msg.getSourceReference() instanceof DMNModelInstrumentedBase) {
            DMNModelInstrumentedBase ib = (DMNModelInstrumentedBase) msg.getSourceReference();
            while (ib.getParent() != null) {
                ib = ib.getParent();
            }
            if (ib instanceof Definitions) {
                return (((Definitions) ib).getName() + ": ");
            }
        }
        return "";
    }

    public static ValidationOption fromContext(KogitoBuildContext context) {
        if (context == null) {
            LOG.info("No GeneratorContext available, will assume {}=ENABLED", DecisionCodegen.VALIDATION_CONFIGURATION_KEY);
            return ValidationOption.ENABLED;
        }
        Optional<String> applicationProperty = context.getApplicationProperty(DecisionCodegen.VALIDATION_CONFIGURATION_KEY);
        if (applicationProperty.isEmpty()) {
            return ValidationOption.ENABLED; // the default
        }
        Optional<ValidationOption> configOption = Arrays.stream(ValidationOption.values())
                .filter(e -> e.name().equalsIgnoreCase(applicationProperty.get()))
                .findAny();
        if (configOption.isEmpty()) {
            LOG.warn("Validation configuration value {} does not correspond to any valid option, will assume {}=ENABLED", applicationProperty.get(), DecisionCodegen.VALIDATION_CONFIGURATION_KEY);
            return ValidationOption.ENABLED;
        }
        return configOption.get();
    }

    private static void logValidationMessages(List<DMNMessage> validation,
            Function<DMNMessage, String> prefixer,
            Function<DMNMessage, String> computeMessage) {
        for (DMNMessage msg : validation) {
            Consumer<String> logFn = switch (msg.getLevel()) {
                case ERROR -> LOG::error;
                case WARNING -> LOG::warn;
                default -> LOG::info;
            };
            StringBuilder sb = new StringBuilder();
            sb.append(prefixer.apply(msg));
            sb.append(computeMessage.apply(msg));
            logFn.accept(sb.toString());
        }
    }

    private static void processMessagesHandleErrors(ValidationOption validateOption, Collection<DMNMessage> messages) {
        List<DMNMessage> errors = messages.stream().filter(m -> m.getLevel() == Level.ERROR).toList();
        if (!errors.isEmpty()) {
            if (validateOption != ValidationOption.IGNORE) {
                StringBuilder sb = new StringBuilder("DMN Validation schema and model validation contained errors").append("\n");
                sb.append("You may configure ").append(DecisionCodegen.VALIDATION_CONFIGURATION_KEY).append("=IGNORE to ignore validation errors").append("\n");
                sb.append("DMN Validation errors:").append("\n");
                sb.append(errors.stream().map(m -> modelName(m) + ": " + m.getText()).collect(Collectors.joining(",\n")));
                LOG.error(sb.toString());
                throw new RuntimeException(sb.toString());
            } else {
                LOG.warn("DMN Validation encountered errors but validation configuration was set to IGNORE, continuing with no blocking error.");
            }
        }
    }

    private static String modelName(DMNMessage m) {
        Object sr = m.getSourceReference();
        if (sr instanceof DMNModelInstrumentedBase) {
            DMNModelInstrumentedBase dmnIB = (DMNModelInstrumentedBase) sr;
            while (dmnIB != null && !(dmnIB instanceof Definitions)) {
                dmnIB = dmnIB.getParent();
            }
            if (dmnIB instanceof Definitions) {
                return ((Definitions) dmnIB).getName();
            }
        }
        return "";
    }

    private static String nameOrIDOfTable(DTAnalysis r) { // TODO pending DROOLS-5072 refactoring
        DecisionTable sourceDT = r.getSource();
        if (sourceDT.getOutputLabel() != null && !sourceDT.getOutputLabel().isEmpty()) {
            return sourceDT.getOutputLabel();
        } else if (sourceDT.getParent() instanceof NamedElement) {
            return ((NamedElement) sourceDT.getParent()).getName();
        } else if (sourceDT.getParent() instanceof FunctionDefinition && sourceDT.getParent().getParent() instanceof NamedElement) {
            return ((NamedElement) sourceDT.getParent().getParent()).getName();
        } else {
            return new StringBuilder("[ID: ").append(sourceDT.getId()).append("]").toString();
        }
    }
}
