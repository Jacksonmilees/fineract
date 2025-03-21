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
package org.apache.fineract.investor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "m_external_asset_owner_loan_product_configurable_attributes")
public class ExternalAssetOwnerLoanProductAttributes extends AbstractAuditableWithUTCDateTimeCustom<Long> {

    @Column(name = "loan_product_id", nullable = false)
    private Long loanProductId;

    @Column(name = "attribute_key", nullable = false)
    private String attributeKey;

    @Column(name = "attribute_value", nullable = false)
    private String attributeValue;

}
