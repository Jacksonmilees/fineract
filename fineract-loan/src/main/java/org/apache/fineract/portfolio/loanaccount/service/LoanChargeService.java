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
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanChargeValidator;

@RequiredArgsConstructor
public class LoanChargeService {

    private final LoanChargeValidator loanChargeValidator;

    public void recalculateAllCharges(final Loan loan) {
        Set<LoanCharge> charges = loan.getActiveCharges();
        int penaltyWaitPeriod = 0;
        for (final LoanCharge loanCharge : charges) {
            recalculateLoanCharge(loan, loanCharge, penaltyWaitPeriod);
        }
        loan.updateSummaryWithTotalFeeChargesDueAtDisbursement(loan.deriveSumTotalOfChargesDueAtDisbursement());
    }

    public void recalculateLoanCharge(final Loan loan, final LoanCharge loanCharge, final int penaltyWaitPeriod) {
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal chargeAmt;
        BigDecimal totalChargeAmt = BigDecimal.ZERO;
        if (loanCharge.getChargeCalculation().isPercentageBased()) {
            if (loanCharge.isOverdueInstallmentCharge()) {
                amount = loan.calculateOverdueAmountPercentageAppliedTo(loanCharge, penaltyWaitPeriod);
            } else {
                amount = loan.calculateAmountPercentageAppliedTo(loanCharge);
            }
            chargeAmt = loanCharge.getPercentage();
            if (loanCharge.isInstalmentFee()) {
                totalChargeAmt = loan.calculatePerInstallmentChargeAmount(loanCharge);
            }
        } else {
            chargeAmt = loanCharge.amountOrPercentage();
        }
        if (loanCharge.isActive()) {
            loan.clearLoanInstallmentChargesBeforeRegeneration(loanCharge);
            loanCharge.update(chargeAmt, loanCharge.getDueLocalDate(), amount, loan.fetchNumberOfInstallmensAfterExceptions(),
                    totalChargeAmt);
            loanChargeValidator.validateChargeHasValidSpecifiedDateIfApplicable(loan, loanCharge, loan.getDisbursementDate());
        }
    }

    public void makeChargePayment(final Loan loan, final Long chargeId, final LoanLifecycleStateMachine loanLifecycleStateMachine,
            final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds,
            final LoanTransaction paymentTransaction, final Integer installmentNumber) {
        loanChargeValidator.validateChargePaymentNotInFuture(paymentTransaction);
        existingTransactionIds.addAll(loan.findExistingTransactionIds());
        existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
        LoanCharge charge = null;
        for (final LoanCharge loanCharge : loan.getCharges()) {
            if (loanCharge.isActive() && chargeId.equals(loanCharge.getId())) {
                charge = loanCharge;
            }
        }
        loan.handleChargePaidTransaction(charge, paymentTransaction, loanLifecycleStateMachine, installmentNumber);
    }
}
