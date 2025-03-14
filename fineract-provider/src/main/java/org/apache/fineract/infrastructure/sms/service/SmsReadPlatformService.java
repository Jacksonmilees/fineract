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
package org.apache.fineract.infrastructure.sms.service;

import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.sms.data.SmsData;

public interface SmsReadPlatformService {

    List<SmsData> retrieveAll();

    SmsData retrieveOne(Long resourceId);

    List<SmsData> retrieveAllPending(Long campaignId, Integer limit);

    List<SmsData> retrieveAllSent(Integer limit);

    List<SmsData> retrieveAllDelivered(Integer limit);

    List<SmsData> retrieveAllFailed(Integer limit);

    Page<SmsData> retrieveSmsByStatus(Long campaignId, SearchParameters searchParameters, Integer status, LocalDate dateFrom,
            LocalDate dateTo);

    List<Long> retrieveExternalIdsOfAllSent(Integer limit);

    Page<Long> retrieveAllWaitingForDeliveryReport(Integer limit);

    List<Long> retrieveAllPending(Integer limit);
}
