/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.accounting.financialactivityaccount.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * A {@link RuntimeException} thrown when product to GL account mapping are not found.
 */
public class FinancialActivityAccountNotFoundException extends AbstractPlatformResourceNotFoundException {

    private static final String DOES_NOT_EXIST = " does not exist";
    private static final String ERROR_CODE = "error.msg.financialActivityAccount.not.found";

    public FinancialActivityAccountNotFoundException(final Long id) {
        super(ERROR_CODE, "Financial Activity account with Id " + id + DOES_NOT_EXIST, id);
    }

    public FinancialActivityAccountNotFoundException(final Long id, EmptyResultDataAccessException e) {
        super(ERROR_CODE, "Financial Activity account with Id " + id + DOES_NOT_EXIST, id, e);
    }

    public FinancialActivityAccountNotFoundException(final Integer financialActivityType) {
        super(ERROR_CODE, "Financial Activity account with for the financial Activity with Id " + financialActivityType + DOES_NOT_EXIST,
                financialActivityType);
    }

}
